package com.bookswap.media_service.service;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Slf4j
public class S3Service {
  private final S3Client s3Client;
  private final String bucketName;
  
  public S3Service(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
  }

  public void uploadFile(MultipartFile file) throws IOException {
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucketName).key(file.getOriginalFilename()).build(),
        RequestBody.fromBytes(file.getBytes()));
  }

  public byte[] downloadFile(String key) {
    ResponseBytes<GetObjectResponse> objectAsBytes =
        s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key(key).build());
    return objectAsBytes.asByteArray();
  }
}
