package com.bookswap.backend_for_frontend.api;

import com.bookswap.backend_for_frontend.client.media.dto.response.UploadInitResponseDto;
import com.bookswap.backend_for_frontend.dto.book.request.CreateBookDto;
import com.bookswap.backend_for_frontend.dto.book.request.UploadCompleteRequestDto;
import com.bookswap.backend_for_frontend.dto.book.response.BookCardDto;
import com.bookswap.backend_for_frontend.dto.book.response.CompleteBookDto;
import com.bookswap.backend_for_frontend.dto.home.response.CompletionConfirmDto;
import com.bookswap.backend_for_frontend.service.BookService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bff/books")
@AllArgsConstructor
@Slf4j
public class BookController {
  private final BookService bookService;

  /**
   * Receive a request to create book, save to catalog, call media service to init upload, and
   * return presigned URLs to client.
   */
  @PostMapping("/create/init")
  public ResponseEntity<UploadInitResponseDto> createBook(
      @RequestBody CreateBookDto createBookRequestDto) {
    return ResponseEntity.ok(bookService.createBook(createBookRequestDto));
  }

  @PostMapping("/create/complete")
  public ResponseEntity<CompletionConfirmDto> completeBookCreation(
      @RequestBody UploadCompleteRequestDto uploadCompleteRequestDto) {
    return ResponseEntity.ok(bookService.completeBookCreation(uploadCompleteRequestDto));
  }

  @GetMapping("/get/{bookId}")
  public ResponseEntity<CompleteBookDto> getBookById(@PathVariable String bookId) {
    return ResponseEntity.ok(bookService.getBookById(bookId));
  }

  @GetMapping("/matches/{bookId}")
  public ResponseEntity<List<BookCardDto>> getMatchingBooks(
      @PathVariable String bookId, @RequestParam(defaultValue = "0.15") double tolerance) {
    return ResponseEntity.ok(bookService.getMatchingBooks(bookId, tolerance));
  }

  @GetMapping("/me/get")
  public ResponseEntity<List<BookCardDto>> getMyBooks(Authentication authentication) {
    return ResponseEntity.ok(bookService.getMyBooks(authentication.getName()));
  }

  @DeleteMapping("/me/delete/{bookId}")
  public ResponseEntity<String> deleteMyBook(@PathVariable String bookId) {
    return ResponseEntity.ok(bookService.deleteMyBook(bookId));
  }
}
