import { bffFetch } from "@/lib/bff-client";
import { FeedItem } from "../types";

export async function getHomeFeed(limit = 20): Promise<FeedItem[]> {
  return bffFetch<FeedItem[]>(`/api/bff/home/feed?limit=${limit}`);
}
