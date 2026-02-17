package com.bookswap.backend_for_frontend.security.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SessionRepository {

  private final StringRedisTemplate redis;
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  public SessionRepository(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public void save(String sessionId, SessionData data, Duration ttl) {
    try {
      String json = mapper.writeValueAsString(data);
      redis.opsForValue().set(key(sessionId), json, ttl);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize session", e);
    }
  }

  public SessionData find(String sessionId) {
    String json = redis.opsForValue().get(key(sessionId));
    if (json == null) return null;

    try {
      return mapper.readValue(json, SessionData.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize session", e);
    }
  }

  public void delete(String sessionId) {
    redis.delete(key(sessionId));
  }

  private String key(String sessionId) {
    return "session:" + sessionId;
  }
}
