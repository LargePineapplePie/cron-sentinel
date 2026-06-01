package com.cronsentinel.dto;

import com.cronsentinel.entity.CheckItem;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检查项响应，包含拼好的 ping URL。
 */
@Data
public class CheckResponse {

    private Long id;
    private String name;
    private String token;
    private String pingUrl;
    private Integer periodSeconds;
    private Integer graceSeconds;
    private String status;
    private LocalDateTime lastPingAt;
    private LocalDateTime nextExpectedAt;
    private String alertEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CheckResponse from(CheckItem item, String baseUrl) {
        CheckResponse r = new CheckResponse();
        r.setId(item.getId());
        r.setName(item.getName());
        r.setToken(item.getToken());
        r.setPingUrl(baseUrl + "/ping/" + item.getToken());
        r.setPeriodSeconds(item.getPeriodSeconds());
        r.setGraceSeconds(item.getGraceSeconds());
        r.setStatus(item.getStatus());
        r.setLastPingAt(item.getLastPingAt());
        r.setNextExpectedAt(item.getNextExpectedAt());
        r.setAlertEmail(item.getAlertEmail());
        r.setCreatedAt(item.getCreatedAt());
        r.setUpdatedAt(item.getUpdatedAt());
        return r;
    }
}
