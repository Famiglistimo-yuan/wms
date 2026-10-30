package com.wms.common;

/**
 * 全局共享常量（客户端与服务端复用）。
 */
public final class WmsConstants {

    private WmsConstants() { }

    /** 内置系统管理员账号：不可修改/停用/删除，权限恒为全部允许 */
    public static final String SYSTEM_ADMIN_USERNAME = "admin";
}
