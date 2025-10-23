package com.bookswap.backend_for_frontend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

  private final CorsConfig corsConfig;

  public SecurityConfig(CorsConfig corsConfig) {
    this.corsConfig = corsConfig;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, KeycloakIntrospectionFilter filter)
      throws Exception {
    http.cors(c -> c.configurationSource(corsConfig.corsConfigurationSource()))
        // No server-side sessions (BFF relays bearer tokens)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            reg ->
                reg.requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated());
    http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
