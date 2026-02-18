export interface FeedItem {
  bookId: string;
  title: string;
  description: string;
  genre: string;
  author: string;
  bookCondition: string;
  valuation: number;
  bookStatus: string;
  thumbnailUrl: string | null;
  ownerUserId: string;
}
