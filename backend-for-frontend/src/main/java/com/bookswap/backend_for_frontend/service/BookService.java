package com.bookswap.backend_for_frontend.service;

import com.bookswap.backend_for_frontend.client.catalog.CatalogClient;
import com.bookswap.backend_for_frontend.client.catalog.dto.request.AddBookDto;
import com.bookswap.backend_for_frontend.client.catalog.dto.response.BookDto;
import com.bookswap.backend_for_frontend.client.catalog.dto.response.BookSimpleDto;
import com.bookswap.backend_for_frontend.client.media.MediaClient;
import com.bookswap.backend_for_frontend.client.media.dto.request.UploadInitRequestDto;
import com.bookswap.backend_for_frontend.client.media.dto.response.MediaViewDto;
import com.bookswap.backend_for_frontend.client.media.dto.response.UploadConfirmDto;
import com.bookswap.backend_for_frontend.client.media.dto.response.UploadInitResponseDto;
import com.bookswap.backend_for_frontend.dto.book.request.CreateBookDto;
import com.bookswap.backend_for_frontend.dto.book.request.UploadCompleteRequestDto;
import com.bookswap.backend_for_frontend.dto.book.response.BookCardDto;
import com.bookswap.backend_for_frontend.dto.book.response.CompleteBookDto;
import com.bookswap.backend_for_frontend.dto.home.response.CompletionConfirmDto;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class BookService {
  private final CatalogClient catalogClient;
  private final MediaClient mediaClient;

  public UploadInitResponseDto createBook(CreateBookDto createBookDto) {
    try {
      // Create book in catalog service
      AddBookDto addBookDto =
          AddBookDto.builder()
              .title(createBookDto.getTitle())
              .description(createBookDto.getDescription())
              .genre(createBookDto.getGenre())
              .author(createBookDto.getAuthor())
              .bookCondition(createBookDto.getBookCondition())
              .valuation(createBookDto.getValuation())
              .mediaIds(createBookDto.getMediaIds())
              .build();

      BookSimpleDto bookResponse = catalogClient.addBook(addBookDto);
      log.info("Book created with id={}", bookResponse.getBookId());

      // TODO: Require retries below as book has been created but media upload init failed
      // Initialize media upload
      UploadInitRequestDto uploadInitRequestDto =
          UploadInitRequestDto.builder().files(createBookDto.getFiles()).build();

      UploadInitResponseDto uploadInitResponseDto =
          mediaClient.initializeUpload(uploadInitRequestDto, bookResponse.getBookId());

      log.info("Media upload initialized for book id={}", bookResponse.getBookId());
      return uploadInitResponseDto;

    } catch (Exception e) {
      log.error("Error creating book with message={}", e.getMessage());
      return UploadInitResponseDto.builder().build();
    }
  }

  public CompletionConfirmDto completeBookCreation(
      UploadCompleteRequestDto uploadCompletedRequestDto) {
    try {
      UploadConfirmDto uploadConfirmDto =
          mediaClient.confirmUpload(
              uploadCompletedRequestDto.getBookId(), uploadCompletedRequestDto.getMediaIds());
      log.info("Media upload completed for book id={}", uploadCompletedRequestDto.getBookId());

      return CompletionConfirmDto.builder()
          .bookId(uploadConfirmDto.getBookId())
          .totalCount(uploadConfirmDto.getTotalCount())
          .successCount(uploadConfirmDto.getSuccessCount())
          .build();
    } catch (Exception e) {
      log.error("Error completing book creation with message={}", e.getMessage());
      return CompletionConfirmDto.builder()
          .bookId(uploadCompletedRequestDto.getBookId())
          .totalCount(uploadCompletedRequestDto.getMediaIds().size())
          .successCount(0)
          .build();
    }
  }

  public CompleteBookDto getBookById(String bookId) {
    try {
      BookDto bookDto = catalogClient.getBookById(bookId);
      log.info("Fetched book details for book id={}", bookId);

      // Get media URLs
      List<MediaViewDto> mediaViews = mediaClient.getMediaByBookId(bookId);
      log.info("Fetched media views for book id={}", bookId);

      List<String> mediaUrls =
          mediaViews == null
              ? List.of()
              : mediaViews.stream()
                  .map(MediaViewDto::getPresignedUrl)
                  .filter(Objects::nonNull)
                  .toList();

      return CompleteBookDto.builder()
          .bookId(bookDto.getBookId())
          .title(bookDto.getTitle())
          .description(bookDto.getDescription())
          .genre(bookDto.getGenre())
          .author(bookDto.getAuthor())
          .bookCondition(bookDto.getBookCondition())
          .valuation(bookDto.getValuation())
          .bookStatus(bookDto.getBookStatus())
          .ownerUserId(bookDto.getOwnerUserId())
          .mediaUrls(mediaUrls)
          .createdAt(bookDto.getCreatedAt())
          .updatedAt(bookDto.getUpdatedAt())
          .build();
    } catch (Exception e) {
      log.error("Error fetching book by id with message={}", e.getMessage());
      return CompleteBookDto.builder().build();
    }
  }

  public List<BookCardDto> getMatchingBooks(String bookId, double tolerance) {
    try {
      List<BookDto> matchingBooks = catalogClient.getMatchingBooks(bookId, tolerance);
      if (matchingBooks == null) matchingBooks = List.of();
      log.info("Fetched {} matching books for book id={}", matchingBooks.size(), bookId);

      Map<String, String> bookToPrimaryMediaId = new LinkedHashMap<>();
      for (BookDto b : matchingBooks) {
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

      // 5) Map each BookDto -> BookCardDto, attaching the corresponding thumbnail URL
      List<BookCardDto> bookCardDtoList = new ArrayList<>(matchingBooks.size());
      for (BookDto b : matchingBooks) {
        String primaryMediaId = bookToPrimaryMediaId.get(b.getBookId());
        String thumbnailUrl = (primaryMediaId == null) ? null : mediaIdToUrl.get(primaryMediaId);

        BookCardDto item =
            BookCardDto.builder()
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

        bookCardDtoList.add(item);
      }

      log.info("Mapped matching books to BookCardDto for book id={}", bookId);
      return bookCardDtoList;
    } catch (Exception e) {
      log.error("Error fetching matching books by id with message={}", e.getMessage());
      return Collections.emptyList();
    }
  }
}
