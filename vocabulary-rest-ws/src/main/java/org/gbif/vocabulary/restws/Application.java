package org.gbif.vocabulary.restws;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.vocabulary.SpringConfig;
import org.gbif.vocabulary.restws.security.SecurityConfig;
import org.gbif.vocabulary.restws.security.jwt.JwtRequestFilter;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@SpringBootApplication
@Import(SpringConfig.class)
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .additionalInterceptors(
            (request, body, execution) -> {
              request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
              return execution.execute(request, body);
            })
        .build();
  }

  @Configuration
  @Order(10)
  static class ActuatorSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String ACTUATOR_USER = "actuatorAdmin";

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
          .withUser(ACTUATOR_USER)
          .password(passwordEncoder().encode(securityConfig.getStopSecret()))
          .roles("ACTUATOR");
    }

    @Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }

  @Configuration
  @Order(20)
  static class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String VOCABULARIES_PATTERN = "/vocabularies/**";

    @Autowired private AuthenticationProvider basicAuthAuthenticationProvider;

    @Autowired private AuthenticationProvider jwtAuthenticationProvider;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.authorizeRequests()
          .antMatchers(HttpMethod.POST, VOCABULARIES_PATTERN)
          .hasAnyAuthority(UserRole.VOCABULARY_ADMIN.name(), UserRole.VOCABULARY_EDITOR.name())
          .antMatchers(HttpMethod.PUT, VOCABULARIES_PATTERN)
          .hasAnyAuthority(UserRole.VOCABULARY_ADMIN.name(), UserRole.VOCABULARY_EDITOR.name())
          .antMatchers(HttpMethod.DELETE, VOCABULARIES_PATTERN)
          .hasAnyAuthority(UserRole.VOCABULARY_ADMIN.name(), UserRole.VOCABULARY_EDITOR.name())
          .anyRequest()
          .permitAll()
          .and()
          .httpBasic()
          .and()
          .csrf()
          .disable()
          .cors()
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .addFilterBefore(new JwtRequestFilter(), BasicAuthenticationFilter.class);
      // TODO: xss filter
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
      auth.authenticationProvider(basicAuthAuthenticationProvider);
      auth.authenticationProvider(jwtAuthenticationProvider);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
      // CorsFilter only applies this if the origin header is present in the request
      CorsConfiguration configuration = new CorsConfiguration();
      configuration.setAllowedOrigins(Collections.singletonList("*"));
      configuration.setAllowedMethods(
          Arrays.asList("HEAD", "GET", "POST", "DELETE", "PUT", "OPTIONS"));
      UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", configuration);
      return source;
    }
  }
}