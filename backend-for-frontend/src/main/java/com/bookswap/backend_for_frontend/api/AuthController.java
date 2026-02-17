package com.bookswap.backend_for_frontend.api;

import com.bookswap.backend_for_frontend.security.session.SessionData;
import com.bookswap.backend_for_frontend.security.session.SessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/bff/auth")
public class AuthController {

  private final SessionRepository sessions;
  private final Duration sessionTtl;
  private final String cookieName;

  public AuthController(
      SessionRepository sessions,
      @Value("${auth.session.ttl-seconds}") long ttlSeconds,
      @Value("${auth.session.cookie-name}") String cookieName) {
    this.sessions = sessions;
    this.sessionTtl = Duration.ofSeconds(ttlSeconds);
    this.cookieName = cookieName;
  }

  /**
   * This endpoint is authenticated by Spring Security JWT (Authorization: Bearer ...). It does not
   * verify with Firebase Admin and trusts Spring's JWT validation and create a session cookie.
   */
  @PostMapping("/login")
  public ResponseEntity<Void> login(
      @AuthenticationPrincipal Jwt jwt, HttpServletResponse response) {

    if (jwt == null) {
      return ResponseEntity.status(401).build();
    }

    // Expiry from the verified JWT
    Instant tokenExpiresAt = jwt.getExpiresAt();
    if (tokenExpiresAt == null) {
      return ResponseEntity.status(401).build();
    }

    Duration untilTokenExpiry = Duration.between(Instant.now(), tokenExpiresAt);
    if (untilTokenExpiry.isZero() || untilTokenExpiry.isNegative()) {
      return ResponseEntity.status(401).build();
    }

    Duration effectiveTtl =
        (sessionTtl.compareTo(untilTokenExpiry) < 0) ? sessionTtl : untilTokenExpiry;

    String sessionId = UUID.randomUUID().toString();

    SessionData data = new SessionData();
    data.setUserId(jwt.getSubject());
    data.setEmail(jwt.getClaimAsString("email"));
    // Store the actual JWT string so BFF can relay it downstream
    data.setFirebaseIdToken(jwt.getTokenValue());
    data.setExpiresAt(tokenExpiresAt);

    sessions.save(sessionId, data, effectiveTtl);

    ResponseCookie cookie =
        ResponseCookie.from(cookieName, sessionId)
            .httpOnly(true)
            .secure(false) // TODO: set true in prod (HTTPS)
            .sameSite("Lax") // TODO: likely "None" in prod if cross-site
            .path("/")
            .maxAge(effectiveTtl)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

    String sessionId = readCookie(request, cookieName);
    if (sessionId != null && !sessionId.isBlank()) {
      sessions.delete(sessionId);
    }

    ResponseCookie cleared =
        ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
    return ResponseEntity.noContent().build();
  }

  private String readCookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;
    for (Cookie c : cookies) {
      if (name.equals(c.getName())) return c.getValue();
    }
    return null;
  }
}
