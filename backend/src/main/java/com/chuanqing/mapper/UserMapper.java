package com.chuanqing.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.chuanqing.entity.UserEntity;
import java.util.List;

@Mapper
public interface UserMapper {

    UserEntity findByPhone(@Param("phone") String phone);

    UserEntity findByEmail(@Param("email") String email);

    boolean existsByPhone(@Param("phone") String phone);

    boolean existsByEmail(@Param("email") String email);

    void insert(UserEntity user);

    UserEntity findById(@Param("id") Long id);

    void updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    void updateProfile(UserEntity user);

    boolean existsByZgIdExceptId(@Param("zgId") String zgId, @Param("excludeId") Long excludeId);

    List<UserEntity> listByIds(@Param("ids") List<Long> ids);
}
