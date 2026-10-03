import type { FeedItem } from "@/types/knowPost";

export interface SearchResponse {
  items: FeedItem[];
  nextAfter: string | null;
  hasMore: boolean;
}

export interface SuggestResponse {
  items: string[];
}
