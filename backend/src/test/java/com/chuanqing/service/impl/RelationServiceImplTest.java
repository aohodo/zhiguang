package com.chuanqing.service.impl;

import com.chuanqing.mapper.OutboxMapper;
import com.chuanqing.mapper.RelationMapper;
import com.chuanqing.mapper.UserMapper;
import com.chuanqing.utils.SnowflakeIdGeneratorUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationServiceImplTest {

    @Mock
    private RelationMapper relationMapper;

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SnowflakeIdGeneratorUtils idGenerator;

    private ObjectMapper objectMapper;
    private RelationServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new RelationServiceImpl(
                relationMapper,
                outboxMapper,
                redis,
                objectMapper,
                userMapper,
                idGenerator
        );
    }

    @Test
    void followWritesVersionedOutboxEnvelope() throws Exception {
        when(redis.execute(any(DefaultRedisScript.class), eq(List.of("rl:follow:1")), eq("100"), eq("1")))
                .thenReturn(1L);
        when(idGenerator.nextId()).thenReturn(10L, 11L);
        when(relationMapper.insertFollowing(10L, 1L, 2L, 1)).thenReturn(1);
        when(outboxMapper.insert(eq(11L), eq("following"), eq(10L), eq("FollowCreated"), any()))
                .thenReturn(1);

        assertThat(service.follow(1L, 2L)).isTrue();

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxMapper).insert(eq(11L), eq("following"), eq(10L),
                eq("FollowCreated"), payloadCaptor.capture());
        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.get("eventId").asLong()).isEqualTo(11L);
        assertThat(payload.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(payload.get("entity").asText()).isEqualTo("relation");
    }

    @Test
    void outboxFailurePropagatesSoTransactionCanRollback() {
        when(redis.execute(any(DefaultRedisScript.class), eq(List.of("rl:follow:1")), eq("100"), eq("1")))
                .thenReturn(1L);
        when(idGenerator.nextId()).thenReturn(10L, 11L);
        when(relationMapper.insertFollowing(10L, 1L, 2L, 1)).thenReturn(1);
        when(outboxMapper.insert(eq(11L), eq("following"), eq(10L), eq("FollowCreated"), any()))
                .thenThrow(new IllegalStateException("database failure"));

        assertThatThrownBy(() -> service.follow(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database failure");
    }
}
