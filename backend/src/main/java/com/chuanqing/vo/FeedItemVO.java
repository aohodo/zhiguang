package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 首页 Feed 单条记录。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedItemVO {

    private String id;
    private String title;
    private String description;
    private String coverImage;
    private List<String> tags;
    private String authorAvatar;
    private String authorNickname;
    private String tagJson;
    private Long likeCount;
    private Long favoriteCount;
    private Boolean liked;
    private Boolean faved;
    private Boolean isTop;

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String coverImage() {
        return coverImage;
    }

    public List<String> tags() {
        return tags;
    }

    public String authorAvatar() {
        return authorAvatar;
    }

    public String authorNickname() {
        return authorNickname;
    }

    public String tagJson() {
        return tagJson;
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
}