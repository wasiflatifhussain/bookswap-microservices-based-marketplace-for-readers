package com.bookswap.backend_for_frontend.client.media.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadConfirmDto {
  private String bookId;
  private int totalCount;
  private int successCount;
  private List<Item> items;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Item {
    private String mediaId;
    private String bookId;
    private String status;
    private String message;
  }
}
