package com.bookswap.backend_for_frontend.security;

import com.bookswap.backend_for_frontend.security.session.SessionData;
import com.bookswap.backend_for_frontend.security.session.SessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class SessionCookieBearerTokenResolver implements BearerTokenResolver {

  private final SessionRepository sessions;
  private final String cookieName;

  public SessionCookieBearerTokenResolver(
      SessionRepository sessions, @Value("${auth.session.cookie-name}") String cookieName) {
    this.sessions = sessions;
    this.cookieName = cookieName;
  }

  @Override
  public String resolve(HttpServletRequest request) {

    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth != null && auth.startsWith("Bearer ")) {
      return auth.substring("Bearer ".length());
    }

    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;

    for (Cookie c : cookies) {
      if (cookieName.equals(c.getName())) {
        SessionData s = sessions.find(c.getValue());
        if (s == null) return null;

        if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(Instant.now())) {
          return null;
        }

        return s.getFirebaseIdToken();
      }
    }
    return null;
  }
}
