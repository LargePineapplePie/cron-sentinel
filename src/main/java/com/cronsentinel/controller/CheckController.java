package com.cronsentinel.controller;

import com.cronsentinel.dto.ApiResult;
import com.cronsentinel.dto.CheckCreateRequest;
import com.cronsentinel.dto.CheckResponse;
import com.cronsentinel.dto.CheckUpdateRequest;
import com.cronsentinel.entity.CheckItem;
import com.cronsentinel.service.CheckService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 检查项管理 REST 接口。
 */
@RestController
@RequestMapping("/api/checks")
public class CheckController {

    private final CheckService checkService;

    public CheckController(CheckService checkService) {
        this.checkService = checkService;
    }

    @PostMapping
    public ResponseEntity<ApiResult<CheckResponse>> create(@Valid @RequestBody CheckCreateRequest req,
                                                           HttpServletRequest request) {
        CheckItem item = checkService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(CheckResponse.from(item, baseUrl(request))));
    }

    @GetMapping
    public ApiResult<List<CheckResponse>> list(HttpServletRequest request) {
        String base = baseUrl(request);
        List<CheckResponse> list = checkService.list().stream()
                .map(i -> CheckResponse.from(i, base))
                .collect(Collectors.toList());
        return ApiResult.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<CheckResponse>> detail(@PathVariable Long id, HttpServletRequest request) {
        CheckItem item = checkService.getById(id);
        if (item == null) {
            return notFound();
        }
        return ResponseEntity.ok(ApiResult.ok(CheckResponse.from(item, baseUrl(request))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<CheckResponse>> update(@PathVariable Long id,
                                                          @Valid @RequestBody CheckUpdateRequest req,
                                                          HttpServletRequest request) {
        CheckItem item = checkService.update(id, req);
        if (item == null) {
            return notFound();
        }
        return ResponseEntity.ok(ApiResult.ok(CheckResponse.from(item, baseUrl(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(@PathVariable Long id) {
        boolean ok = checkService.delete(id);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResult.fail(404, "检查项不存在"));
        }
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResult<CheckResponse>> pause(@PathVariable Long id, HttpServletRequest request) {
        CheckItem item = checkService.pause(id);
        if (item == null) {
            return notFound();
        }
        return ResponseEntity.ok(ApiResult.ok(CheckResponse.from(item, baseUrl(request))));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResult<CheckResponse>> resume(@PathVariable Long id, HttpServletRequest request) {
        CheckItem item = checkService.resume(id);
        if (item == null) {
            return notFound();
        }
        return ResponseEntity.ok(ApiResult.ok(CheckResponse.from(item, baseUrl(request))));
    }

    private <T> ResponseEntity<ApiResult<T>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResult.fail(404, "检查项不存在"));
    }

    /**
     * 从请求中推导服务基础 URL，用于拼接 ping URL。
     */
    private String baseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);
        boolean defaultPort = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        if (!defaultPort) {
            sb.append(":").append(port);
        }
        return sb.toString();
    }
}
