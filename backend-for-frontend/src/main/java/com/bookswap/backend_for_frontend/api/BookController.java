package com.bookswap.backend_for_frontend.api;

import com.bookswap.backend_for_frontend.client.media.dto.response.UploadInitResponseDto;
import com.bookswap.backend_for_frontend.dto.book.request.CreateBookDto;
import com.bookswap.backend_for_frontend.dto.book.request.UploadCompleteRequestDto;
import com.bookswap.backend_for_frontend.dto.home.response.CompletionConfirmDto;
import com.bookswap.backend_for_frontend.service.BookService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
  public UploadInitResponseDto createBook(@RequestBody CreateBookDto createBookRequestDto) {
    return bookService.createBook(createBookRequestDto);
  }

  @PostMapping("/create/complete")
  public CompletionConfirmDto completeBookCreation(
      @RequestBody UploadCompleteRequestDto uploadCompleteRequestDto) {
    return bookService.completeBookCreation(uploadCompleteRequestDto);
  }
}
