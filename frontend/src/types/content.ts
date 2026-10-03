export type ContentTag = string;

export interface Mentor {
  id: string;
  name: string;
  alias?: string;
  description?: string;
}

export type ContentKind = "course" | "article" | "video";

export interface ContentItem {
  id: string;
  title: string;
  summary: string;
  tags: ContentTag[];
  isFree: boolean;
  coverImage?: string;
  likes: number;
  views: number;
  mentor: Mentor;
  kind: ContentKind;
  category: string;
  createdAt: string;
  body?: string;
}

export interface SearchSuggestion {
  id: string;
  label: string;
}
