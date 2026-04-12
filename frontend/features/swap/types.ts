export interface SwapBook {
  bookId: string;
  title: string;
  description: string;
  author: string;
  valuation: number;
  ownerUserId: string;
  primaryMediaId: string | null;
  thumbnailUrl?: string | null;
}

export interface SwapItem {
  swapId: string;
  requesterUserId: string;
  responderUserId: string;
  requesterBookId: string;
  responderBookId: string;
  swapStatus: string | null;
  requesterBook: SwapBook | null;
  responderBook: SwapBook | null;
  message: string | null;
}
