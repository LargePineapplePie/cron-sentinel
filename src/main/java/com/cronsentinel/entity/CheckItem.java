package com.cronsentinel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检查项实体，对应表 check_item。
 */
@Data
@TableName("check_item")
public class CheckItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 user_account.id */
    private Long ownerUserId;

    /** 检查项名称，如"每日数据备份" */
    private String name;

    /** 唯一标识，用于 ping URL，UUID 生成 */
    private String token;

    /** 预期周期(秒)，如每天=86400 */
    private Integer periodSeconds;

    /** 宽限期(秒)，超时容忍，默认 3600 */
    private Integer graceSeconds;

    /** 状态：NEW(新建未收到过) / UP(正常) / DOWN(故障) / PAUSED(暂停) */
    private String status;

    /** 最后一次收到心跳的时间 */
    private LocalDateTime lastPingAt;

    /** 下次预期收到心跳的最晚时间(=last_ping+period+grace) */
    private LocalDateTime nextExpectedAt;

    /** 告警通知邮箱 */
    private String alertEmail;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
