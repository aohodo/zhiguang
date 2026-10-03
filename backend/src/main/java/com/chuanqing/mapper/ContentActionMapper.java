package com.chuanqing.mapper;

import com.chuanqing.entity.ContentActionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContentActionMapper {
    int activateExisting(@Param("userId") Long userId,
                         @Param("entityType") String entityType,
                         @Param("entityId") Long entityId,
                         @Param("actionType") String actionType);

    int insertIfAbsent(@Param("id") Long id,
                       @Param("userId") Long userId,
                       @Param("entityType") String entityType,
                       @Param("entityId") Long entityId,
                       @Param("actionType") String actionType);

    int deactivate(@Param("userId") Long userId,
                   @Param("entityType") String entityType,
                   @Param("entityId") Long entityId,
                   @Param("actionType") String actionType);

    ContentActionEntity findByKey(@Param("userId") Long userId,
                                  @Param("entityType") String entityType,
                                  @Param("entityId") Long entityId,
                                  @Param("actionType") String actionType);

    int existsActive(@Param("userId") Long userId,
                     @Param("entityType") String entityType,
                     @Param("entityId") Long entityId,
                     @Param("actionType") String actionType);

    long countActive(@Param("entityType") String entityType,
                     @Param("entityId") Long entityId,
                     @Param("actionType") String actionType);

    List<ContentActionEntity> listActiveByUser(@Param("userId") Long userId,
                                               @Param("actionType") String actionType,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);
}
