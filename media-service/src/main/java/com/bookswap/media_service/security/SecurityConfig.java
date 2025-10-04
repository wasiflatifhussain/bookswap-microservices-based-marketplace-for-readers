package com.bookswap.media_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, KeycloakIntrospectionFilter filter)
      throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.authorizeHttpRequests(
        reg ->
            reg.requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**")
                .permitAll()
                .anyRequest()
                .authenticated());
    http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
