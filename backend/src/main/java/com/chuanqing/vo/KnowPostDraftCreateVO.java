package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建草稿响应：返回新建的帖子 ID（字符串避免前端精度丢失）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowPostDraftCreateVO {

    private String id;

    public String id() {
        return id;
    }


}