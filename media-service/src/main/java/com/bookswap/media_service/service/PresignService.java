package com.bookswap.media_service.service;

import java.net.URL;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresignService {
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  // TODO: might require constructor based injection
  @Value("${aws.s3.bucket-name}")
  private String bucketName;

  public URL presignPutUrl(String objectKey, String contentType, Duration ttl) {
    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(objectKey)
              .contentType(contentType)
              .build();

      PresignedPutObjectRequest presignRequest =
          s3Presigner.presignPutObject(
              b -> b.signatureDuration(ttl).putObjectRequest(putObjectRequest));

      return presignRequest.url();
    } catch (Exception e) {
      log.error(
          "Failed to generate presigned PUT URL for key {}: {}", objectKey, e.getMessage(), e);
      throw new RuntimeException("Failed to generate presigned PUT URL", e);
    }
  }

  public URL presignGetUrl(String objectKey, Duration ttl) {
    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(bucketName).key(objectKey).build();

      PresignedGetObjectRequest presignRequest =
          s3Presigner.presignGetObject(
              b -> b.signatureDuration(ttl).getObjectRequest(getObjectRequest));

      return presignRequest.url();
    } catch (Exception e) {
      log.error(
          "Failed to generate presigned GET URL for key {}: {}", objectKey, e.getMessage(), e);
      throw new RuntimeException("Failed to generate presigned GET URL", e);
    }
  }

  public HeadObjectResponse headObjectResponse(String objectKey) {
    try {
      return s3Client.headObject(
          HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build());
    } catch (Exception e) {
      log.error("Failed to get head object for key {}: {}", objectKey, e.getMessage(), e);
      throw new RuntimeException("Failed to get head object", e);
    }
  }
}
