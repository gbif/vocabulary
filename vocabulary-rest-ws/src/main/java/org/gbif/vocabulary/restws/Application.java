/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.vocabulary.restws;

import org.gbif.common.messaging.ConnectionParameters;
import org.gbif.common.messaging.DefaultMessagePublisher;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.vocabulary.SpringConfig;
import org.gbif.vocabulary.restws.config.ConfigPropertiesValidator;
import org.gbif.vocabulary.restws.config.ExportConfig;
import org.gbif.vocabulary.restws.config.MessagingConfig;
import org.gbif.vocabulary.restws.security.SecurityConfig;
import org.gbif.ws.remoteauth.RemoteAuthClient;
import org.gbif.ws.remoteauth.RemoteAuthWebSecurityConfigurer;
import org.gbif.ws.remoteauth.RestTemplateRemoteAuthClient;
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;
import org.gbif.ws.server.provider.PageableHandlerMethodArgumentResolver;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@Import({
  SpringConfig.class,
  HttpServletRequestWrapperFilter.class,
  RequestHeaderParamUpdateFilter.class
})
@ComponentScan(
    basePackages = {"org.gbif.vocabulary.restws", "org.gbif.ws.remoteauth"},
    excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE)})
@EnableConfigurationProperties({ExportConfig.class, MessagingConfig.class, SecurityConfig.class})
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Configuration
  public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
      argumentResolvers.add(new PageableHandlerMethodArgumentResolver());
    }
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder, SecurityConfig securityConfig) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofSeconds(60))
        .rootUri(securityConfig.getLoginApiBasePath())
        .additionalInterceptors(
            (request, body, execution) -> {
              request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
              return execution.execute(request, body);
            })
        .build();
  }

  @Bean
  public RemoteAuthClient remoteAuthClient(RestTemplate restTemplate) {
    return new RestTemplateRemoteAuthClient(restTemplate);
  }

  @Bean
  @ConditionalOnProperty(value = "messaging.enabled", havingValue = "true")
  @Autowired
  public MessagePublisher messagePublisher(MessagingConfig config) throws IOException {
    return new DefaultMessagePublisher(
        new ConnectionParameters(
            config.getHost(),
            config.getPort(),
            config.getUsername(),
            config.getPassword(),
            config.getVirtualHost()));
  }

  @Bean
  public static ConfigPropertiesValidator configurationPropertiesValidator() {
    return new ConfigPropertiesValidator();
  }

  @Configuration
  @Order(10)
  static class ActuatorSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired private SecurityConfig securityConfig;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.antMatcher("/actuator/**")
          .authorizeRequests()
          .anyRequest()
          .authenticated()
          .and()
          .httpBasic()
          .and()
          .csrf()
          .disable()
          .cors()
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
      auth.inMemoryAuthentication()
          .withUser(securityConfig.getActuatorUser())
          .password(passwordEncoder().encode(securityConfig.getActuatorSecret()))
          .roles("ACTUATOR");
    }

    @Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }

  @Configuration
  @Order(20)
  public class SpringSecurityConfig extends RemoteAuthWebSecurityConfigurer {

    public SpringSecurityConfig(
        ApplicationContext applicationContext, RemoteAuthClient remoteAuthClient) {
      super(applicationContext, remoteAuthClient);
    }
  }
}
