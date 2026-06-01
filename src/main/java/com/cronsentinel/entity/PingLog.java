package com.cronsentinel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心跳记录实体，对应表 ping_log。
 */
@Data
@TableName("ping_log")
public class PingLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 check_item.id */
    private Long checkId;

    /** 类型：SUCCESS / START / FAIL */
    private String type;

    /** 来源 IP */
    private String sourceIp;

    private LocalDateTime createdAt;
}
