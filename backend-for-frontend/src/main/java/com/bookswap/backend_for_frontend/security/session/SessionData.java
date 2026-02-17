package com.bookswap.backend_for_frontend.security.session;

import java.time.Instant;
import lombok.Data;

@Data
public class SessionData {
  private String userId;
  private String email;
  private String firebaseIdToken;
  private Instant expiresAt;
}
