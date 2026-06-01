package com.cronsentinel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 更新检查项请求。所有字段可选，非 null 才更新。
 */
@Data
public class CheckUpdateRequest {

    private String name;

    @Min(value = 1, message = "periodSeconds 必须大于 0")
    private Integer periodSeconds;

    @Min(value = 0, message = "graceSeconds 不能为负")
    private Integer graceSeconds;

    @Email(message = "alertEmail 格式不正确")
    private String alertEmail;
}
