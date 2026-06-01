package com.cronsentinel.controller;

import com.cronsentinel.service.PingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 心跳接收接口。无需登录鉴权，要求快速响应。
 * 支持 GET 与 POST。
 */
@RestController
public class PingController {

    private final PingService pingService;

    public PingController(PingService pingService) {
        this.pingService = pingService;
    }

    /** 任务成功心跳 */
    @RequestMapping(value = "/ping/{token}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> ping(@PathVariable String token, HttpServletRequest request) {
        boolean found = pingService.success(token, clientIp(request));
        return found ? ResponseEntity.ok("OK")
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND");
    }

    /** 任务开始心跳（可选，仅记录日志） */
    @RequestMapping(value = "/ping/{token}/start", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> start(@PathVariable String token, HttpServletRequest request) {
        boolean found = pingService.start(token, clientIp(request));
        return found ? ResponseEntity.ok("OK")
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND");
    }

    /** 任务失败心跳，立即告警 */
    @RequestMapping(value = "/ping/{token}/fail", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> fail(@PathVariable String token, HttpServletRequest request) {
        boolean found = pingService.fail(token, clientIp(request));
        return found ? ResponseEntity.ok("OK")
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND");
    }

    /**
     * 获取客户端 IP，优先取反向代理头。
     */
    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // X-Forwarded-For 可能是逗号分隔的链路，取第一个
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
