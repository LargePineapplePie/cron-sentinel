package com.cronsentinel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cronsentinel.common.CheckStatus;
import com.cronsentinel.dto.CheckCreateRequest;
import com.cronsentinel.dto.CheckUpdateRequest;
import com.cronsentinel.entity.CheckItem;
import com.cronsentinel.mapper.CheckItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 检查项 CRUD 与状态管理服务。
 */
@Slf4j
@Service
public class CheckService {

    private final CheckItemMapper checkItemMapper;

    public CheckService(CheckItemMapper checkItemMapper) {
        this.checkItemMapper = checkItemMapper;
    }

    /**
     * 创建检查项，自动生成 token。
     */
    public CheckItem create(CheckCreateRequest req) {
        CheckItem item = new CheckItem();
        item.setName(req.getName());
        item.setToken(UUID.randomUUID().toString().replace("-", ""));
        item.setPeriodSeconds(req.getPeriodSeconds());
        item.setGraceSeconds(req.getGraceSeconds() == null ? 3600 : req.getGraceSeconds());
        item.setStatus(CheckStatus.NEW);
        item.setAlertEmail(req.getAlertEmail());
        LocalDateTime now = LocalDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        checkItemMapper.insert(item);
        log.info("创建检查项 id={}, name={}, token={}", item.getId(), item.getName(), item.getToken());
        return item;
    }

    public List<CheckItem> list() {
        QueryWrapper<CheckItem> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("id");
        return checkItemMapper.selectList(wrapper);
    }

    public CheckItem getById(Long id) {
        return checkItemMapper.selectById(id);
    }

    public CheckItem getByToken(String token) {
        QueryWrapper<CheckItem> wrapper = new QueryWrapper<>();
        wrapper.eq("token", token);
        return checkItemMapper.selectOne(wrapper);
    }

    /**
     * 更新检查项基本信息（非 null 字段才更新）。
     */
    public CheckItem update(Long id, CheckUpdateRequest req) {
        CheckItem item = checkItemMapper.selectById(id);
        if (item == null) {
            return null;
        }
        if (req.getName() != null) {
            item.setName(req.getName());
        }
        if (req.getPeriodSeconds() != null) {
            item.setPeriodSeconds(req.getPeriodSeconds());
        }
        if (req.getGraceSeconds() != null) {
            item.setGraceSeconds(req.getGraceSeconds());
        }
        if (req.getAlertEmail() != null) {
            item.setAlertEmail(req.getAlertEmail());
        }
        item.setUpdatedAt(LocalDateTime.now());
        checkItemMapper.updateById(item);
        return item;
    }

    public boolean delete(Long id) {
        return checkItemMapper.deleteById(id) > 0;
    }

    /**
     * 暂停：置为 PAUSED，不再参与超时扫描。
     */
    public CheckItem pause(Long id) {
        CheckItem item = checkItemMapper.selectById(id);
        if (item == null) {
            return null;
        }
        item.setStatus(CheckStatus.PAUSED);
        item.setUpdatedAt(LocalDateTime.now());
        checkItemMapper.updateById(item);
        return item;
    }

    /**
     * 恢复：从 PAUSED 恢复。
     * 若已收到过心跳则回到 UP（并重算 next_expected_at），否则回到 NEW。
     */
    public CheckItem resume(Long id) {
        CheckItem item = checkItemMapper.selectById(id);
        if (item == null) {
            return null;
        }
        if (item.getLastPingAt() != null) {
            item.setStatus(CheckStatus.UP);
            item.setNextExpectedAt(item.getLastPingAt()
                    .plusSeconds(item.getPeriodSeconds())
                    .plusSeconds(item.getGraceSeconds()));
        } else {
            item.setStatus(CheckStatus.NEW);
        }
        item.setUpdatedAt(LocalDateTime.now());
        checkItemMapper.updateById(item);
        return item;
    }
}
