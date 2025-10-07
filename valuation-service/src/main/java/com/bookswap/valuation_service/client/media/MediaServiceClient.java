package com.bookswap.valuation_service.client.media;

import com.bookswap.valuation_service.client.media.dto.MediaResponse;
import java.util.List;
import reactor.core.publisher.Mono;

public interface MediaServiceClient {
  Mono<List<MediaResponse>> getMediaByBookId(String bookId);
}
