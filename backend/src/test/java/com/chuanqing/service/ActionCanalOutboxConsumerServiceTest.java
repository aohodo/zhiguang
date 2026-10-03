package com.chuanqing.service;

import com.chuanqing.entity.ContentActionEventEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActionCanalOutboxConsumerServiceTest {

    @Mock
    private ContentActionEventProcessorService processor;
    @Mock
    private OutboxIdempotencyService idempotencyService;
    @Mock
    private Acknowledgment acknowledgment;

    private ObjectMapper objectMapper;
    private ActionCanalOutboxConsumerService consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new ActionCanalOutboxConsumerService(objectMapper, processor, idempotencyService);
    }

    @Test
    void processesActionEnvelopeThenAcknowledges() throws Exception {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return true;
        }).when(idempotencyService).execute(any(), any(), any());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventId", 101L);
        payload.put("eventType", "ContentActionActivated");
        payload.put("entity", "content_action");
        payload.put("actionId", 10L);
        payload.put("userId", 1L);
        payload.put("entityType", "knowpost");
        payload.put("entityId", 9L);
        payload.put("actionType", "like");

        consumer.onMessage(message(101L, payload), acknowledgment);

        ArgumentCaptor<ContentActionEventEntity> eventCaptor =
                ArgumentCaptor.forClass(ContentActionEventEntity.class);
        verify(idempotencyService).execute(eq("action"), eq(101L), any());
        verify(processor).process(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getActionId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().getActionType()).isEqualTo("like");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void malformedOwnEventIsNotAcknowledged() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventType", "ContentActionActivated");
        payload.put("entity", "content_action");

        assertThatThrownBy(() -> consumer.onMessage(message(102L, payload), acknowledgment))
                .isInstanceOf(IllegalArgumentException.class);

        verify(acknowledgment, never()).acknowledge();
    }

    private String message(long eventId, ObjectNode payload) throws Exception {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("id", eventId);
        row.put("payload", objectMapper.writeValueAsString(payload));
        ArrayNode data = objectMapper.createArrayNode().add(row);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("table", "outbox");
        root.put("type", "INSERT");
        root.set("data", data);
        return objectMapper.writeValueAsString(root);
    }
}
