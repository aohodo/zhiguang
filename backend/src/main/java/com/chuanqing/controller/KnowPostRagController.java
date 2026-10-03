package com.chuanqing.controller;

import com.chuanqing.service.RagIndexService;
import com.chuanqing.service.RagQueryService;
import com.chuanqing.service.JwtService;
import com.chuanqing.service.KnowPostPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/knowposts")
@Validated
@RequiredArgsConstructor
public class KnowPostRagController {

    private final RagIndexService indexService;
    private final RagQueryService ragQueryService;
    private final KnowPostPermissionService permissionService;
    private final JwtService jwtService;

    /**
     * 单篇知文 RAG 问答（WebFlux + Flux 流式输出）。
     * 示例：GET /api/v1/knowposts/{id}/qa/stream?question=...&topK=5&maxTokens=1024
     */
    @GetMapping(value = "/{id}/qa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> qaStream(@PathVariable("id") long id,
                                 @RequestParam("question") @NotBlank String question,
                                 @RequestParam(value = "topK", defaultValue = "5") @Min(1) @Max(20) int topK,
                                 @RequestParam(value = "maxTokens", defaultValue = "1024") @Min(64) @Max(4096) int maxTokens,
                                 @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt == null ? null : jwtService.extractUserId(jwt);
        return ragQueryService.streamAnswerFlux(id, userId, question, topK, maxTokens);
    }

    /**
     * 手动触发单篇索引重建（返回重建的切片数）。
     */
    @PostMapping("/{id}/rag/reindex")
    public int reindex(@PathVariable("id") long id, @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        permissionService.requireOwner(id, userId);
        return indexService.reindexSinglePost(id);
    }
}
