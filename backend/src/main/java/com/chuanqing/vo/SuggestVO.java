package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 联想响应：返回候选标题列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestVO {

    private List<String> items;

    public List<String> items() {
        return items;
    }
}