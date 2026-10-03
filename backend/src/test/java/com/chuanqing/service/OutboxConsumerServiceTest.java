package com.chuanqing.service;

import com.chuanqing.entity.RelationEventEntity;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OutboxConsumerServiceTest {

    @Mock
    private SearchIndexService searchIndexService;

    @Mock
    private RelationEventProcessorService relationProcessor;

    @Mock
    private OutboxIdempotencyService idempotencyService;

    @Mock
    private Acknowledgment acknowledgment;

    private ObjectMapper objectMapper;
    private SearchCanalOutboxConsumerService searchConsumer;
    private CanalOutboxConsumerService relationConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        searchConsumer = new SearchCanalOutboxConsumerService(
                objectMapper,
                searchIndexService,
                idempotencyService
        );
        relationConsumer = new CanalOutboxConsumerService(
                objectMapper,
                relationProcessor,
                idempotencyService
        );
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return true;
        }).when(idempotencyService).execute(any(), any(), any());
    }

    @Test
    void searchConsumerProcessesThenAcknowledges() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventId", 101L);
        payload.put("entity", "knowpost");
        payload.put("op", "upsert");
        payload.put("id", 9L);

        searchConsumer.onMessage(message(101L, payload), acknowledgment);

        verify(idempotencyService).execute(eq("search"), eq(101L), any());
        verify(searchIndexService).upsertKnowPost(9L);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void relationConsumerIgnoresKnowpostWithoutBlockingItsGroup() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("entity", "knowpost");
        payload.put("op", "upsert");
        payload.put("id", 9L);

        relationConsumer.onMessage(message(102L, payload), acknowledgment);

        verify(relationProcessor, never()).process(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void relationConsumerProcessesEnvelopeAndAcknowledges() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("eventId", 103L);
        payload.put("entity", "relation");
        payload.put("eventType", "FollowCreated");
        payload.put("fromUserId", 1L);
        payload.put("toUserId", 2L);
        payload.put("id", 3L);

        relationConsumer.onMessage(message(103L, payload), acknowledgment);

        ArgumentCaptor<RelationEventEntity> eventCaptor = ArgumentCaptor.forClass(RelationEventEntity.class);
        verify(relationProcessor).process(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo("FollowCreated");
        assertThat(eventCaptor.getValue().fromUserId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().toUserId()).isEqualTo(2L);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void malformedMessageThrowsAndIsNotAcknowledged() {
        assertThatThrownBy(() -> searchConsumer.onMessage("not-json", acknowledgment))
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
