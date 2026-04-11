export interface BookDetail {
  bookId: string;
  title: string;
  description: string;
  genre: string;
  author: string;
  bookCondition: string;
  valuation: number;
  bookStatus: string;
  ownerUserId: string;
  mediaUrls: string[];
  createdAt: string;
  updatedAt: string;
}

export interface BookMatchCard {
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
