package com.chuanqing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowPostVisibilityPatchDTO {

    private @NotBlank String visible;

    public String visible() {
        return visible;
    }
}