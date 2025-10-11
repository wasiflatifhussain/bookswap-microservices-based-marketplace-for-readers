package com.bookswap.catalog_service.service;

import com.bookswap.catalog_service.domain.book.Book;
import com.bookswap.catalog_service.domain.book.BookStatus;
import com.bookswap.catalog_service.domain.outbox.AggregateType;
import com.bookswap.catalog_service.dto.event.BookCreatedEvent;
import com.bookswap.catalog_service.dto.event.BookUnlistedEvent;
import com.bookswap.catalog_service.dto.request.BookIdList;
import com.bookswap.catalog_service.dto.request.BookRequest;
import com.bookswap.catalog_service.dto.response.BookDetailedResponse;
import com.bookswap.catalog_service.dto.response.BookResponseWithMedia;
import com.bookswap.catalog_service.dto.response.BookSimpleResponse;
import com.bookswap.catalog_service.repository.BookRepository;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class BookService {
  private final BookRepository bookRepository;
  private final OutboxService outboxService;

  @Transactional
  public BookSimpleResponse addBook(BookRequest bookRequest, String keycloakId) {
    log.info("Initiating adding book to for title={}", bookRequest.getTitle());

    try {
      Book book = mapRequestToBook(bookRequest, keycloakId);
      Book savedBook = bookRepository.save(book);

      BookCreatedEvent bookCreatedEvent =
          BookCreatedEvent.builder()
              .bookId(savedBook.getBookId())
              .title(savedBook.getTitle())
              .description(savedBook.getDescription())
              .author(savedBook.getAuthor())
              .bookCondition(savedBook.getBookCondition())
              .ownerUserId(savedBook.getOwnerUserId())
              .build();

      outboxService.enqueueEvent(
          AggregateType.BOOK, savedBook.getBookId(), "BOOK_CREATED", bookCreatedEvent);

      return mapBookToSimplifiedBook(savedBook);

    } catch (Exception e) {
      log.error("Error while creating new book entry with e=", e);
      return BookSimpleResponse.builder()
          .message("Error while creating new book entry with e=" + e)
          .build();
    }
  }

  @Transactional(readOnly = true)
  public BookDetailedResponse getBookByBookId(String bookId) {
    log.info("Initiating search for book by bookId={}", bookId);

    try {
      Optional<Book> book = bookRepository.findByBookId(bookId);
      if (book.isEmpty()) {
        return BookDetailedResponse.builder().message("No books with this bookId exist").build();
      }
      return mapBookToDetailedBook(book.get());
    } catch (Exception e) {
      log.error("Error while searching for book with e=", e);
      return BookDetailedResponse.builder()
          .message("Error while searching for book with e={}" + e)
          .build();
    }
  }

  @Transactional(readOnly = true)
  public BookDetailedResponse[] getBooksByUserId(String userId) {
    log.info("Initiating search for books by userId={}", userId);

    try {
      List<Book> books = bookRepository.findByOwnerUserId(userId);
      if (books.isEmpty()) {
        return new BookDetailedResponse[] {
          BookDetailedResponse.builder().message("No books with this userId exist").build()
        };
      }
      return books.stream().map(this::mapBookToDetailedBook).toArray(BookDetailedResponse[]::new);
    } catch (Exception e) {
      log.error("Error while searching for book with e=", e);
      return new BookDetailedResponse[] {
        BookDetailedResponse.builder().message("Error while searching for book with e=" + e).build()
      };
    }
  }

  @Transactional
  public String deleteBookByBookId(String bookId) {
    log.info("Initiating deletion for book by bookId={}", bookId);
    try {
      Optional<Book> bookOpt = bookRepository.findByBookId(bookId);
      if (bookOpt.isEmpty()) {
        return "ERROR: Book not found";
      }
      Book book = bookOpt.get();
      bookRepository.deleteByBookId(bookId);

      BookUnlistedEvent unlistedEvent =
          BookUnlistedEvent.builder()
              .bookId(book.getBookId())
              .ownerUserId(book.getOwnerUserId())
              .valuation(book.getValuation())
              .build();

      // NOTE: This also puts an entry in Outbox table for audit - Outbox is an append-only log
      // keeping both copies for record
      outboxService.enqueueEvent(AggregateType.BOOK, bookId, "BOOK_UNLISTED", unlistedEvent);
      return "SUCCESS";
    } catch (Exception e) {
      log.error("Error while deleting book with error=", e);
      return "ERROR: " + e;
    }
  }

  @Transactional(readOnly = true)
  public BookDetailedResponse[] getRecentListedBooks(int limit) {
    try {
      List<Book> books =
          bookRepository.findByBookStatusOrderByCreatedAtDesc(
              BookStatus.AVAILABLE, PageRequest.of(0, limit));
      if (books.isEmpty()) {
        return new BookDetailedResponse[] {
          BookDetailedResponse.builder().message("No recent listed books found").build()
        };
      }
      return books.stream().map(this::mapBookToDetailedBook).toArray(BookDetailedResponse[]::new);
    } catch (Exception e) {
      log.error("Error while fetching recent listed books with e=", e);
      return new BookDetailedResponse[] {
        BookDetailedResponse.builder()
            .message("Error while fetching recent listed books with e=" + e)
            .build()
      };
    }
  }

  @Transactional(readOnly = true)
  public BookDetailedResponse[] getMatchingBooks(String bookId, double tolerance) {
    try {
      Optional<Book> myBookOpt = bookRepository.findByBookId(bookId);
      if (myBookOpt.isEmpty()) {
        return new BookDetailedResponse[] {
          BookDetailedResponse.builder().message("Book not found").build()
        };
      }
      Book myBook = myBookOpt.get();
      double val = myBook.getValuation();
      double minVal = val * (1 - tolerance);
      double maxVal = val * (1 + tolerance);

      List<Book> matches =
          bookRepository.findMatchingBooks(
              BookStatus.AVAILABLE, bookId, minVal, maxVal, PageRequest.of(0, 20));
      if (matches.isEmpty()) {
        return new BookDetailedResponse[] {
          BookDetailedResponse.builder().message("No matching books found").build()
        };
      }
      return matches.stream().map(this::mapBookToDetailedBook).toArray(BookDetailedResponse[]::new);
    } catch (Exception e) {
      log.error("Error while fetching matching books", e);
      return new BookDetailedResponse[] {
        BookDetailedResponse.builder().message("Error while fetching matching books: " + e).build()
      };
    }
  }

  @Transactional
  public void appendMediaToBook(String bookId, String ownerUserId, List<String> mediaIds) {
    try {
      if (mediaIds == null || mediaIds.isEmpty()) {
        log.info("No mediaIds to append for bookId={} ownerUserId={}", bookId, ownerUserId);
        return;
      }

      Optional<Book> bookOpt = bookRepository.findByBookIdAndOwnerUserId(bookId, ownerUserId);
      if (bookOpt.isEmpty()) {
        log.warn(
            "Book not found or owner mismatch when appending media: bookId={}, ownerUserId={}",
            bookId,
            ownerUserId);
        return;
      }

      Book book = bookOpt.get();
      List<String> current = book.getMediaIds();
      if (current == null) current = new java.util.ArrayList<>();

      LinkedHashSet<String> mergedMediaIds = new LinkedHashSet<>(current);
      mergedMediaIds.addAll(mediaIds);

      if (mergedMediaIds.size() == current.size()) {
        log.info("No new mediaIds to append for bookId={} ownerUserId={}", bookId, ownerUserId);
        return;
      }

      book.setMediaIds(new ArrayList<>(mergedMediaIds));
      bookRepository.save(book);

      // Enqueue BOOK_MEDIA_FINALIZED event
      // Payload is the full Book object with updated mediaIds
      // Gets picked up by OutboxRelay to be sent to Kafka for ValuationService to process
      outboxService.enqueueEvent(
          AggregateType.BOOK, book.getBookId(), "BOOK_MEDIA_FINALIZED", book);

      log.info(
          "Appended mediaIds={} to bookId={} for ownerUserId={}", mediaIds, bookId, ownerUserId);
    } catch (Exception e) {
      log.error(
          "Error while appending media to bookId={} ownerUserId={}: ", bookId, ownerUserId, e);
    }
  }

  @Transactional
  public void appendValuationToBook(String bookId, String ownerUserId, Float bookCoinValue) {
    try {
      Optional<Book> myBookOpt = bookRepository.findByBookId(bookId);
      if (myBookOpt.isEmpty()) {
        log.info("Book not found when appending valuation: bookId={}", bookId);
        return;
      }

      Book myBook = myBookOpt.get();
      if (!myBook.getOwnerUserId().equals(ownerUserId)) {
        log.warn(
            "Owner mismatch when appending valuation: bookId={}, ownerUserId={}",
            bookId,
            ownerUserId);
        return;
      }

      myBook.setValuation(bookCoinValue);
      bookRepository.save(myBook);

      outboxService.enqueueEvent(
          AggregateType.BOOK, myBook.getBookId(), "BOOK_VALUATION_FINALIZED", myBook);
      log.info(
          "Appended valuation={} to bookId={} for ownerUserId={}",
          bookCoinValue,
          bookId,
          ownerUserId);
    } catch (Exception e) {
      log.error(
          "Error while appending valuation to bookId={} ownerUserId={}: ", bookId, ownerUserId, e);
    }
  }

  @Transactional(readOnly = true)
  public List<BookResponseWithMedia> getBooksByBulkBookIdOrder(BookIdList bookIdList) {
    log.info("Initiating bulk book fetch for bookIdList={}", bookIdList);

    try {
      if (bookIdList == null
          || bookIdList.getBookIds() == null
          || bookIdList.getBookIds().isEmpty()) {
        log.warn("No bookIds provided. bookIdList={}", bookIdList);
        return List.of();
      }

      // De-dup while preserving input order
      List<String> inputOrder = new ArrayList<>(new LinkedHashSet<>(bookIdList.getBookIds()));

      // Single bulk fetch
      List<Book> booksFound = bookRepository.findAllByBookIdIn(inputOrder);

      // Index by id (for re-ordering to match request)
      Map<String, Book> indexedById =
          booksFound.stream()
              .collect(Collectors.toMap(Book::getBookId, Function.identity(), (a, b) -> a));

      return inputOrder.stream()
          .map(indexedById::get)
          .filter(Objects::nonNull)
          .map(
              book ->
                  BookResponseWithMedia.builder()
                      .bookId(book.getBookId())
                      .title(book.getTitle())
                      .description(book.getDescription())
                      .author(book.getAuthor())
                      .valuation(book.getValuation())
                      .ownerUserId(book.getOwnerUserId())
                      .primaryMediaId(firstOrNull(book.getMediaIds()))
                      .build())
          .toList();
    } catch (Exception e) {
      log.info("Error fetching books with error={}", e.getMessage());
      return List.of(BookResponseWithMedia.builder().build());
    }
  }

  @Transactional
  public Boolean reserveBookForSwap(String bookId) {
    log.info("Initiating reservation for book by bookId={}", bookId);
    try {
      Optional<Book> bookOpt = bookRepository.findByBookId(bookId);
      if (bookOpt.isEmpty()) {
        log.warn("Book not found for reservation and bookId={}", bookId);
        return false;
      }
      Book book = bookOpt.get();
      if (book.getBookStatus() != BookStatus.AVAILABLE) {
        log.warn(
            "Book not available for reservation with bookId={} and currentStatus={}",
            bookId,
            book.getBookStatus());
        return false;
      }

      book.setBookStatus(BookStatus.RESERVED);
      bookRepository.save(book);
      log.info("Book reserved successfully for bookId={}", bookId);
      return true;

    } catch (Exception e) {
      log.error("Error while reserving book with error=", e);
      return false;
    }
  }

  @Transactional
  public Boolean unreserveBookForSwap(String bookId) {
    log.info("Initiating un-reservation for book by bookId={}", bookId);
    try {
      Optional<Book> bookOpt = bookRepository.findByBookId(bookId);
      if (bookOpt.isEmpty()) {
        log.warn("Book not found for un-reservation and bookId={}", bookId);
        return false;
      }
      Book book = bookOpt.get();
      if (book.getBookStatus() != BookStatus.RESERVED) {
        log.warn(
            "Book not reserved currently for un-reservation with bookId={} and currentStatus={}",
            bookId,
            book.getBookStatus());
        return false;
      }

      book.setBookStatus(BookStatus.AVAILABLE);
      bookRepository.save(book);
      log.info("Book un-reserved successfully for bookId={}", bookId);
      return true;

    } catch (Exception e) {
      log.error("Error while un-reserving book with error=", e);
      return false;
    }
  }

  private static <T> T firstOrNull(List<T> list) {
    return (list != null && !list.isEmpty()) ? list.get(0) : null;
  }

  private Book mapRequestToBook(BookRequest bookRequest, String keycloakId) {
    return Book.builder()
        .title(bookRequest.getTitle())
        .description(bookRequest.getDescription())
        .genre(bookRequest.getGenre())
        .author(bookRequest.getAuthor())
        .bookCondition(bookRequest.getBookCondition())
        .valuation(bookRequest.getValuation())
        .bookStatus(BookStatus.AVAILABLE)
        .ownerUserId(keycloakId)
        .mediaIds(bookRequest.getMediaIds())
        .build();
  }

  private BookSimpleResponse mapBookToSimplifiedBook(Book book) {
    log.info("Mapping book to simple response: bookId={}", book.getBookId());
    return BookSimpleResponse.builder()
        .bookId(book.getBookId())
        .title(book.getTitle())
        .valuation(book.getValuation())
        .ownerUserId(book.getOwnerUserId())
        .message("Book saved to database successfully.")
        .build();
  }

  private BookDetailedResponse mapBookToDetailedBook(Book book) {
    log.info("Mapping book to detailed response: bookId={}", book.getBookId());
    return BookDetailedResponse.builder()
        .bookId(book.getBookId())
        .title(book.getTitle())
        .description(book.getDescription())
        .genre(book.getGenre())
        .author(book.getAuthor())
        .bookCondition(book.getBookCondition())
        .valuation(book.getValuation())
        .bookStatus(book.getBookStatus())
        .mediaIds(book.getMediaIds())
        .ownerUserId(book.getOwnerUserId())
        .createdAt(book.getCreatedAt())
        .updatedAt(book.getUpdatedAt())
        .message("Book found successfully with id:" + book.getBookId())
        .build();
  }
}
