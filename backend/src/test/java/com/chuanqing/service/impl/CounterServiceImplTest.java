package com.chuanqing.service.impl;

import com.chuanqing.common.enums.ErrorCodeEnums;
import com.chuanqing.entity.ContentActionEntity;
import com.chuanqing.exception.BusinessException;
import com.chuanqing.mapper.ContentActionMapper;
import com.chuanqing.mapper.OutboxMapper;
import com.chuanqing.service.KnowPostPermissionService;
import com.chuanqing.utils.SnowflakeIdGeneratorUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CounterServiceImplTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private RedissonClient redisson;
    @Mock
    private ContentActionMapper actionMapper;
    @Mock
    private KnowPostPermissionService permissionService;
    @Mock
    private OutboxMapper outboxMapper;
    @Mock
    private SnowflakeIdGeneratorUtils idGenerator;

    private ObjectMapper objectMapper;
    private CounterServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new CounterServiceImpl(
                redis, redisson, actionMapper, permissionService,
                outboxMapper, idGenerator, objectMapper);
    }

    @Test
    void rejectsUnsupportedEntityType() {
        assertThatThrownBy(() -> service.like("comment", "9", 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCodeEnums.BAD_REQUEST));

        verify(permissionService, never()).requireReadable(any(Long.class), any(Long.class));
    }

    @Test
    void rejectsInvalidEntityId() {
        assertThatThrownBy(() -> service.like("knowpost", "abc", 1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCodeEnums.BAD_REQUEST));
    }

    @Test
    void newLikeWritesVersionedOutbox() throws Exception {
        when(idGenerator.nextId()).thenReturn(10L, 11L);
        when(actionMapper.insertIfAbsent(10L, 1L, "knowpost", 9L, "like")).thenReturn(1);
        when(outboxMapper.insert(eq(11L), eq("content_action"), eq(10L),
                eq("ContentActionActivated"), any())).thenReturn(1);

        assertThat(service.like("knowpost", "9", 1L)).isTrue();

        verify(permissionService).requireReadable(9L, 1L);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxMapper).insert(eq(11L), eq("content_action"), eq(10L),
                eq("ContentActionActivated"), payloadCaptor.capture());
        JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
        assertThat(payload.get("entity").asText()).isEqualTo("content_action");
        assertThat(payload.get("entityType").asText()).isEqualTo("knowpost");
        assertThat(payload.get("entityId").asLong()).isEqualTo(9L);
        assertThat(payload.get("actionType").asText()).isEqualTo("like");
    }

    @Test
    void repeatedLikeDoesNotWriteDuplicateEvent() {
        when(idGenerator.nextId()).thenReturn(10L);
        when(actionMapper.insertIfAbsent(10L, 1L, "knowpost", 9L, "like")).thenReturn(0);

        assertThat(service.like("knowpost", "9", 1L)).isFalse();

        verify(outboxMapper, never()).insert(any(), any(), any(), any(), any());
    }

    @Test
    void reactivatedFavoriteUsesPersistedActionId() {
        ContentActionEntity action = action(77L, 1);
        when(actionMapper.activateExisting(1L, "knowpost", 9L, "fav")).thenReturn(1);
        when(actionMapper.findByKey(1L, "knowpost", 9L, "fav")).thenReturn(action);
        when(idGenerator.nextId()).thenReturn(11L);
        when(outboxMapper.insert(eq(11L), eq("content_action"), eq(77L),
                eq("ContentActionActivated"), any())).thenReturn(1);

        assertThat(service.fav("knowpost", "9", 1L)).isTrue();

        verify(actionMapper, never()).insertIfAbsent(any(), any(), any(), any(), any());
    }

    @Test
    void unlikeOnlyWritesEventOnRealTransition() {
        when(actionMapper.deactivate(1L, "knowpost", 9L, "like")).thenReturn(1);
        when(actionMapper.findByKey(1L, "knowpost", 9L, "like")).thenReturn(action(77L, 0));
        when(idGenerator.nextId()).thenReturn(11L);
        when(outboxMapper.insert(eq(11L), eq("content_action"), eq(77L),
                eq("ContentActionDeactivated"), any())).thenReturn(1);

        assertThat(service.unlike("knowpost", "9", 1L)).isTrue();
    }

    @Test
    void repeatedUnfavoriteDoesNotWriteEvent() {
        when(actionMapper.deactivate(1L, "knowpost", 9L, "fav")).thenReturn(0);

        assertThat(service.unfav("knowpost", "9", 1L)).isFalse();

        verify(outboxMapper, never()).insert(any(), any(), any(), any(), any());
    }

    @Test
    void outboxFailurePropagatesForTransactionRollback() {
        when(idGenerator.nextId()).thenReturn(10L, 11L);
        when(actionMapper.insertIfAbsent(10L, 1L, "knowpost", 9L, "like")).thenReturn(1);
        when(outboxMapper.insert(eq(11L), eq("content_action"), eq(10L),
                eq("ContentActionActivated"), any())).thenThrow(new IllegalStateException("database failure"));

        assertThatThrownBy(() -> service.like("knowpost", "9", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database failure");
    }

    private ContentActionEntity action(long id, int status) {
        ContentActionEntity action = new ContentActionEntity();
        action.setId(id);
        action.setActionStatus(status);
        return action;
    }
}
