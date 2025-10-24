package com.bookswap.backend_for_frontend.service;

import com.bookswap.backend_for_frontend.client.catalog.CatalogClient;
import com.bookswap.backend_for_frontend.client.catalog.dto.request.AddBookDto;
import com.bookswap.backend_for_frontend.client.catalog.dto.response.BookSimpleDto;
import com.bookswap.backend_for_frontend.client.media.MediaClient;
import com.bookswap.backend_for_frontend.client.media.dto.request.UploadInitRequestDto;
import com.bookswap.backend_for_frontend.client.media.dto.response.UploadConfirmDto;
import com.bookswap.backend_for_frontend.client.media.dto.response.UploadInitResponseDto;
import com.bookswap.backend_for_frontend.dto.book.request.CreateBookDto;
import com.bookswap.backend_for_frontend.dto.book.request.UploadCompleteRequestDto;
import com.bookswap.backend_for_frontend.dto.home.response.CompletionConfirmDto;
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
}
