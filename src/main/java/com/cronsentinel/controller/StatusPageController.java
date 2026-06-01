package com.cronsentinel.controller;

import com.cronsentinel.dto.CheckCreateRequest;
import com.cronsentinel.entity.CheckItem;
import com.cronsentinel.service.CheckService;
import com.cronsentinel.service.PingLogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    /** 心跳记录页 */
    @GetMapping("/logs")
    public String logs(@RequestParam(required = false) Long checkId, Model model) {
        model.addAttribute("logs", pingLogService.recent(checkId, 200));
        model.addAttribute("checks", checkService.list());
        model.addAttribute("selectedCheckId", checkId);
        return "logs";
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
