package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 帖子元数据更新请求（部分字段可选）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowPostPatchDTO {

    private String title;
    private Long tagId;
    private @Size(max = 20) List<String> tags;
    private @Size(max = 20) List<String> imgUrls;
    private String visible;
    private Boolean isTop;
    private String description;

    public String title() {
        return title;
    }

    public Long tagId() {
        return tagId;
    }

    public List<String> tags() {
        return tags;
    }

    public List<String> imgUrls() {
        return imgUrls;
    }

    public String visible() {
        return visible;
    }

    public Boolean isTop() {
        return isTop;
    }

    public String description() {
        return description;
    }
}