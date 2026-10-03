package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DescriptionSuggestDTO {

    private @NotBlank(message = "content 不能为空") String content;

    public String content() {
        return content;
    }
}