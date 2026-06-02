-- ============================================================
-- Cron Sentinel - 数据库建表脚本 (MySQL 8.x)
-- 用法：CREATE DATABASE cron_sentinel; USE cron_sentinel; 然后执行本脚本
-- ============================================================

CREATE DATABASE IF NOT EXISTS cron_sentinel
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE cron_sentinel;

-- ------------------------------------------------------------
-- 表 0：user_account 用户账号
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_account (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    email         VARCHAR(200) NOT NULL                COMMENT '登录邮箱',
    password_hash VARCHAR(200) NOT NULL                COMMENT 'BCrypt 密码哈希',
    display_name  VARCHAR(100)          DEFAULT NULL   COMMENT '显示名称',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
    created_at    DATETIME     NOT NULL                COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账号';

-- ------------------------------------------------------------
-- 表 1：check_item 检查项
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS check_item (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    owner_user_id    BIGINT       NOT NULL                COMMENT '所属用户 user_account.id',
    name             VARCHAR(100) NOT NULL                COMMENT '检查项名称，如"每日数据备份"',
    token            VARCHAR(64)  NOT NULL                COMMENT '唯一标识，用于 ping URL，UUID 生成',
    period_seconds   INT          NOT NULL                COMMENT '预期周期(秒)，如每天=86400',
    grace_seconds    INT          NOT NULL DEFAULT 3600   COMMENT '宽限期(秒)，超时容忍，默认 3600',
    status           VARCHAR(20)  NOT NULL DEFAULT 'NEW'  COMMENT '状态：NEW/UP/DOWN/PAUSED',
    last_ping_at     DATETIME              DEFAULT NULL   COMMENT '最后一次收到心跳的时间',
    next_expected_at DATETIME              DEFAULT NULL   COMMENT '下次预期收到心跳的最晚时间',
    alert_email      VARCHAR(200)          DEFAULT NULL   COMMENT '告警通知邮箱',
    created_at       DATETIME     NOT NULL                COMMENT '创建时间',
    updated_at       DATETIME     NOT NULL                COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_token (token),
    KEY idx_owner_user_id (owner_user_id),
    KEY idx_owner_status (owner_user_id, status),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查项';

-- ------------------------------------------------------------
-- 表 2：ping_log 心跳记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ping_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    owner_user_id BIGINT   NOT NULL                COMMENT '所属用户 user_account.id',
    check_id   BIGINT      NOT NULL                COMMENT '关联 check_item.id',
    type       VARCHAR(20) NOT NULL                COMMENT '类型：SUCCESS / START / FAIL',
    source_ip  VARCHAR(64)          DEFAULT NULL   COMMENT '来源 IP',
    created_at DATETIME    NOT NULL                COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_owner_user_id (owner_user_id),
    KEY idx_owner_check_created (owner_user_id, check_id, created_at),
    KEY idx_check_id (check_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心跳记录';
