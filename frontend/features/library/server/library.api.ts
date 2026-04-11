import { bffFetch } from "@/lib/bff-client";
import { LibraryBook } from "../types";

export async function getMyBooks(): Promise<LibraryBook[]> {
  return bffFetch<LibraryBook[]>("/api/bff/books/me/get");
}
