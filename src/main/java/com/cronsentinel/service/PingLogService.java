package com.cronsentinel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cronsentinel.dto.PingLogView;
import com.cronsentinel.entity.CheckItem;
import com.cronsentinel.entity.PingLog;
import com.cronsentinel.mapper.CheckItemMapper;
import com.cronsentinel.mapper.PingLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 心跳记录查询服务（供状态页/日志页使用）。
 */
@Service
public class PingLogService {

    private final PingLogMapper pingLogMapper;
    private final CheckItemMapper checkItemMapper;

    public PingLogService(PingLogMapper pingLogMapper, CheckItemMapper checkItemMapper) {
        this.pingLogMapper = pingLogMapper;
        this.checkItemMapper = checkItemMapper;
    }

    /**
     * 分页查询心跳记录，支持按检查项、类型与时间范围过滤。
     *
     * @param checkId  仅查某个检查项；为 null 时查全部
     * @param type     仅查某种类型(SUCCESS/START/FAIL)；为 null/空 时查全部
     * @param start    起始时间（含）；为 null 不限
     * @param end      结束时间（含）；为 null 不限
     * @param pageNo   页码，从 1 开始
     * @param pageSize 每页条数
     */
    public Page<PingLogView> page(Long checkId, String type, LocalDateTime start, LocalDateTime end,
                                  long pageNo, long pageSize) {
        QueryWrapper<PingLog> wrapper = new QueryWrapper<>();
        if (checkId != null) {
            wrapper.eq("check_id", checkId);
        }
        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq("type", type.trim());
        }
        if (start != null) {
            wrapper.ge("created_at", start);
        }
        if (end != null) {
            wrapper.le("created_at", end);
        }
        wrapper.orderByDesc("id");

        Page<PingLog> result = pingLogMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);

        // id -> name 映射
        Map<Long, String> nameMap = checkItemMapper.selectList(null).stream()
                .collect(Collectors.toMap(CheckItem::getId, CheckItem::getName, (a, b) -> a));

        List<PingLogView> views = result.getRecords().stream().map(log -> {
            PingLogView v = new PingLogView();
            v.setId(log.getId());
            v.setCheckId(log.getCheckId());
            v.setCheckName(nameMap.getOrDefault(log.getCheckId(), "(已删除 #" + log.getCheckId() + ")"));
            v.setType(log.getType());
            v.setSourceIp(log.getSourceIp());
            v.setCreatedAt(log.getCreatedAt());
            return v;
        }).collect(Collectors.toList());

        // 用同样的分页元信息构造视图分页对象
        Page<PingLogView> viewPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        viewPage.setRecords(views);
        return viewPage;
    }
}
