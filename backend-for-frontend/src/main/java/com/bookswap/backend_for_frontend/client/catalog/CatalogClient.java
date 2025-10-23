package com.bookswap.backend_for_frontend.client.catalog;

import com.bookswap.backend_for_frontend.client.catalog.dto.BookDto;
import com.bookswap.backend_for_frontend.config.ServiceEndpoints;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class CatalogClient {
  private WebClient webClient;

  public CatalogClient(WebClient.Builder builder, ServiceEndpoints serviceEndpoints) {
    this.webClient = builder.baseUrl(serviceEndpoints.getCatalog()).build();
  }

  public List<BookDto> getRecentListedBooks(int limit) {
    return webClient
        .get()
        .uri("/api/catalog/books/recent?limit={limit}", limit)
        .retrieve()
        .bodyToFlux(BookDto.class)
        .collectList()
        .block();
  }
}
