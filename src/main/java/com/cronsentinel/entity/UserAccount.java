package com.cronsentinel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户账号实体，对应表 user_account。
 */
@Data
@TableName("user_account")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录邮箱 */
    private String email;

    /** BCrypt 密码哈希 */
    private String passwordHash;

    /** 显示名称 */
    private String displayName;

    /** 状态：ACTIVE / DISABLED */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
