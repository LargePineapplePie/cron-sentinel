package com.cronsentinel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cronsentinel.entity.PingLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 心跳记录 Mapper，继承 MyBatis-Plus BaseMapper。
 */
@Mapper
public interface PingLogMapper extends BaseMapper<PingLog> {
}
