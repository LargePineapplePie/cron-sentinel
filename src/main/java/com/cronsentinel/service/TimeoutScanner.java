package com.cronsentinel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cronsentinel.common.CheckStatus;
import com.cronsentinel.entity.CheckItem;
import com.cronsentinel.mapper.CheckItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时扫描器：每 1 分钟扫描一次，将超时的检查项置为 DOWN 并告警。
 */
@Slf4j
@Component
public class TimeoutScanner {

    private final CheckItemMapper checkItemMapper;
    private final AlertService alertService;

    public TimeoutScanner(CheckItemMapper checkItemMapper, AlertService alertService) {
        this.checkItemMapper = checkItemMapper;
        this.alertService = alertService;
    }

    /**
     * 每分钟执行：
     * 1. 查出 status 为 UP 或 NEW 的检查项（PAUSED / DOWN 不参与）
     * 2. 计算该检查项的"最晚预期时间"：
     *    - UP   状态用 next_expected_at
     *    - NEW  状态用 created_at + period + grace
     * 3. 若 now 已超过最晚预期时间，置 DOWN 并发送告警（状态变化才发，避免重复轰炸）
     */
    @Scheduled(fixedRate = 60_000)
    public void scan() {
        LocalDateTime now = LocalDateTime.now();

        QueryWrapper<CheckItem> wrapper = new QueryWrapper<>();
        wrapper.in("status", CheckStatus.UP, CheckStatus.NEW);
        List<CheckItem> candidates = checkItemMapper.selectList(wrapper);

        if (candidates.isEmpty()) {
            return;
        }

        int downCount = 0;
        for (CheckItem item : candidates) {
            LocalDateTime deadline = resolveDeadline(item);
            if (deadline == null) {
                continue;
            }
            if (now.isAfter(deadline)) {
                item.setStatus(CheckStatus.DOWN);
                item.setUpdatedAt(now);
                checkItemMapper.updateById(item);
                downCount++;
                log.warn("检查项 id={}, name={} 超时(deadline={})，置为 DOWN",
                        item.getId(), item.getName(), deadline);
                // 状态从非 DOWN 变为 DOWN，发送告警
                alertService.sendDownAlert(item);
            }
        }

        if (downCount > 0) {
            log.info("超时扫描完成：候选 {} 个，新增 DOWN {} 个", candidates.size(), downCount);
        }
    }

    /**
     * 计算该检查项的最晚预期心跳时间。
     */
    private LocalDateTime resolveDeadline(CheckItem item) {
        if (CheckStatus.UP.equals(item.getStatus())) {
            return item.getNextExpectedAt();
        }
        if (CheckStatus.NEW.equals(item.getStatus())) {
            if (item.getCreatedAt() == null) {
                return null;
            }
            return item.getCreatedAt()
                    .plusSeconds(item.getPeriodSeconds())
                    .plusSeconds(item.getGraceSeconds());
        }
        return null;
    }
}
