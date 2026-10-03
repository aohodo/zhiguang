export interface RelationStatusResponse {
  following: boolean;
  followedBy: boolean;
  mutual: boolean;
}

export interface RelationCountersResponse {
  followings: number;
  followers: number;
  posts: number;
  likedPosts: number;
  favedPosts: number;
}
