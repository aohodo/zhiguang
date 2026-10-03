package com.chuanqing.service;

import com.chuanqing.entity.RelationEventEntity;
import com.chuanqing.utils.OutboxMessageUtils;
import com.chuanqing.utils.OutboxTopicUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CanalOutboxConsumerService {

    private final ObjectMapper objectMapper;
    private final RelationEventProcessorService processor;
    private final OutboxIdempotencyService idempotencyService;

    public CanalOutboxConsumerService(ObjectMapper objectMapper,
                                      RelationEventProcessorService processor,
                                      OutboxIdempotencyService idempotencyService) {
        this.objectMapper = objectMapper;
        this.processor = processor;
        this.idempotencyService = idempotencyService;
    }

    @KafkaListener(topics = OutboxTopicUtils.CANAL_OUTBOX, groupId = "relation-outbox-consumer")
    public void onMessage(String message, Acknowledgment acknowledgment) {
        List<JsonNode> rows = OutboxMessageUtils.extractRows(objectMapper, message);
        for (JsonNode row : rows) {
            JsonNode payloadNode = row.get("payload");
            if (payloadNode == null || payloadNode.asText().isBlank()) {
                throw new IllegalArgumentException("Outbox row is missing payload");
            }

            JsonNode payload = parsePayload(payloadNode.asText());
            String eventType = text(payload.get("eventType"));
            if (eventType == null) {
                eventType = text(payload.get("type"));
            }
            String entity = text(payload.get("entity"));
            if (!isRelationEvent(entity, eventType)) {
                continue;
            }

            Long fromUserId = asLong(payload.get("fromUserId"));
            Long toUserId = asLong(payload.get("toUserId"));
            if (fromUserId == null || toUserId == null) {
                throw new IllegalArgumentException("Relation event is missing user IDs");
            }

            RelationEventEntity event = new RelationEventEntity(
                    eventType,
                    fromUserId,
                    toUserId,
                    asLong(payload.get("id"))
            );
            Long eventId = firstLong(row.get("id"), payload.get("eventId"));
            idempotencyService.execute("relation", eventId, () -> processor.process(event));
            log.debug("Relation outbox event processed, eventId={}, type={}", eventId, eventType);
        }
        acknowledgment.acknowledge();
    }

    private JsonNode parsePayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid relation outbox payload", exception);
        }
    }

    private boolean isRelationEvent(String entity, String eventType) {
        return "relation".equalsIgnoreCase(entity)
                || "following".equalsIgnoreCase(entity)
                || "FollowCreated".equals(eventType)
                || "FollowCanceled".equals(eventType);
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
