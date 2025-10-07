package com.bookswap.valuation_service.client.media;

import com.bookswap.valuation_service.client.gemini.dto.GeminiInlineImage;
import com.bookswap.valuation_service.client.media.dto.MediaResponse;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MediaDownloadClient {
  private static final int DEFAULT_MAX_IMAGES = 8;

  private final WebClient mediaDownloadClient;

  public MediaDownloadClient(@Qualifier("mediaDownloadWebClient") WebClient mediaDownloadClient) {
    this.mediaDownloadClient = mediaDownloadClient;
  }

  public List<GeminiInlineImage> downloadImages(List<MediaResponse> mediaList) {
    if (mediaList == null || mediaList.isEmpty()) return List.of();

    return mediaList.stream()
        .filter(Objects::nonNull)
        .filter(m -> m.presignedUrl() != null && !m.presignedUrl().isBlank())
        .limit(DEFAULT_MAX_IMAGES)
        .map(
            image -> {
              String url = image.presignedUrl();
              try {
                byte[] bytes =
                    mediaDownloadClient
                        .get()
                        .uri(URI.create(url))
                        .accept(MediaType.ALL)
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .onErrorResume(
                            ex -> {
                              log.warn("Failed to download image {}: {}", url, ex.toString());
                              return Mono.empty();
                            })
                        .block();

                if (bytes == null || bytes.length == 0) {
                  log.warn("Empty bytes for presigned image {}", url);
                  return null;
                }

                log.info(
                    "Successfully downloaded image with url={} of size={} bytes",
                    url,
                    bytes.length);

                String mime = guessMime(image.mimeType(), url);
                String b64 = Base64.getEncoder().encodeToString(bytes);
                return GeminiInlineImage.builder().mimeType(mime).base64Data(b64).build();

              } catch (Exception e) {
                log.warn("Skipping image with url={} due to error={}", url, e.toString());
                return null;
              }
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private String guessMime(String mimeType, String url) {
    if (mimeType != null && !mimeType.isBlank()) return mimeType;
    return url.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
  }
}
