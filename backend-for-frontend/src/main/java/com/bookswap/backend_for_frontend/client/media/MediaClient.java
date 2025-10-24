package com.bookswap.backend_for_frontend.client.media;

import com.bookswap.backend_for_frontend.client.media.dto.request.UploadInitRequestDto;
import com.bookswap.backend_for_frontend.client.media.dto.response.MediaViewDto;
import com.bookswap.backend_for_frontend.client.media.dto.response.UploadConfirmDto;
import com.bookswap.backend_for_frontend.client.media.dto.response.UploadInitResponseDto;
import com.bookswap.backend_for_frontend.config.ServiceEndpoints;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class MediaClient {
  private WebClient webClient;

  public MediaClient(WebClient.Builder builder, ServiceEndpoints serviceEndpoints) {
    this.webClient = builder.baseUrl(serviceEndpoints.getMedia()).build();
  }

  public List<MediaViewDto> getViewUrlsByMediaIds(List<String> mediaIds) {
    return webClient
        .post()
        .uri("/api/media/downloads/by-media/batch")
        .bodyValue(mediaIds)
        .retrieve()
        .bodyToFlux(MediaViewDto.class)
        .collectList()
        .block();
  }

  public UploadInitResponseDto initializeUpload(
      UploadInitRequestDto uploadInitRequestDto, String bookId) {
    return webClient
        .post()
        .uri("/api/media/uploads/{bookId}/init", bookId)
        .bodyValue(uploadInitRequestDto)
        .retrieve()
        .bodyToMono(UploadInitResponseDto.class)
        .block();
  }

  public UploadConfirmDto confirmUpload(String bookId, List<String> mediaIds) {
    return webClient
        .post()
        .uri("/api/media/uploads/{bookId}/complete", bookId)
        .bodyValue(mediaIds)
        .retrieve()
        .bodyToMono(UploadConfirmDto.class)
        .block();
  }
}
