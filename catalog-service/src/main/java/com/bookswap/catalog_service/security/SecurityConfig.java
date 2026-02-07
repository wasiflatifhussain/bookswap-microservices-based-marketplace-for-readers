package com.bookswap.catalog_service.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig
 *
 * <p>Purpose: Configures JWT-based authentication for the Catalog Service using Spring Security.
 *
 * <p>This service acts as an OAuth2 Resource Server and validates incoming Firebase-issued JWT ID
 * tokens locally (no token introspection).
 *
 * <p>Key Responsibilities: - Enforces authentication for all protected API endpoints - Validates
 * JWT signature, issuer, and expiration using Firebase public keys - Builds a Spring Security
 * Authentication object from the JWT
 *
 * <p>Design Decisions: - Role / authority mapping is intentionally disabled for now (identity-based
 * authorization is sufficient for this service) - User identity is derived from the JWT `sub` claim
 * (Firebase UID) - Authorization is enforced at the application/data layer (e.g., ownerUserId
 * checks in services)
 *
 * <p>Security Model: - Stateless, zero-trust authentication - No runtime dependency on Firebase
 * services - JWTs are verified locally using Google’s public signing keys - Each request is
 * independently authenticated using cryptographic verification
 *
 * <p>Notes: - Role-based access control (RBAC) can be added later by enabling JWT
 * claim-to-authority conversion if needed.
 */
@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));

    return http.build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> List.of());
    return converter;
  }
}
