package com.cronsentinel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cronsentinel.entity.CheckItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检查项 Mapper，继承 MyBatis-Plus BaseMapper。
 */
@Mapper
public interface CheckItemMapper extends BaseMapper<CheckItem> {
}
