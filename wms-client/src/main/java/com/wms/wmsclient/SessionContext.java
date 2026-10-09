package com.wms.wmsclient;

import com.wms.common.dto.LoginResponse;

import java.util.Collections;
import java.util.List;

/**
 * 客户端登录态：静态单例，登录成功后存一份，整个应用生命周期可用。
 * 退出登录时 reset() 清空。
 */
public final class SessionContext {

    private static volatile LoginResponse current;

    private SessionContext() {
    }

    public static void set(LoginResponse response) {
        current = response;
    }

    public static LoginResponse get() {
        return current;
    }

    public static String getUsername() {
        return current == null ? null : current.getUsername();
    }

    public static String getRealName() {
        return current == null ? null : current.getRealName();
    }

    public static List<String> getPermissions() {
        return current == null ? Collections.emptyList() : current.getPermissions();
    }

    /** 是否含指定权限码 */
    public static boolean hasPermission(String permCode) {
        return getPermissions().contains(permCode);
    }

    /** 清空登录态（退出登录时调） */
    public static void reset() {
        current = null;
    }
}
