package com.bookswap.backend_for_frontend.api;

import com.bookswap.backend_for_frontend.dto.home.response.FeedItemDto;
import com.bookswap.backend_for_frontend.service.HomeFeedService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bff/home")
@AllArgsConstructor
@Slf4j
public class HomeFeedController {
  private final HomeFeedService homeFeedService;

  @GetMapping("/feed")
  public ResponseEntity<List<FeedItemDto>> getHomeFeed(
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(homeFeedService.getHomeFeed(limit));
  }
}
