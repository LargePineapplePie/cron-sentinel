package com.cronsentinel.common;

/**
 * 检查项状态常量。
 */
public final class CheckStatus {

    private CheckStatus() {
    }

    /** 新建，尚未收到过任何心跳 */
    public static final String NEW = "NEW";

    /** 正常 */
    public static final String UP = "UP";

    /** 故障 */
    public static final String DOWN = "DOWN";

    /** 暂停（不参与超时扫描） */
    public static final String PAUSED = "PAUSED";
}
