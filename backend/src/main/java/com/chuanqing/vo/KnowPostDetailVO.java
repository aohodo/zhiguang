package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 知文详情响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowPostDetailVO {

    private String id;
    private String title;
    private String description;
    private String contentUrl;
    private List<String> images;
    private List<String> tags;
    private String authorId;
    private String authorAvatar;
    private String authorNickname;
    private String authorTagJson;
    private Long likeCount;
    private Long favoriteCount;
    private Boolean liked;
    private Boolean faved;
    private Boolean isTop;
    private String visible;
    private String type;
    private Instant publishTime;

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String contentUrl() {
        return contentUrl;
    }

    public List<String> images() {
        return images;
    }

    public List<String> tags() {
        return tags;
    }

    public String authorId() {
        return authorId;
    }

    public String authorAvatar() {
        return authorAvatar;
    }

    public String authorNickname() {
        return authorNickname;
    }

    public String authorTagJson() {
        return authorTagJson;
    }

    public Long likeCount() {
        return likeCount;
    }

    public Long favoriteCount() {
        return favoriteCount;
    }

    public Boolean liked() {
        return liked;
    }

    public Boolean faved() {
        return faved;
    }

    public Boolean isTop() {
        return isTop;
    }

    public String visible() {
        return visible;
    }

    public String type() {
        return type;
    }

    public Instant publishTime() {
        return publishTime;
    }
}