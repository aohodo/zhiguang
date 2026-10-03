package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首页 Feed 分页响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedPageVO {

    private List<FeedItemVO> items;
    private int page;
    private int size;
    private boolean hasMore;

    public List<FeedItemVO> items() {
        return items;
    }

    public int page() {
        return page;
    }

    public int size() {
        return size;
    }

    public boolean hasMore() {
        return hasMore;
    }
}