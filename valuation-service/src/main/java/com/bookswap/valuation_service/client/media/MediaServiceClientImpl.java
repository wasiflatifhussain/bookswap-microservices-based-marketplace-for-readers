package com.bookswap.valuation_service.client.media;

import com.bookswap.valuation_service.client.media.dto.MediaResponse;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MediaServiceClientImpl implements MediaServiceClient {
  private final WebClient mediaServiceWebClient;

  public MediaServiceClientImpl(
      @Qualifier("mediaServiceWebClient") WebClient mediaServiceWebClient) {
    this.mediaServiceWebClient = mediaServiceWebClient;
  }

  @Override
  public Mono<List<MediaResponse>> getMediaByBookId(String bookId) {
    log.info("Starting book media fetch from Media-Service for bookId={}", bookId);

    // NOTE: do not use try-catch for reactive calls as exceptions for reactive pipeline won't be
    // caught here. use reactive error handlers.
    return mediaServiceWebClient
        .get()
        .uri("/downloads/{bookId}/view", bookId)
        .exchangeToMono(
            resp -> {
              if (resp.statusCode().is2xxSuccessful()) {
                return resp.bodyToMono(new ParameterizedTypeReference<List<MediaResponse>>() {});
              } else if (resp.statusCode().is4xxClientError()) {
                return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        body -> {
                          log.warn(
                              "Media-Service 4xx for bookId={}, status={}, body={}",
                              bookId,
                              resp.statusCode(),
                              body);
                          return Mono.just(List.of());
                        });
              } else if (resp.statusCode().is5xxServerError()) {
                return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        body -> {
                          log.error(
                              "Media-Service 5xx for bookId={}, status={}, body={}",
                              bookId,
                              resp.statusCode(),
                              body);
                          return Mono.error(new IllegalStateException("Media service error"));
                        });
              }
              return Mono.error(
                  new IllegalStateException("Unexpected HTTP status: " + resp.statusCode()));
            })
        .timeout(Duration.ofSeconds(15)) // throw TimeoutException if doesn't finish by 15 sec
        .doOnError(
            e -> log.error("Failed fetching media for bookId={}, error={}", bookId, e.toString()))
        .onErrorResume(e -> Mono.just(List.of())); // empty list fallback
  }
}
