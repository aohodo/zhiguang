package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 认证用户响应。
 * <p>
 * 面向客户端展示的基础用户信息，供“我是谁”与首页显示使用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private String zhId;
    private LocalDate birthday;
    private String school;
    private String bio;
    private String gender;
    private String tagJson;

    public Long id() {
        return id;
    }

    public String nickname() {
        return nickname;
    }

    public String avatar() {
        return avatar;
    }

    public String phone() {
        return phone;
    }

    public String zhId() {
        return zhId;
    }

    public LocalDate birthday() {
        return birthday;
    }

    public String school() {
        return school;
    }

    public String bio() {
        return bio;
    }

    public String gender() {
        return gender;
    }

    public String tagJson() {
        return tagJson;
    }

}
