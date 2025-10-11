package com.bookswap.swap_service.client.catalog;

import com.bookswap.swap_service.client.catalog.dto.BookRequest;
import com.bookswap.swap_service.client.catalog.dto.BookResponseDetailed;
import com.bookswap.swap_service.client.catalog.dto.BookResponseWithMedia;
import com.bookswap.swap_service.security.ServiceTokenProvider;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class CatalogServiceClient {
  private final WebClient catalogServiceWebClient;
  private final ServiceTokenProvider serviceTokenProvider;

  public CatalogServiceClient(
      @Qualifier("catalogServiceWebClient") WebClient catalogServiceWebClient,
      ServiceTokenProvider serviceTokenProvider) {
    this.catalogServiceWebClient = catalogServiceWebClient;
    this.serviceTokenProvider = serviceTokenProvider;
  }

  public Mono<List<BookResponseWithMedia>> getBooksByBulkBookIdOrder(BookRequest bookRequest) {
    log.info("Starting book fetch from Catalog-Service for bookRequest={}", bookRequest);

    // NOTE: do not use try-catch for reactive calls as exceptions for reactive pipeline won't be
    // caught here. use reactive error handlers.
    return catalogServiceWebClient
        .post()
        .uri("/books/bulk")
        .bodyValue(bookRequest)
        .exchangeToMono(
            resp -> {
              if (resp.statusCode().is2xxSuccessful()) {
                return resp.bodyToMono(
                    new ParameterizedTypeReference<List<BookResponseWithMedia>>() {});
              } else if (resp.statusCode().is4xxClientError()) {
                return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        body -> {
                          log.warn(
                              "Catalog-Service 4xx with status={}, body={}",
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
                              "Catalog-Service 5xx with status={}, body={}",
                              resp.statusCode(),
                              body);
                          return Mono.error(new IllegalStateException("Catalog-Service error"));
                        });
              }
              return Mono.error(
                  new IllegalStateException("Unexpected HTTP status: " + resp.statusCode()));
            })
        .timeout(Duration.ofSeconds(15)) // throw TimeoutException if doesn't finish by 15 sec
        .doOnError(e -> log.error("Failed fetching books with error={}", e.toString()))
        .onErrorResume(e -> Mono.just(List.of())); // empty list fallback
  }

  public Mono<BookResponseDetailed> getBookByBookId(String bookId) {
    log.info("Starting book fetch from Catalog-Service for bookId={}", bookId);

    return catalogServiceWebClient
        .get()
        .uri("/books/{bookId}", bookId)
        .exchangeToMono(
            resp -> {
              if (resp.statusCode().is2xxSuccessful()) {
                return resp.bodyToMono(new ParameterizedTypeReference<BookResponseDetailed>() {});
              } else if (resp.statusCode().is4xxClientError()) {
                return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        body -> {
                          log.warn(
                              "Catalog-Service 4xx with status={}, body={}",
                              resp.statusCode(),
                              body);
                          return Mono.error(new IllegalArgumentException("Catalog 4xx: " + body));
                        });
              } else if (resp.statusCode().is5xxServerError()) {
                return resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(
                        body -> {
                          log.error(
                              "Catalog-Service 5xx with status={}, body={}",
                              resp.statusCode(),
                              body);
                          return Mono.error(new IllegalStateException("Catalog-Service error"));
                        });
              }
              return Mono.error(
                  new IllegalStateException("Unexpected HTTP status: " + resp.statusCode()));
            })
        .timeout(Duration.ofSeconds(15)) // throw TimeoutException if doesn't finish by 15 sec
        .doOnError(e -> log.error("Failed fetching books with error={}", e.toString()));
  }

  public Mono<Boolean> reserveBook(String bookId) {
    log.info("Starting book reservation from Catalog-Service for bookId={}", bookId);

    return catalogServiceWebClient
        .post()
        .uri("/books/{bookId}/reserve", bookId)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            resp ->
                resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .doOnNext(
                        body -> log.warn("Catalog 4xx status={}, body={}", resp.statusCode(), body))
                    .then(Mono.error(new IllegalStateException("Catalog 4xx"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            resp ->
                resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .doOnNext(
                        body ->
                            log.error("Catalog 5xx status={}, body={}", resp.statusCode(), body))
                    .then(Mono.error(new IllegalStateException("Catalog 5xx"))))
        .bodyToMono(Boolean.class)
        .timeout(Duration.ofSeconds(10)) // throw TimeoutException if doesn't finish by 15 sec
        .doOnError(
            e -> log.error("Reserve book failed for bookId={}, error={}", bookId, e.toString()));
  }

  public Mono<Boolean> unreserveBook(String bookId) {
    // sends request tp /books/{bookId}/unreserve
    log.info("Starting book unreservation from Catalog-Service for bookId={}", bookId);

    return catalogServiceWebClient
        .post()
        .uri("/books/{bookId}/unreserve", bookId)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            resp ->
                resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .doOnNext(
                        body -> log.warn("Catalog 4xx status={}, body={}", resp.statusCode(), body))
                    .then(Mono.error(new IllegalStateException("Catalog 4xx"))))
        .onStatus(
            HttpStatusCode::is5xxServerError,
            resp ->
                resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .doOnNext(
                        body ->
                            log.error("Catalog 5xx status={}, body={}", resp.statusCode(), body))
                    .then(Mono.error(new IllegalStateException("Catalog 5xx"))))
        .bodyToMono(Boolean.class)
        .timeout(Duration.ofSeconds(10)) // throw TimeoutException if doesn't finish by 15 sec
        .doOnError(
            e -> log.error("Unreserve book failed for bookId={}, error={}", bookId, e.toString()));
  }
}
