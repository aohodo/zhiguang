package com.chuanqing.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String bio;
    private String zgId;
    private String gender;
    private LocalDate birthday;
    private String school;
    private String phone;
    private String email;
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

    public String bio() {
        return bio;
    }

    public String zgId() {
        return zgId;
    }

    public String gender() {
        return gender;
    }

    public LocalDate birthday() {
        return birthday;
    }

    public String school() {
        return school;
    }

    public String phone() {
        return phone;
    }

    public String email() {
        return email;
    }

    public String tagJson() {
        return tagJson;
    }
}