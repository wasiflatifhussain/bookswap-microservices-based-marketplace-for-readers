import { bffFetch } from "@/lib/bff-client";
import { BookDetail, BookMatchCard } from "../types";

export async function getBookById(bookId: string): Promise<BookDetail> {
  return bffFetch<BookDetail>(`/api/bff/books/get/${bookId}`);
}

export async function getBookMatches(
  bookId: string,
  tolerance = 0.15,
): Promise<BookMatchCard[]> {
  return bffFetch<BookMatchCard[]>(
    `/api/bff/books/matches/${bookId}?tolerance=${tolerance}`,
  );
}
