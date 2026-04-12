import { bffFetch } from "@/lib/bff-client";
import { getBookById } from "@/features/catalog/server/catalog.api";
import { SwapItem } from "../types";

function normalize(items: SwapItem[]): SwapItem[] {
  return items.filter((item) => Boolean(item?.swapId));
}

async function enrichWithThumbnails(items: SwapItem[]): Promise<SwapItem[]> {
  const normalized = normalize(items);

  const bookIds = new Set<string>();
  for (const item of normalized) {
    if (item.requesterBookId) bookIds.add(item.requesterBookId);
    if (item.responderBookId) bookIds.add(item.responderBookId);
  }

  const entries = await Promise.all(
    Array.from(bookIds).map(async (bookId) => {
      try {
        const detail = await getBookById(bookId);
        return [bookId, detail.mediaUrls?.[0] ?? null] as const;
      } catch {
        return [bookId, null] as const;
      }
    }),
  );

  const thumbnailByBookId = new Map<string, string | null>(entries);

  return normalized.map((item) => ({
    ...item,
    requesterBook: item.requesterBook
      ? {
          ...item.requesterBook,
          thumbnailUrl:
            thumbnailByBookId.get(item.requesterBook.bookId) ??
            item.requesterBook.thumbnailUrl ??
            null,
        }
      : null,
    responderBook: item.responderBook
      ? {
          ...item.responderBook,
          thumbnailUrl:
            thumbnailByBookId.get(item.responderBook.bookId) ??
            item.responderBook.thumbnailUrl ??
            null,
        }
      : null,
  }));
}

export async function getMySentSwaps(): Promise<SwapItem[]> {
  const data = await bffFetch<SwapItem[]>("/api/bff/swap/me/sent");
  return enrichWithThumbnails(data);
}

export async function getMyReceivedSwaps(): Promise<SwapItem[]> {
  const data = await bffFetch<SwapItem[]>("/api/bff/swap/me/received");
  return enrichWithThumbnails(data);
}
