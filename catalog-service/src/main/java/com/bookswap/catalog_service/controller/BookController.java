package com.bookswap.catalog_service.controller;

import com.bookswap.catalog_service.dto.request.BookIdList;
import com.bookswap.catalog_service.dto.request.BookRequest;
import com.bookswap.catalog_service.dto.response.BookDetailedResponse;
import com.bookswap.catalog_service.dto.response.BookResponseWithMedia;
import com.bookswap.catalog_service.dto.response.BookSimpleResponse;
import com.bookswap.catalog_service.service.BookService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog")
@AllArgsConstructor
@Slf4j
public class BookController {
  private final BookService bookService;

  @PostMapping("/books")
  public ResponseEntity<BookSimpleResponse> addBook(
      @Valid @RequestBody BookRequest bookRequest, Authentication authentication) {
    String keyCloakId = authentication.getName(); // Remove tight-coupling of service with Keycloak
    return ResponseEntity.ok(bookService.addBook(bookRequest, keyCloakId));
  }

  @GetMapping("/books/{bookId}")
  public ResponseEntity<BookDetailedResponse> getBookByBookId(@PathVariable String bookId) {
    return ResponseEntity.ok(bookService.getBookByBookId(bookId));
  }

  // NOTE: This method can be modified to check for user existence before searching for books by
  // userId
  @GetMapping("/books/user/{userId}")
  public ResponseEntity<BookDetailedResponse[]> getBooksByUserId(@PathVariable String userId) {
    return ResponseEntity.ok(bookService.getBooksByUserId(userId));
  }

  @DeleteMapping("/books/{bookId}")
  public ResponseEntity<String> deleteBookByBookId(@PathVariable String bookId) {
    return ResponseEntity.ok(bookService.deleteBookByBookId(bookId));
  }

  @GetMapping("/books/recent")
  public ResponseEntity<BookDetailedResponse[]> getRecentListedBooks(
      @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(bookService.getRecentListedBooks(limit));
  }

  @GetMapping("/books/matches")
  public ResponseEntity<BookDetailedResponse[]> getMatchingBooks(
      @RequestParam("book-id") String bookId,
      @RequestParam(defaultValue = "0.15") double tolerance) {
    return ResponseEntity.ok(bookService.getMatchingBooks(bookId, tolerance));
  }

  @PostMapping("/books/bulk")
  public ResponseEntity<List<BookResponseWithMedia>> getBooksByBulkBookIdOrder(
      @RequestBody BookIdList bookIdList) {
    return ResponseEntity.ok(bookService.getBooksByBulkBookIdOrder(bookIdList));
  }

  @PostMapping("/books/{bookId}/reserve")
  public ResponseEntity<Boolean> reserveBookForSwap(@PathVariable String bookId) {
    return ResponseEntity.ok(bookService.reserveBookForSwap(bookId));
  }

  @PostMapping("/books/{bookId}/unreserve")
  public ResponseEntity<Boolean> unreserveBookForSwap(@PathVariable String bookId) {
    return ResponseEntity.ok(bookService.unreserveBookForSwap(bookId));
  }
}
