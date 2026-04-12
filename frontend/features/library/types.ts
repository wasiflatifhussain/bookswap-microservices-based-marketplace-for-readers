export interface LibraryBook {
  bookId: string;
  title: string;
  description: string;
  genre: string;
  author: string;
  bookCondition: string;
  valuation: number;
  bookStatus: string;
  thumbnailUrl: string | null;
}

export interface UploadInitItem {
  clientRef: string;
  status: "READY" | "FAILED" | string;
  mediaId: string;
  objectKey: string;
  presignedPutUrl: string;
  requiredHeaders?: {
    contentType?: string;
  };
  expiresAt?: string;
  errorCode?: string;
  errorMessage?: string;
}

export interface UploadInitResponse {
  bookId: string;
  results: UploadInitItem[];
}

export interface UploadCompleteResponse {
  bookId: string;
  totalCount: number;
  successCount: number;
}
