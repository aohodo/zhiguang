package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowPostTopPatchDTO {

    private @NotNull Boolean isTop;

    public Boolean isTop() {
        return isTop;
    }
}