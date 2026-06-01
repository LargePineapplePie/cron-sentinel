package com.cronsentinel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cronsentinel.dto.PingLogView;
import com.cronsentinel.entity.CheckItem;
import com.cronsentinel.entity.PingLog;
import com.cronsentinel.mapper.CheckItemMapper;
import com.cronsentinel.mapper.PingLogMapper;
import org.springframework.stereotype.Service;

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
     * 查询最近的心跳记录。
     *
     * @param checkId 仅查某个检查项；为 null 时查全部
     * @param limit   最多返回条数
     */
    public List<PingLogView> recent(Long checkId, int limit) {
        QueryWrapper<PingLog> wrapper = new QueryWrapper<>();
        if (checkId != null) {
            wrapper.eq("check_id", checkId);
        }
        wrapper.orderByDesc("id").last("limit " + Math.max(1, limit));
        List<PingLog> logs = pingLogMapper.selectList(wrapper);

        // 取出涉及的检查项名称，做 id -> name 映射
        Map<Long, String> nameMap = checkItemMapper.selectList(null).stream()
                .collect(Collectors.toMap(CheckItem::getId, CheckItem::getName, (a, b) -> a));

        return logs.stream().map(log -> {
            PingLogView v = new PingLogView();
            v.setId(log.getId());
            v.setCheckId(log.getCheckId());
            v.setCheckName(nameMap.getOrDefault(log.getCheckId(), "(已删除 #" + log.getCheckId() + ")"));
            v.setType(log.getType());
            v.setSourceIp(log.getSourceIp());
            v.setCreatedAt(log.getCreatedAt());
            return v;
        }).collect(Collectors.toList());
    }
}
