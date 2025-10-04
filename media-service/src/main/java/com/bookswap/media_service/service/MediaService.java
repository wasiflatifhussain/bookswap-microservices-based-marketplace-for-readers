package com.bookswap.media_service.service;

import com.bookswap.media_service.domain.media.Media;
import com.bookswap.media_service.domain.media.Status;
import com.bookswap.media_service.domain.outbox.AggregateType;
import com.bookswap.media_service.dto.download.MediaViewResponse;
import com.bookswap.media_service.dto.event.MediaStoredEvent;
import com.bookswap.media_service.dto.upload.CompleteResponse;
import com.bookswap.media_service.dto.upload.UploadInitRequest;
import com.bookswap.media_service.dto.upload.UploadInitResponse;
import com.bookswap.media_service.repository.MediaRepository;
import java.net.URL;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {
  private final MediaRepository mediaRepository;
  private final PresignService presignService;
  private final OutboxService outboxService;

  @Value("${media.upload.ttl.minutes:10}")
  private int uploadTtlMinutes;

  @Value("${media.allowed.mimes:image/png,image/jpeg}")
  private String allowedMimes;

  @Value("${media.max.file.size.bytes:5242880}")
  private long maxFileSizeBytes;

  @Value("${media.download.ttl.minutes:30}")
  private int downloadTtlMinutes;

  public UploadInitResponse initUploads(
      String bookId, String ownerUserId, UploadInitRequest uploadInitRequest) {

    final Set<String> allowedMimeSet = parseAllowedMimes(allowedMimes);
    final Duration ttl = Duration.ofMinutes(uploadTtlMinutes);
    final OffsetDateTime expiresAt = OffsetDateTime.now().plus(ttl);

    log.info(
        "Initiating upload for bookId={} ownerUserId={} with {} files",
        bookId,
        ownerUserId,
        uploadInitRequest == null || uploadInitRequest.files() == null
            ? 0
            : uploadInitRequest.files().size());

    List<UploadInitResponse.Item> results = new ArrayList<>();

    if (uploadInitRequest == null
        || uploadInitRequest.files() == null
        || uploadInitRequest.files().isEmpty()) {
      return new UploadInitResponse(
          bookId,
          List.of(UploadInitResponse.Item.failed("N/A", "EMPTY_REQUEST", "No files provided.")));
    }

    for (UploadInitRequest.FileItem file : uploadInitRequest.files()) {
      String clientRef = file.clientRef();
      String mediaId = UUID.randomUUID().toString();
      try {
        // Validate inputs
        String normalizedMime = normalizeMime(file.mimeType());
        Optional<String> validationError =
            validateFile(file, allowedMimeSet, normalizedMime, maxFileSizeBytes);
        if (validationError.isPresent()) {
          results.add(
              UploadInitResponse.Item.failed(
                  clientRef, "VALIDATION_FAILED", validationError.get()));
          continue;
        }

        // IDs & keys
        String objectKey = buildObjectKey(bookId, mediaId, safeExtension(file.name()));

        // Persist PENDING row up front
        Media mediaRow =
            Media.builder()
                .mediaId(mediaId)
                .bookId(bookId)
                .ownerUserId(ownerUserId)
                .objectKey(objectKey)
                .status(Status.PENDING)
                .build();
        mediaRepository.save(mediaRow);

        // Presign
        URL url = presignService.presignPutUrl(objectKey, normalizedMime, ttl);
        UploadInitResponse.Item.Headers headers =
            new UploadInitResponse.Item.Headers(normalizedMime);

        results.add(
            UploadInitResponse.Item.ready(
                clientRef, mediaId, objectKey, url.toString(), headers, expiresAt));

      } catch (Exception e) {
        // Clean up the just-created row so no orphan PENDING
        log.error("Error during presign for mediaId={} e=", mediaId, e);
        mediaRepository.deleteById(mediaId);
        results.add(
            UploadInitResponse.Item.failed(
                clientRef, "S3_PRESIGN_FAILED", "Could not create upload ticket."));
      }
    }

    return new UploadInitResponse(bookId, results);
  }

  public CompleteResponse completeUpload(String mediaId, String ownerUserId) {
    log.info("Completing upload verification for mediaId={} ownerUserId={}", mediaId, ownerUserId);

    try {
      Optional<Media> mediaOpt = mediaRepository.findById(mediaId);
      if (mediaOpt.isEmpty()) {
        return makeCompleteResponse(mediaId, null, Status.FAILED, "Media not found.");
      }

      Media mediaFound = mediaOpt.get();
      if (mediaFound.getStatus() == Status.STORED) {
        return makeCompleteResponse(
            mediaId, mediaFound.getBookId(), Status.STORED, "Media already exists.");
      }

      try {
        HeadObjectResponse head = presignService.headObjectResponse(mediaFound.getObjectKey());
        if (head == null) {
          return makeCompleteResponse(
              mediaId, mediaFound.getBookId(), Status.FAILED, "Object not found.");
        }
        mediaFound.setMimeType(head.contentType());
        mediaFound.setSizeBytes(head.contentLength());
        mediaFound.setStatus(Status.STORED);
        mediaRepository.save(mediaFound);

        MediaStoredEvent mediaStoredEvent =
            MediaStoredEvent.builder()
                .bookId(mediaFound.getBookId())
                .ownerUserId(mediaFound.getOwnerUserId())
                .mediaId(mediaFound.getMediaId())
                .build();

        outboxService.enqueueEvent(
            AggregateType.MEDIA,
            mediaFound.getMediaId(),
            mediaFound.getBookId(),
            "MEDIA_STORED",
            mediaStoredEvent);

        return makeCompleteResponse(
            mediaFound.getMediaId(),
            mediaFound.getBookId(),
            Status.STORED,
            "Media verified and stored successfully.");
      } catch (Exception e) {
        log.warn(
            "HeadObject failed for mediaId={} key={} (likely not uploaded yet).",
            mediaId,
            mediaFound.getObjectKey(),
            e);

        return makeCompleteResponse(
            mediaId,
            mediaFound.getBookId(),
            Status.FAILED,
            "Upload not found/accessible in storage.");
      }

    } catch (Exception e) {
      log.error("CompleteUpload failed for mediaId={} with error:", mediaId, e);
      return makeCompleteResponse(
          mediaId, null, Status.FAILED, "Unexpected error completing upload.");
    }
  }

  public List<MediaViewResponse> getMediaByBookId(String bookId) {
    log.info("Fetching media for bookId={}", bookId);

    try {
      List<Media> mediaList = mediaRepository.findByBookIdAndStatus(bookId, Status.STORED);

      if (mediaList.isEmpty()) {
        log.info("No stored media found for bookId={}", bookId);
        return List.of();
      }

      Duration ttl = Duration.ofMinutes(downloadTtlMinutes);
      OffsetDateTime expiresAt = OffsetDateTime.now().plus(ttl);

      return mediaList.stream()
          .map(
              media -> {
                try {
                  URL presignedUrl = presignService.presignGetUrl(media.getObjectKey(), ttl);

                  return MediaViewResponse.builder()
                      .mediaId(media.getMediaId())
                      .mimeType(media.getMimeType())
                      .presignedUrl(presignedUrl.toString())
                      .build();

                } catch (Exception e) {
                  log.warn(
                      "Failed to presign URL for mediaId={} bookId={}",
                      media.getMediaId(),
                      bookId,
                      e);
                  return null;
                }
              })
          .filter(Objects::nonNull)
          .toList();

    } catch (Exception e) {
      log.error("Error fetching media for bookId={}:", bookId, e);
      return List.of();
    }
  }

  private static String normalizeMime(String mime) {
    return (mime == null) ? "" : mime.trim().toLowerCase();
  }

  private static Set<String> parseAllowedMimes(String allowedMimes) {
    Set<String> mimeSet = new HashSet<>();
    if (allowedMimes != null && !allowedMimes.isBlank()) {
      for (String mime : allowedMimes.split(",")) {
        String normalizedMime = normalizeMime(mime);
        if (!normalizedMime.isEmpty()) mimeSet.add(normalizedMime);
      }
    }
    return mimeSet;
  }

  private static Optional<String> validateFile(
      UploadInitRequest.FileItem file,
      Set<String> allowedMimeSet,
      String normalizedMime,
      long maxFileSizeBytes) {

    if (file == null) {
      return Optional.of("File item is null.");
    }
    if (file.name() == null || file.name().isBlank()) {
      return Optional.of("Filename is required.");
    }
    if (!allowedMimeSet.contains(normalizedMime)) {
      return Optional.of("Only image/jpeg and image/png are allowed.");
    }
    if (file.sizeBytes() <= 0) {
      return Optional.of("File size must be positive.");
    }
    if (file.sizeBytes() > maxFileSizeBytes) {
      return Optional.of("File exceeds max size of " + maxFileSizeBytes + " bytes.");
    }

    return Optional.empty();
  }

  private static String safeExtension(String filename) {
    if (filename == null) return "";
    int dot = filename.lastIndexOf('.');
    if (dot == -1 || dot == filename.length() - 1) return "";
    String ext = filename.substring(dot).trim();
    // basic hardening
    if (ext.length() > 10) return "";
    return ext.toLowerCase();
  }

  private static String buildObjectKey(String bookId, String mediaId, String extension) {
    String safeBook = (bookId == null || bookId.isBlank()) ? "unknown" : bookId;
    String ext = (extension == null) ? "" : extension;
    return "%s/%s%s".formatted(safeBook, mediaId, ext);
  }

  private CompleteResponse makeCompleteResponse(
      String mediaId, String bookId, Status status, String message) {
    return CompleteResponse.builder()
        .mediaId(mediaId)
        .bookId(bookId)
        .status(status)
        .message(message)
        .build();
  }
}
