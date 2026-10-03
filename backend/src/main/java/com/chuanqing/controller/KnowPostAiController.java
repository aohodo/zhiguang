package com.chuanqing.controller;

import com.chuanqing.dto.DescriptionSuggestDTO;
import com.chuanqing.vo.DescriptionSuggestVO;
import com.chuanqing.service.KnowPostDescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/v1/knowposts", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class KnowPostAiController {

    private final KnowPostDescriptionService descriptionService;

    /**
     * 生成不超过 50 字的知文描述。
     * 需要鉴权（默认策略），防止匿名滥用。
     */
    @PostMapping(path = "/description/suggest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DescriptionSuggestVO suggest(@Valid @RequestBody DescriptionSuggestDTO req) {
        String desc = descriptionService.generateDescription(req.content());
        return new DescriptionSuggestVO(desc);
    }
}