package com.chuanqing.service;

import com.chuanqing.common.enums.ErrorCodeEnums;
import com.chuanqing.entity.KnowPostDetailEntity;
import com.chuanqing.entity.UserEntity;
import com.chuanqing.exception.BusinessException;
import com.chuanqing.mapper.KnowPostMapper;
import com.chuanqing.mapper.RelationMapper;
import com.chuanqing.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class KnowPostPermissionServiceTest {

    @Mock
    private KnowPostMapper knowPostMapper;

    @Mock
    private RelationMapper relationMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private KnowPostPermissionService service;

    @Test
    void missingPostReturnsNotFound() {
        when(knowPostMapper.findDetailById(10L)).thenReturn(null);

        assertError(() -> service.requireReadable(10L, null), ErrorCodeEnums.KNOW_POST_NOT_FOUND);
    }

    @Test
    void ownerCanReadOwnDraftRegardlessOfVisibility() {
        KnowPostDetailEntity post = post("draft", "private");
        when(knowPostMapper.findDetailById(10L)).thenReturn(post);

        assertThat(service.requireReadable(10L, 1L)).isSameAs(post);
        verify(relationMapper, never()).existsFollowing(1L, 1L);
    }

    @Test
    void publicAndUnlistedPublishedPostsAllowDirectAnonymousRead() {
        KnowPostDetailEntity publicPost = post("published", "public");
        when(knowPostMapper.findDetailById(10L)).thenReturn(publicPost);
        assertThat(service.requireReadable(10L, null)).isSameAs(publicPost);

        KnowPostDetailEntity unlistedPost = post("published", "unlisted");
        when(knowPostMapper.findDetailById(11L)).thenReturn(unlistedPost);
        assertThat(service.requireReadable(11L, null)).isSameAs(unlistedPost);
    }

    @Test
    void followerVisibilityRequiresActiveFollowRelation() {
        KnowPostDetailEntity post = post("published", "followers");
        when(knowPostMapper.findDetailById(10L)).thenReturn(post);
        when(relationMapper.existsFollowing(2L, 1L)).thenReturn(1);

        assertThat(service.requireReadable(10L, 2L)).isSameAs(post);

        when(relationMapper.existsFollowing(3L, 1L)).thenReturn(0);
        assertError(() -> service.requireReadable(10L, 3L), ErrorCodeEnums.KNOW_POST_FORBIDDEN);
    }

    @Test
    void schoolVisibilityRequiresSameNonBlankSchool() {
        KnowPostDetailEntity post = post("published", "school");
        post.setAuthorSchool("同济大学");
        UserEntity viewer = new UserEntity();
        viewer.setSchool(" 同济大学 ");
        when(knowPostMapper.findDetailById(10L)).thenReturn(post);
        when(userMapper.findById(2L)).thenReturn(viewer);

        assertThat(service.requireReadable(10L, 2L)).isSameAs(post);

        viewer.setSchool("复旦大学");
        assertError(() -> service.requireReadable(10L, 2L), ErrorCodeEnums.KNOW_POST_FORBIDDEN);
    }

    @Test
    void privatePublishedPostRejectsNonOwner() {
        when(knowPostMapper.findDetailById(10L)).thenReturn(post("published", "private"));

        assertError(() -> service.requireReadable(10L, 2L), ErrorCodeEnums.KNOW_POST_FORBIDDEN);
    }

    @Test
    void onlyPublishedPublicPostIsDiscoverable() {
        assertThat(service.isDiscoverable(post("published", "public"))).isTrue();
        assertThat(service.isDiscoverable(post("published", "unlisted"))).isFalse();
        assertThat(service.isDiscoverable(post("draft", "public"))).isFalse();
    }

    @Test
    void batchDiscoveryCheckFiltersStaleSearchDocumentsWithOneQuery() {
        com.chuanqing.entity.KnowPostEntity publicPost = com.chuanqing.entity.KnowPostEntity.builder()
                .id(10L).status("published").visible("public").build();
        com.chuanqing.entity.KnowPostEntity privatePost = com.chuanqing.entity.KnowPostEntity.builder()
                .id(11L).status("published").visible("private").build();
        com.chuanqing.entity.KnowPostEntity draft = com.chuanqing.entity.KnowPostEntity.builder()
                .id(12L).status("draft").visible("public").build();
        when(knowPostMapper.listAccessByIds(List.of(10L, 11L, 12L)))
                .thenReturn(List.of(publicPost, privatePost, draft));

        Set<Long> result = service.retainDiscoverableIds(List.of(10L, 11L, 12L, 10L));

        assertThat(result).containsExactly(10L);
        verify(knowPostMapper).listAccessByIds(List.of(10L, 11L, 12L));
    }

    private KnowPostDetailEntity post(String status, String visibility) {
        KnowPostDetailEntity post = new KnowPostDetailEntity();
        post.setId(10L);
        post.setCreatorId(1L);
        post.setStatus(status);
        post.setVisible(visibility);
        return post;
    }

    private void assertError(Runnable action, ErrorCodeEnums expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
