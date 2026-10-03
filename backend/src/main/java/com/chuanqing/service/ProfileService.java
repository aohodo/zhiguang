package com.chuanqing.service;

import com.chuanqing.dto.ProfilePatchDTO;
import com.chuanqing.vo.ProfileVO;
import com.chuanqing.entity.UserEntity;

import java.util.Optional;

/**
 * 个人资料业务接口。
 */
public interface ProfileService {

    Optional<UserEntity> getById(long userId);

    ProfileVO updateProfile(long userId, ProfilePatchDTO req);

    ProfileVO updateAvatar(long userId, String avatarUrl);
}