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
