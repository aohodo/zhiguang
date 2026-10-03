package com.chuanqing.service.impl;

import com.chuanqing.common.enums.ErrorCodeEnums;
import com.chuanqing.exception.BusinessException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void rejectsFollowingSelf() {
        assertThatThrownBy(() -> service.follow(1L, 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCodeEnums.RELATION_SELF_FOLLOW));

        verify(userMapper, never()).existsById(any());
        verify(relationMapper, never()).activateFollowing(any(), any());
    }

    @Test
    void rejectsMissingTargetUser() {
        when(userMapper.existsById(2L)).thenReturn(false);

        assertThatThrownBy(() -> service.follow(1L, 2L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCodeEnums.USER_NOT_FOUND));

        verifyNoInteractions(redis);
    }

    @Test
    void newFollowWritesVersionedOutboxEnvelope() throws Exception {
        allowFollowCommand();
        when(idGenerator.nextId()).thenReturn(10L, 11L);
        when(relationMapper.insertFollowingIfAbsent(10L, 1L, 2L)).thenReturn(1);
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
        assertThat(payload.get("id").asLong()).isEqualTo(10L);
    }

    @Test
    void reactivatingCanceledRelationUsesPersistedRelationId() {
        allowFollowCommand();
        when(relationMapper.activateFollowing(1L, 2L)).thenReturn(1);
        when(relationMapper.findFollowingId(1L, 2L)).thenReturn(77L);
        when(idGenerator.nextId()).thenReturn(11L);
        when(outboxMapper.insert(eq(11L), eq("following"), eq(77L), eq("FollowCreated"), any()))
                .thenReturn(1);

        assertThat(service.follow(1L, 2L)).isTrue();

        verify(relationMapper, never()).insertFollowingIfAbsent(any(), any(), any());
    }

    @Test
    void repeatedFollowIsSuccessfulWithoutDuplicateEvent() {
        allowFollowCommand();
        when(idGenerator.nextId()).thenReturn(10L);
        when(relationMapper.insertFollowingIfAbsent(10L, 1L, 2L)).thenReturn(0);

        assertThat(service.follow(1L, 2L)).isTrue();

        verify(outboxMapper, never()).insert(any(), any(), any(), any(), any());
    }

    @Test
    void firstUnfollowWritesEventWithPersistedRelationId() {
        when(userMapper.existsById(2L)).thenReturn(true);
        when(relationMapper.cancelFollowing(1L, 2L)).thenReturn(1);
        when(relationMapper.findFollowingId(1L, 2L)).thenReturn(77L);
        when(idGenerator.nextId()).thenReturn(11L);
        when(outboxMapper.insert(eq(11L), eq("following"), eq(77L), eq("FollowCanceled"), any()))
                .thenReturn(1);

        assertThat(service.unfollow(1L, 2L)).isTrue();
    }

    @Test
    void repeatedUnfollowIsSuccessfulWithoutDuplicateEvent() {
        when(userMapper.existsById(2L)).thenReturn(true);
        when(relationMapper.cancelFollowing(1L, 2L)).thenReturn(0);

        assertThat(service.unfollow(1L, 2L)).isTrue();

        verify(outboxMapper, never()).insert(any(), any(), any(), any(), any());
        verify(idGenerator, never()).nextId();
    }

    @Test
    void outboxFailurePropagatesSoTransactionCanRollback() {
        allowFollowCommand();
        when(idGenerator.nextId()).thenReturn(10L, 11L);
        when(relationMapper.insertFollowingIfAbsent(10L, 1L, 2L)).thenReturn(1);
        when(outboxMapper.insert(eq(11L), eq("following"), eq(10L), eq("FollowCreated"), any()))
                .thenThrow(new IllegalStateException("database failure"));

        assertThatThrownBy(() -> service.follow(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database failure");
    }

    private void allowFollowCommand() {
        when(userMapper.existsById(2L)).thenReturn(true);
        when(redis.execute(any(DefaultRedisScript.class), eq(List.of("rl:follow:1")), eq("100"), eq("1")))
                .thenReturn(1L);
    }
}
