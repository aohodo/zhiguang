package com.chuanqing.service;

import com.chuanqing.common.enums.ErrorCodeEnums;
import com.chuanqing.common.enums.KnowPostStatusEnums;
import com.chuanqing.common.enums.KnowPostVisibilityEnums;
import com.chuanqing.entity.KnowPostDetailEntity;
import com.chuanqing.entity.KnowPostEntity;
import com.chuanqing.entity.UserEntity;
import com.chuanqing.exception.BusinessException;
import com.chuanqing.mapper.KnowPostMapper;
import com.chuanqing.mapper.RelationMapper;
import com.chuanqing.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KnowPostPermissionService {

    private final KnowPostMapper knowPostMapper;
    private final RelationMapper relationMapper;
    private final UserMapper userMapper;

    public KnowPostDetailEntity requireReadable(long postId, Long viewerId) {
        KnowPostDetailEntity post = knowPostMapper.findDetailById(postId);
        if (post == null || KnowPostStatusEnums.DELETED.getValue().equalsIgnoreCase(post.getStatus())) {
            throw new BusinessException(ErrorCodeEnums.KNOW_POST_NOT_FOUND);
        }

        if (isOwner(post.getCreatorId(), viewerId)) {
            return post;
        }

        if (!KnowPostStatusEnums.PUBLISHED.getValue().equalsIgnoreCase(post.getStatus())
                || !canReadPublished(post, viewerId)) {
            throw new BusinessException(ErrorCodeEnums.KNOW_POST_FORBIDDEN);
        }
        return post;
    }

    public void requireOwner(long postId, long userId) {
        KnowPostEntity post = knowPostMapper.findById(postId);
        if (post == null || KnowPostStatusEnums.DELETED.getValue().equalsIgnoreCase(post.getStatus())) {
            throw new BusinessException(ErrorCodeEnums.KNOW_POST_NOT_FOUND);
        }
        if (!isOwner(post.getCreatorId(), userId)) {
            throw new BusinessException(ErrorCodeEnums.KNOW_POST_FORBIDDEN);
        }
    }

    public boolean isDiscoverable(KnowPostDetailEntity post) {
        return post != null
                && KnowPostStatusEnums.PUBLISHED.getValue().equalsIgnoreCase(post.getStatus())
                && KnowPostVisibilityEnums.PUBLIC.getValue().equalsIgnoreCase(post.getVisible());
    }

    public Set<Long> retainDiscoverableIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<KnowPostEntity> posts = knowPostMapper.listAccessByIds(postIds.stream().distinct().toList());
        Set<Long> discoverable = new HashSet<>();
        for (KnowPostEntity post : posts) {
            if (post != null
                    && KnowPostStatusEnums.PUBLISHED.getValue().equalsIgnoreCase(post.getStatus())
                    && KnowPostVisibilityEnums.PUBLIC.getValue().equalsIgnoreCase(post.getVisible())) {
                discoverable.add(post.getId());
            }
        }
        return discoverable;
    }

    private boolean canReadPublished(KnowPostDetailEntity post, Long viewerId) {
        KnowPostVisibilityEnums visibility;
        try {
            visibility = KnowPostVisibilityEnums.fromValue(post.getVisible());
        } catch (IllegalArgumentException exception) {
            return false;
        }

        return switch (visibility) {
            case PUBLIC, UNLISTED -> true;
            case PRIVATE -> false;
            case FOLLOWERS -> viewerId != null
                    && relationMapper.existsFollowing(viewerId, post.getCreatorId()) > 0;
            case SCHOOL -> viewerId != null && hasSameSchool(post, viewerId);
        };
    }

    private boolean hasSameSchool(KnowPostDetailEntity post, long viewerId) {
        if (post.getAuthorSchool() == null || post.getAuthorSchool().isBlank()) {
            return false;
        }
        UserEntity viewer = userMapper.findById(viewerId);
        return viewer != null
                && viewer.getSchool() != null
                && !viewer.getSchool().isBlank()
                && post.getAuthorSchool().trim().equalsIgnoreCase(viewer.getSchool().trim());
    }

    private boolean isOwner(Long creatorId, Long viewerId) {
        return creatorId != null && creatorId.equals(viewerId);
    }
}
