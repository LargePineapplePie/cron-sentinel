package com.cronsentinel.service;

import com.cronsentinel.entity.CheckItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 告警邮件服务。
 * 所有发送均 try-catch，失败仅记录日志，绝不影响主流程。
 */
@Slf4j
@Service
public class AlertService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;

    @Value("${cron-sentinel.mail.from:}")
    private String from;

    @Value("${cron-sentinel.mail.enabled:true}")
    private boolean mailEnabled;

    public AlertService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送故障告警。
     */
    public void sendDownAlert(CheckItem item) {
        String subject = "[Cron Sentinel] 故障告警: " + item.getName();
        String body = buildBody(item,
                "您的检查项已被判定为【故障 DOWN】，可能任务未按时执行或已崩溃。");
        send(item.getAlertEmail(), subject, body);
    }

    /**
     * 发送恢复通知。
     */
    public void sendRecoveryNotice(CheckItem item) {
        String subject = "[Cron Sentinel] 恢复通知: " + item.getName();
        String body = buildBody(item,
                "您的检查项已重新收到心跳，状态恢复为【正常 UP】。");
        send(item.getAlertEmail(), subject, body);
    }

    private String buildBody(CheckItem item, String headline) {
        StringBuilder sb = new StringBuilder();
        sb.append(headline).append("\n\n");
        sb.append("检查项名称: ").append(item.getName()).append("\n");
        sb.append("当前状态:   ").append(item.getStatus()).append("\n");
        sb.append("最后心跳:   ")
                .append(item.getLastPingAt() == null ? "(从未收到)" : item.getLastPingAt().format(FMT))
                .append("\n");
        sb.append("下次预期:   ")
                .append(item.getNextExpectedAt() == null ? "-" : item.getNextExpectedAt().format(FMT))
                .append("\n");
        sb.append("当前时间:   ").append(LocalDateTime.now().format(FMT)).append("\n");
        return sb.toString();
    }

    private void send(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("邮件发送已禁用(cron-sentinel.mail.enabled=false)，跳过。主题: {}", subject);
            return;
        }
        if (to == null || to.isBlank()) {
            log.warn("检查项未配置 alertEmail，跳过邮件发送。主题: {}", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                message.setFrom(from);
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("告警邮件已发送至 {}，主题: {}", to, subject);
        } catch (Exception e) {
            // 邮件失败不能影响主流程
            log.error("告警邮件发送失败，主题: {}，原因: {}", subject, e.getMessage(), e);
        }
    }
}
