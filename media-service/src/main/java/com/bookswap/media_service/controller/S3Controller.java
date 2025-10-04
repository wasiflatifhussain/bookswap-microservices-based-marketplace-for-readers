package com.bookswap.media_service.controller;

import com.bookswap.media_service.service.S3Service;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
@Slf4j
public class S3Controller {
  private final S3Service s3Service;

  @PostMapping("/upload")
  public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file)
      throws IOException {
    s3Service.uploadFile(file);
    return ResponseEntity.ok("File uploaded successfully!");
  }

  @GetMapping("/download/{filename}")
  public ResponseEntity<byte[]> download(@PathVariable String filename) {
    byte[] data = s3Service.downloadFile(filename);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .body(data);
  }
}
