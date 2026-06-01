package com.cronsentinel.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心跳记录的展示对象，额外带上所属检查项名称，方便页面显示。
 */
@Data
public class PingLogView {

    private Long id;
    private Long checkId;
    private String checkName;
    private String type;
    private String sourceIp;
    private LocalDateTime createdAt;
}
