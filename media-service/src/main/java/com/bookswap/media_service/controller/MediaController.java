package com.bookswap.media_service.controller;

import com.bookswap.media_service.dto.upload.CompleteResponse;
import com.bookswap.media_service.dto.upload.UploadInitRequest;
import com.bookswap.media_service.dto.upload.UploadInitResponse;
import com.bookswap.media_service.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/media")
public class MediaController {
  private final MediaService mediaService;

  @PostMapping("/uploads/{bookId}/init")
  public ResponseEntity<UploadInitResponse> initUploads(
      @PathVariable String bookId,
      @RequestBody UploadInitRequest body,
      Authentication authentication) {
    String ownerUserId = authentication.getName();
    return ResponseEntity.ok(mediaService.initUploads(bookId, ownerUserId, body));
  }

  @PostMapping("/uploads/{mediaId}/complete")
  public ResponseEntity<CompleteResponse> completeUpload(
      @PathVariable String mediaId, Authentication authentication) {
    String ownerUserId = authentication.getName();
    return ResponseEntity.ok(mediaService.completeUpload(mediaId, ownerUserId));
  }
}
