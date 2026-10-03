package com.chuanqing.service;

import com.chuanqing.utils.OutboxMessageUtils;
import com.chuanqing.utils.OutboxTopicUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchCanalOutboxConsumerService {

    private final ObjectMapper objectMapper;
    private final SearchIndexService indexService;
    private final OutboxIdempotencyService idempotencyService;

    @KafkaListener(topics = OutboxTopicUtils.CANAL_OUTBOX, groupId = "search-index-consumer")
    public void onMessage(String message, Acknowledgment acknowledgment) {
        List<JsonNode> rows = OutboxMessageUtils.extractRows(objectMapper, message);
        for (JsonNode row : rows) {
            JsonNode payloadNode = row.get("payload");
            if (payloadNode == null || payloadNode.asText().isBlank()) {
                throw new IllegalArgumentException("Outbox row is missing payload");
            }

            JsonNode payload = parsePayload(payloadNode.asText());
            String entity = text(payload.get("entity"));
            if (!"knowpost".equalsIgnoreCase(entity)) {
                continue;
            }

            String operation = text(payload.get("op"));
            Long postId = asLong(payload.get("id"));
            if (postId == null || (!"upsert".equalsIgnoreCase(operation)
                    && !"delete".equalsIgnoreCase(operation))) {
                throw new IllegalArgumentException("Invalid knowpost outbox payload");
            }

            Long eventId = firstLong(row.get("id"), payload.get("eventId"));
            idempotencyService.execute("search", eventId, () -> {
                if ("delete".equalsIgnoreCase(operation)) {
                    indexService.softDeleteKnowPost(postId);
                } else {
                    indexService.upsertKnowPost(postId);
                }
            });
            log.debug("Search outbox event processed, eventId={}, postId={}", eventId, postId);
        }
        acknowledgment.acknowledge();
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid search outbox payload", exception);
        }
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private Long firstLong(JsonNode first, JsonNode second) {
        Long value = asLong(first);
        return value == null ? asLong(second) : value;
    }

    private Long asLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return Long.parseLong(node.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
