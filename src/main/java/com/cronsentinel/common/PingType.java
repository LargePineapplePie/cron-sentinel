package com.cronsentinel.common;

/**
 * 心跳类型常量。
 */
public final class PingType {

    private PingType() {
    }

    /** 任务成功完成 */
    public static final String SUCCESS = "SUCCESS";

    /** 任务开始 */
    public static final String START = "START";

    /** 任务失败 */
    public static final String FAIL = "FAIL";
}
