package com.bookswap.notification_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class UnreadCounterService {
  private final StringRedisTemplate redis;

  private String key(String userId) {
    return "unread:" + userId;
  }

  public void increment(String userId, int incrementBy) {
    if (incrementBy <= 0) return;
    redis.opsForValue().increment(key(userId), incrementBy);
  }

  public void decrement(String userId, int decrementBy) {

    String current = redis.opsForValue().get(key(userId));

    if (current == null) {
      redis.opsForValue().set(key(userId), "0");
      return;
    }

    int currentValue = Integer.parseInt(current);
    if (currentValue - decrementBy < 0) {
      redis.opsForValue().set(key(userId), "0");
      return;
    }

    redis.opsForValue().decrement(key(userId), decrementBy);
  }

  public void reset(String userId, int value) {
    redis.opsForValue().set(key(userId), String.valueOf(Math.max(0, value)));
  }

  /** Returns Redis value or null if not set. */
  public Integer getIfPresent(String userId) {
    String v = redis.opsForValue().get(key(userId));
    return (v == null) ? null : Integer.parseInt(v);
  }
}
