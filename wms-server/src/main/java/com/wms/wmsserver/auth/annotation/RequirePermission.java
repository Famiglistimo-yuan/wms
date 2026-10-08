package com.wms.wmsserver.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级权限声明：标注在 Controller 方法上，值为所需权限码（如 "menu.material.add"）。
 * JwtInterceptor 会校验当前登录用户的权限集是否包含该码；不包含返回 403。
 * 不标注的方法默认只校验 token 有效性（已登录）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequirePermission {
    String value();
}
