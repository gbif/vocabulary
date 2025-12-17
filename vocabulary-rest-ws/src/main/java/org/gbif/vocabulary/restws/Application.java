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
import org.gbif.vocabulary.restws.config.WsConfig;
import org.gbif.vocabulary.restws.resolvers.LanguageRegionHandlerMethodArgumentResolver;
import org.gbif.vocabulary.restws.security.SecurityConfig;
import org.gbif.ws.remoteauth.RemoteAuthClient;
import org.gbif.ws.remoteauth.RemoteAuthWebSecurityConfigurer;
import org.gbif.ws.remoteauth.RestTemplateRemoteAuthClient;
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;
import org.gbif.ws.server.provider.PageableHandlerMethodArgumentResolver;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@Import({
  SpringConfig.class,
  HttpServletRequestWrapperFilter.class,
  RequestHeaderParamUpdateFilter.class
})
@ComponentScan(
    basePackages = {
      "org.gbif.vocabulary.restws",
      "org.gbif.ws.remoteauth",
      "org.gbif.ws.server.interceptor"
    },
    excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE)})
@EnableConfigurationProperties({
  ExportConfig.class,
  MessagingConfig.class,
  SecurityConfig.class,
  WsConfig.class
})
@EnableFeignClients
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Configuration
  public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
      argumentResolvers.add(new PageableHandlerMethodArgumentResolver());
      argumentResolvers.add(new LanguageRegionHandlerMethodArgumentResolver());
    }
  }

  @Bean
  public RemoteAuthClient remoteAuthClient(
      RestTemplateBuilder builder, SecurityConfig securityConfig) {
    return RestTemplateRemoteAuthClient.createInstance(
        builder, securityConfig.getLoginApiBasePath());
  }

  @Bean
  @ConditionalOnProperty(value = "messaging.enabled", havingValue = "true")
  //@Autowired
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
  @EnableWebSecurity
  static class ActuatorSecurityConfig {

    @Autowired private SecurityConfig securityConfig;

    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
      http.securityMatcher("/actuator/**")
          .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
          .httpBasic(httpBasic -> {})
          .csrf(csrf -> csrf.disable())
          .cors(cors -> {})
          .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
      return http.build();
    }

    @Bean
    public UserDetailsService actuatorUserDetailsService() {
      UserDetails user = User.withUsername(securityConfig.getActuatorUser())
          .password(passwordEncoder().encode(securityConfig.getActuatorSecret()))
          .roles("ACTUATOR")
          .build();
      return new InMemoryUserDetailsManager(user);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }

  @Configuration
  @Order(20)
  static class SpringSecurityConfig extends RemoteAuthWebSecurityConfigurer {

    public SpringSecurityConfig(
        ApplicationContext applicationContext, RemoteAuthClient remoteAuthClient) {
      super();
    }
  }
}
