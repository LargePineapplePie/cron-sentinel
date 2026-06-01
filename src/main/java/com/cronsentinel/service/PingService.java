package com.cronsentinel.service;

import com.cronsentinel.common.CheckStatus;
import com.cronsentinel.common.PingType;
import com.cronsentinel.entity.CheckItem;
import com.cronsentinel.entity.PingLog;
import com.cronsentinel.mapper.CheckItemMapper;
import com.cronsentinel.mapper.PingLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 心跳处理服务。
 */
@Slf4j
@Service
public class PingService {

    private final CheckItemMapper checkItemMapper;
    private final PingLogMapper pingLogMapper;
    private final AlertService alertService;

    public PingService(CheckItemMapper checkItemMapper,
                       PingLogMapper pingLogMapper,
                       AlertService alertService) {
        this.checkItemMapper = checkItemMapper;
        this.pingLogMapper = pingLogMapper;
        this.alertService = alertService;
    }

    /**
     * 任务成功心跳：置 UP，刷新 last_ping_at / next_expected_at。
     * 若此前为 DOWN，则发送恢复通知。
     */
    public boolean success(String token, String sourceIp) {
        CheckItem item = findByToken(token);
        if (item == null) {
            return false;
        }
        boolean wasDown = CheckStatus.DOWN.equals(item.getStatus());

        LocalDateTime now = LocalDateTime.now();
        item.setLastPingAt(now);
        item.setNextExpectedAt(now.plusSeconds(item.getPeriodSeconds())
                .plusSeconds(item.getGraceSeconds()));
        item.setStatus(CheckStatus.UP);
        item.setUpdatedAt(now);
        checkItemMapper.updateById(item);

        writeLog(item.getId(), PingType.SUCCESS, sourceIp);

        // DOWN -> UP 时发送恢复通知
        if (wasDown) {
            log.info("检查项 id={} 由 DOWN 恢复为 UP，发送恢复通知", item.getId());
            alertService.sendRecoveryNotice(item);
        }
        return true;
    }

    /**
     * 任务开始心跳：仅记录 START 日志，不改状态。
     */
    public boolean start(String token, String sourceIp) {
        CheckItem item = findByToken(token);
        if (item == null) {
            return false;
        }
        writeLog(item.getId(), PingType.START, sourceIp);
        return true;
    }

    /**
     * 任务失败心跳：置 DOWN，立即触发告警（仅在状态变化时发送）。
     */
    public boolean fail(String token, String sourceIp) {
        CheckItem item = findByToken(token);
        if (item == null) {
            return false;
        }
        boolean wasDown = CheckStatus.DOWN.equals(item.getStatus());

        item.setStatus(CheckStatus.DOWN);
        item.setUpdatedAt(LocalDateTime.now());
        checkItemMapper.updateById(item);

        writeLog(item.getId(), PingType.FAIL, sourceIp);

        // 仅在非 DOWN -> DOWN 时告警，避免重复轰炸
        if (!wasDown) {
            log.info("检查项 id={} 主动上报失败，置为 DOWN 并告警", item.getId());
            alertService.sendDownAlert(item);
        }
        return true;
    }

    private CheckItem findByToken(String token) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CheckItem> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("token", token);
        return checkItemMapper.selectOne(wrapper);
    }

    private void writeLog(Long checkId, String type, String sourceIp) {
        try {
            PingLog plog = new PingLog();
            plog.setCheckId(checkId);
            plog.setType(type);
            plog.setSourceIp(sourceIp);
            plog.setCreatedAt(LocalDateTime.now());
            pingLogMapper.insert(plog);
        } catch (Exception e) {
            // 写日志失败不影响心跳主流程
            log.warn("写 ping_log 失败 checkId={}, type={}, 原因={}", checkId, type, e.getMessage());
        }
    }
}
