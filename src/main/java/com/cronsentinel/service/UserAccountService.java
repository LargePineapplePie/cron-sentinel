package com.cronsentinel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cronsentinel.entity.UserAccount;
import com.cronsentinel.mapper.UserAccountMapper;
import com.cronsentinel.security.CurrentUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 用户注册与登录加载服务。
 */
@Service
public class UserAccountService implements UserDetailsService {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount register(String email, String password, String displayName) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        if (findByEmail(normalizedEmail) != null) {
            throw new IllegalArgumentException("邮箱已注册");
        }

        LocalDateTime now = LocalDateTime.now();
        UserAccount user = new UserAccount();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(resolveDisplayName(displayName, normalizedEmail));
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userAccountMapper.insert(user);
        return user;
    }

    public UserAccount findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return null;
        }
        QueryWrapper<UserAccount> wrapper = new QueryWrapper<>();
        wrapper.eq("email", normalizedEmail);
        return userAccountMapper.selectOne(wrapper);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return new CurrentUser(user);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveDisplayName(String displayName, String email) {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName.trim();
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
