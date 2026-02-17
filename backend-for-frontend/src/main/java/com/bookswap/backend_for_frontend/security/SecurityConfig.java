package com.bookswap.backend_for_frontend.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, SessionCookieBearerTokenResolver resolver) throws Exception {

    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**")
                    .permitAll()

                    // login MUST be authenticated (frontend sends Authorization: Bearer <idToken>)
                    .requestMatchers(HttpMethod.POST, "/auth/login")
                    .authenticated()

                    // logout can be unauthenticated (we just clear cookie + best-effort delete)
                    .requestMatchers(HttpMethod.POST, "/auth/logout")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .bearerTokenResolver(resolver)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));

    return http.build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    // Ignore roles for now; identity-based auth only
    converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of());
    return converter;
  }
}
