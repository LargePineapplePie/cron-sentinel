package com.cronsentinel.sdk.core;

/**
 * 心跳类型，对应服务端三个端点的 URL 后缀。
 */
public enum PingType {

    /** 成功：GET /ping/{token} */
    SUCCESS(""),

    /** 开始：GET /ping/{token}/start */
    START("/start"),

    /** 失败：GET /ping/{token}/fail */
    FAIL("/fail");

    private final String pathSuffix;

    PingType(String pathSuffix) {
        this.pathSuffix = pathSuffix;
    }

    public String getPathSuffix() {
        return pathSuffix;
    }
}
