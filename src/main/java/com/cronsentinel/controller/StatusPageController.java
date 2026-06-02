package com.cronsentinel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cronsentinel.dto.CheckCreateRequest;
import com.cronsentinel.dto.CheckUpdateRequest;
import com.cronsentinel.dto.PingLogView;
import com.cronsentinel.entity.CheckItem;
import com.cronsentinel.service.CheckService;
import com.cronsentinel.service.PingLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 状态页与网页表单操作（Thymeleaf）。
 * 这里的 /web/** 接口面向浏览器表单提交，处理后重定向回页面。
 */
@Controller
public class StatusPageController {

    private final CheckService checkService;
    private final PingLogService pingLogService;

    public StatusPageController(CheckService checkService, PingLogService pingLogService) {
        this.checkService = checkService;
        this.pingLogService = pingLogService;
    }

    /** 首页：检查项列表 + 新建表单 */
    @GetMapping("/")
    public String index(Model model) {
        List<CheckItem> checks = checkService.list();
        model.addAttribute("checks", checks);
        return "index";
    }

    /** 心跳记录页：支持按检查项、时间范围筛选 + 分页 */
    @GetMapping("/logs")
    public String logs(@RequestParam(required = false) Long checkId,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       @RequestParam(defaultValue = "1") long page,
                       @RequestParam(defaultValue = "20") long size,
                       Model model) {
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);
        if (size <= 0) {
            size = 20;
        }

        Page<PingLogView> result = pingLogService.page(checkId, type, start, end, Math.max(1, page), size);

        model.addAttribute("logs", result.getRecords());
        model.addAttribute("page", result.getCurrent());
        model.addAttribute("size", result.getSize());
        model.addAttribute("total", result.getTotal());
        model.addAttribute("pages", result.getPages());
        model.addAttribute("checks", checkService.list());
        model.addAttribute("selectedCheckId", checkId);
        model.addAttribute("selectedType", type);
        model.addAttribute("startTime", startTime);
        model.addAttribute("endTime", endTime);
        return "logs";
    }

    /**
     * 解析 datetime-local 输入（格式 yyyy-MM-ddTHH:mm）；空或非法返回 null。
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** 网页表单：新建检查项 */
    @PostMapping("/web/checks")
    public String createFromForm(@RequestParam String name,
                                 @RequestParam Integer periodSeconds,
                                 @RequestParam(required = false) Integer graceSeconds,
                                 @RequestParam(required = false) String alertEmail) {
        CheckCreateRequest req = new CheckCreateRequest();
        req.setName(name);
        req.setPeriodSeconds(periodSeconds);
        req.setGraceSeconds(graceSeconds);
        req.setAlertEmail(alertEmail);
        checkService.create(req);
        return "redirect:/";
    }

    /** 编辑页：展示预填表单 */
    @GetMapping("/web/checks/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        CheckItem item = checkService.getById(id);
        if (item == null) {
            return "redirect:/";
        }
        model.addAttribute("check", item);
        return "edit";
    }

    /** 网页表单：保存编辑 */
    @PostMapping("/web/checks/{id}/update")
    public String updateFromForm(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam Integer periodSeconds,
                                 @RequestParam(required = false) Integer graceSeconds,
                                 @RequestParam(required = false) String alertEmail) {
        CheckUpdateRequest req = new CheckUpdateRequest();
        req.setName(name);
        req.setPeriodSeconds(periodSeconds);
        req.setGraceSeconds(graceSeconds);
        req.setAlertEmail(alertEmail);
        checkService.update(id, req);
        return "redirect:/";
    }

    /** 网页表单：暂停 */
    @PostMapping("/web/checks/{id}/pause")
    public String pause(@PathVariable Long id) {
        checkService.pause(id);
        return "redirect:/";
    }

    /** 网页表单：恢复 */
    @PostMapping("/web/checks/{id}/resume")
    public String resume(@PathVariable Long id) {
        checkService.resume(id);
        return "redirect:/";
    }

    /** 网页表单：删除 */
    @PostMapping("/web/checks/{id}/delete")
    public String delete(@PathVariable Long id) {
        checkService.delete(id);
        return "redirect:/";
    }
}
