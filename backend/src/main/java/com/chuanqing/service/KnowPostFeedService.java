package com.chuanqing.service;

import com.chuanqing.vo.FeedPageVO;

/**
 * 知文 Feed 业务接口。
 */
public interface KnowPostFeedService {
    FeedPageVO getPublicFeed(int page, int size, Long currentUserIdNullable);

    FeedPageVO getMyPublished(long userId, int page, int size);
}