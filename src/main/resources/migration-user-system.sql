-- ============================================================
-- Cron Sentinel - 用户体系手动迁移脚本
-- 适用于已经存在 cron_sentinel 库和旧表的环境。
-- 新环境直接执行 schema.sql 即可，不需要执行本文件。
-- ============================================================

USE cron_sentinel;

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

-- 先允许为空，方便把历史检查项迁移到某个已注册用户名下。
ALTER TABLE check_item
    ADD COLUMN owner_user_id BIGINT NULL COMMENT '所属用户 user_account.id' AFTER id;

ALTER TABLE ping_log
    ADD COLUMN owner_user_id BIGINT NULL COMMENT '所属用户 user_account.id' AFTER id;

-- 1. 启动应用并注册第一个用户。
-- 2. 查到该用户 id，例如：
--    SELECT id, email FROM user_account;
-- 3. 将历史数据归属给该用户，把下面的 1 替换成真实 user_account.id 后执行：
--    UPDATE check_item SET owner_user_id = 1 WHERE owner_user_id IS NULL;
--    UPDATE ping_log pl
--    JOIN check_item ci ON pl.check_id = ci.id
--    SET pl.owner_user_id = ci.owner_user_id
--    WHERE pl.owner_user_id IS NULL;
-- 4. 确认没有空值后，再收紧约束：
--    ALTER TABLE check_item MODIFY owner_user_id BIGINT NOT NULL;
--    ALTER TABLE ping_log MODIFY owner_user_id BIGINT NOT NULL;

ALTER TABLE check_item
    ADD KEY idx_owner_user_id (owner_user_id),
    ADD KEY idx_owner_status (owner_user_id, status);

ALTER TABLE ping_log
    ADD KEY idx_owner_user_id (owner_user_id),
    ADD KEY idx_owner_check_created (owner_user_id, check_id, created_at);
