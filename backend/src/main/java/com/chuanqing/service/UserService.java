package com.chuanqing.service;

import com.chuanqing.entity.UserEntity;
import java.util.Optional;

/**
 * 用户服务接口。
 */
public interface UserService {

    Optional<UserEntity> findByPhone(String phone);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findById(long id);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    UserEntity createUser(UserEntity user);

    void updatePassword(UserEntity user);
}