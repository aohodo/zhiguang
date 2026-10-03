package com.chuanqing.service;

import com.chuanqing.entity.ContentActionEventEntity;
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
public class ActionCanalOutboxConsumerService {

    private final ObjectMapper objectMapper;
    private final ContentActionEventProcessorService processor;
    private final OutboxIdempotencyService idempotencyService;

    @KafkaListener(topics = OutboxTopicUtils.CANAL_OUTBOX, groupId = "action-outbox-consumer")
    public void onMessage(String message, Acknowledgment acknowledgment) {
        List<JsonNode> rows = OutboxMessageUtils.extractRows(objectMapper, message);
        for (JsonNode row : rows) {
            JsonNode payloadNode = row.get("payload");
            if (payloadNode == null || payloadNode.asText().isBlank()) {
                throw new IllegalArgumentException("Outbox row is missing payload");
            }
            JsonNode payload = parsePayload(payloadNode.asText());
            String entity = text(payload.get("entity"));
            String eventType = text(payload.get("eventType"));
            if (!"content_action".equalsIgnoreCase(entity)
                    && !"ContentActionActivated".equals(eventType)
                    && !"ContentActionDeactivated".equals(eventType)) {
                continue;
            }

            ContentActionEventEntity event = new ContentActionEventEntity(
                    requiredLong(payload, "actionId"),
                    requiredLong(payload, "userId"),
                    requiredText(payload, "entityType"),
                    requiredLong(payload, "entityId"),
                    requiredText(payload, "actionType")
            );
            Long eventId = optionalLong(row.get("id"));
            if (eventId == null) {
                eventId = requiredLong(payload, "eventId");
            }
            Long finalEventId = eventId;
            idempotencyService.execute("action", finalEventId, () -> processor.process(event));
            log.debug("Action outbox event processed, eventId={}, type={}", eventId, eventType);
        }
        acknowledgment.acknowledge();
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid action outbox payload", exception);
        }
    }

    private String requiredText(JsonNode payload, String field) {
        String value = text(payload.get(field));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Action event is missing " + field);
        }
        return value;
    }

    private Long requiredLong(JsonNode payload, String field) {
        Long value = optionalLong(payload.get(field));
        if (value == null) {
            throw new IllegalArgumentException("Action event is missing " + field);
        }
        return value;
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private Long optionalLong(JsonNode node) {
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
