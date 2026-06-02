package com.cronsentinel.controller;

import com.cronsentinel.dto.ApiResult;
import com.cronsentinel.security.CurrentUser;
import com.cronsentinel.service.UserAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录、注册与当前用户接口。
 */
@Controller
public class AuthController {

    private final UserAccountService userAccountService;

    public AuthController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal CurrentUser user) {
        return user == null ? "login" : "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage(@AuthenticationPrincipal CurrentUser user) {
        return user == null ? "register" : "redirect:/";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(required = false) String displayName,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            userAccountService.register(email, password, displayName);
            redirectAttributes.addFlashAttribute("message", "注册成功，请登录");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("displayName", displayName);
            return "register";
        }
    }

    @GetMapping("/api/auth/me")
    @ResponseBody
    public ApiResult<Map<String, Object>> me(@AuthenticationPrincipal CurrentUser user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", user.getId());
        body.put("email", user.getEmail());
        body.put("displayName", user.getDisplayName());
        return ApiResult.ok(body);
    }
}
