package com.chuanqing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchCanalOutboxConsumerServiceTest {

    @Mock
    private SearchIndexService searchIndexService;

    @Mock
    private OutboxIdempotencyService idempotencyService;

    @Mock
    private Acknowledgment acknowledgment;

    private ObjectMapper objectMapper;
    private SearchCanalOutboxConsumerService consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new SearchCanalOutboxConsumerService(
                objectMapper,
                searchIndexService,
                idempotencyService
        );
    }

    @Test
    void processesThenAcknowledges() throws Exception {
        executeIdempotentAction();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventId", 101L);
        payload.put("entity", "knowpost");
        payload.put("op", "upsert");
        payload.put("id", 9L);

        consumer.onMessage(message(101L, payload), acknowledgment);

        verify(idempotencyService).execute(eq("search"), eq(101L), any());
        verify(searchIndexService).upsertKnowPost(9L);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void malformedMessageThrowsAndIsNotAcknowledged() {
        assertThatThrownBy(() -> consumer.onMessage("not-json", acknowledgment))
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

    private void executeIdempotentAction() {
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return true;
        }).when(idempotencyService).execute(any(), any(), any());
    }
}
