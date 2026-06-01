package com.cronsentinel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建检查项请求。
 */
@Data
public class CheckCreateRequest {

    @NotBlank(message = "name 不能为空")
    private String name;

    /** 预期周期(秒)，如每天=86400 */
    @NotNull(message = "periodSeconds 不能为空")
    @Min(value = 1, message = "periodSeconds 必须大于 0")
    private Integer periodSeconds;

    /** 宽限期(秒)，不传则默认 3600 */
    @Min(value = 0, message = "graceSeconds 不能为负")
    private Integer graceSeconds;

    @Email(message = "alertEmail 格式不正确")
    private String alertEmail;
}
