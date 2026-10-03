package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.chuanqing.vo.FeedItemVO;
import java.util.List;

/**
 * 搜索响应：包含结果列表与分页游标。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchVO {

    private List<FeedItemVO> items;
    private String nextAfter;
    private boolean hasMore;

    public List<FeedItemVO> items() {
        return items;
    }

    public String nextAfter() {
        return nextAfter;
    }

    public boolean hasMore() {
        return hasMore;
    }
}