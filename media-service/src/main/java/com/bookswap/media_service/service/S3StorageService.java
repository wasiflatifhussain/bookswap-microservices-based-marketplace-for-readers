package com.bookswap.media_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {
  private final S3Client s3Client;

  @Value("${aws.s3.bucket-name}")
  private String bucketName;

  public void deleteObject(String objectKey) {
    try {
      s3Client.deleteObject(b -> b.bucket(bucketName).key(objectKey));
      log.info("Deleted S3 object with objectKey={}", objectKey);
    } catch (Exception e) {
      log.error(
          "Failed to delete S3 object with objectKey={} with error={}", objectKey, e.getMessage());
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
