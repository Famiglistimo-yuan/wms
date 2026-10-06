package com.wms.common;

import lombok.Data;

/**
 * 版本信息（FR-6 自动升级契约，两端共用）：服务端 /api/version 下发，客户端据此比对与下载。
 */
@Data
public class VersionInfo {

    /** 最新版本号，三段数字（如 1.0.1） */
    private String version;

    /** 升级包下载地址（相对路径，如 /download/wms-client.jar，客户端拼接 BASE_URL） */
    private String downloadUrl;

    /** 升级包 MD5（服务端发布时计算填入，客户端下载后校验） */
    private String md5;
}
