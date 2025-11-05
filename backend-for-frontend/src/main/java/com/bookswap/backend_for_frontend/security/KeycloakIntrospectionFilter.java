package com.bookswap.backend_for_frontend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class KeycloakIntrospectionFilter extends OncePerRequestFilter {

  @Value("${keycloak.introspect-url}")
  private String introspectUrl;

  @Value("${keycloak.client-id}")
  private String clientId;

  @Value("${keycloak.client-secret}")
  private String clientSecret;

  private final RestTemplate restTemplate = new RestTemplate();

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String p = request.getRequestURI();
    return p.startsWith("/actuator")
        || p.startsWith("/swagger")
        || p.startsWith("/v3/api-docs")
        || "OPTIONS".equalsIgnoreCase(request.getMethod());
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    String header = req.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ")) {
      res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
      return;
    }
    String token = header.substring(7);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setBasicAuth(clientId, clientSecret);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("token", token);

    try {
      ResponseEntity<Map> r =
          restTemplate.exchange(
              introspectUrl, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);

      Map body = r.getBody();
      if (body == null || !Boolean.TRUE.equals(body.get("active"))) {
        res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inactive");
        return;
      }

      String userId = body.get("sub") != null ? body.get("sub").toString() : null;
      String email = body.get("email") != null ? body.get("email").toString() : null;

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userId, null, List.of());

      Map<String, Object> authenticationDetails = new HashMap<>();
      authenticationDetails.put("sub", userId);
      authenticationDetails.put("email", email);
      authentication.setDetails(authenticationDetails);

      log.info("Current userId={} and email={}", userId, email);
      SecurityContextHolder.getContext().setAuthentication(authentication);

      chain.doFilter(req, res);
    } catch (Exception e) {
      log.error("Token introspection failed", e);
      res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token introspection failed");
    }
  }
}
