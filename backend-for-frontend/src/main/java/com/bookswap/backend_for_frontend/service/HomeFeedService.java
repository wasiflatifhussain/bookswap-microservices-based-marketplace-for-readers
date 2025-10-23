package com.bookswap.backend_for_frontend.service;

import com.bookswap.backend_for_frontend.client.catalog.CatalogClient;
import com.bookswap.backend_for_frontend.client.catalog.dto.BookDto;
import com.bookswap.backend_for_frontend.client.catalog.dto.FeedItemDto;
import com.bookswap.backend_for_frontend.client.media.MediaClient;
import com.bookswap.backend_for_frontend.client.media.dto.MediaViewDto;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class HomeFeedService {
  private final CatalogClient catalogClient;
  private final MediaClient mediaClient;

  public List<FeedItemDto> getHomeFeed(int limit) {
    try {
      List<BookDto> bookDtoList = catalogClient.getRecentListedBooks(limit);
      log.info("Successfully fetched home feedItemsList with {} books", bookDtoList.size());

      // Pick exactly one mediaId per book (first id if present)
      Map<String, String> bookToPrimaryMediaId = new LinkedHashMap<>();
      for (BookDto b : bookDtoList) {
        List<String> ids = b.getMediaIds();
        if (ids != null && !ids.isEmpty()) {
          bookToPrimaryMediaId.put(b.getBookId(), ids.get(0));
        } else {
          bookToPrimaryMediaId.put(b.getBookId(), null);
        }
      }

      // Dedup primary media IDs for calling media service
      List<String> primaryMediaIds =
          bookToPrimaryMediaId.values().stream().filter(Objects::nonNull).distinct().toList();

      // Batch call Media to get presigned URLs (best-effort)
      Map<String, String> mediaIdToUrl = new HashMap<>();
      try {
        if (!primaryMediaIds.isEmpty()) {
          List<MediaViewDto> mediaViews = mediaClient.getViewUrlsByMediaIds(primaryMediaIds);
          if (mediaViews != null) {
            for (MediaViewDto mv : mediaViews) {
              if (mv != null && mv.getMediaId() != null && mv.getPresignedUrl() != null) {
                mediaIdToUrl.put(mv.getMediaId(), mv.getPresignedUrl());
              }
            }
            log.info("Successfully fetched {} media view URLs", mediaIdToUrl.size());
          }
        }
      } catch (Exception me) {
        log.warn("Media batch fetch failed (continuing without thumbnails): {}", me.getMessage());
      }

      // 5) Map each BookDto -> FeedItemDto, attaching the corresponding thumbnail URL
      List<FeedItemDto> feedItemsList = new ArrayList<>(bookDtoList.size());
      for (BookDto b : bookDtoList) {
        String primaryMediaId = bookToPrimaryMediaId.get(b.getBookId());
        String thumbnailUrl = (primaryMediaId == null) ? null : mediaIdToUrl.get(primaryMediaId);

        FeedItemDto item =
            FeedItemDto.builder()
                .bookId(b.getBookId())
                .title(b.getTitle())
                .description(b.getDescription())
                .genre(b.getGenre())
                .author(b.getAuthor())
                .bookCondition(b.getBookCondition())
                .valuation(b.getValuation())
                .bookStatus(b.getBookStatus())
                .thumbnailUrl(thumbnailUrl)
                .build();

        feedItemsList.add(item);
      }

      log.info("Successfully constructed {} feed items", feedItemsList.size());
      return feedItemsList;
    } catch (Exception e) {
      log.error("Error fetching home feed={}", e.getMessage());
      return List.of();
    }
  }
}
