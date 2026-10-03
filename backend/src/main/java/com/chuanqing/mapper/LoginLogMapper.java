package com.chuanqing.mapper;

import com.chuanqing.entity.LoginLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper {

    void insert(LoginLogEntity log);
}
