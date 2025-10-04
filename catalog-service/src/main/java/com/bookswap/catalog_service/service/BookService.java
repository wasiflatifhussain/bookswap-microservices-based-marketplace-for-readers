package com.bookswap.catalog_service.service;

import com.bookswap.catalog_service.domain.book.Book;
import com.bookswap.catalog_service.domain.book.BookStatus;
import com.bookswap.catalog_service.domain.outbox.AggregateType;
import com.bookswap.catalog_service.dto.event.BookCreatedEvent;
import com.bookswap.catalog_service.dto.event.BookUnlistedEvent;
import com.bookswap.catalog_service.dto.request.BookRequest;
import com.bookswap.catalog_service.dto.response.BookDetailedResponse;
import com.bookswap.catalog_service.dto.response.BookSimpleResponse;
import com.bookswap.catalog_service.repository.BookRepository;
import java.util.List;
import java.util.Optional;
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
              .build();

      outboxService.enqueueEvent(AggregateType.BOOK, bookId, "BOOK_UNLISTED", unlistedEvent);
      return "SUCCESS";
    } catch (Exception e) {
      log.error("Error while deleting book with error=", e);
      return "ERROR: " + e;
    }
  }

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
