package com.wms.updater;

import java.nio.file.Path;

/**
 * 升级参数（主程序 ProcessBuilder 传入，形如 --url=http://... --md5=...）：
 * url=升级包地址，md5=期望指纹，target=待替换主 jar 路径，pid=主程序进程号，launch=重启命令路径。
 */
public final class Params {

    private final String url;
    private final String md5;
    private final Path target;
    private final long pid;
    private final Path launch;

    private Params(String url, String md5, Path target, long pid, Path launch) {
        this.url = url;
        this.md5 = md5;
        this.target = target;
        this.pid = pid;
        this.launch = launch;
    }

    /** 五参数齐备且 pid 可解析才有效，否则抛 IllegalArgumentException（弹错退出） */
    static Params parse(String[] args) {
        String url = null, md5 = null, target = null, launch = null;
        long pid = -1;
        for (String arg : args) {
            if (arg.startsWith("--url=")) url = arg.substring(6);
            else if (arg.startsWith("--md5=")) md5 = arg.substring(6);
            else if (arg.startsWith("--target=")) target = arg.substring(9);
            else if (arg.startsWith("--pid=")) {
                try {
                    pid = Long.parseLong(arg.substring(6));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("pid 非数字：" + arg);
                }
            }
            else if (arg.startsWith("--launch=")) launch = arg.substring(9);
            else throw new IllegalArgumentException("未知参数：" + arg);
        }
        if (url == null || md5 == null || target == null || pid <= 0 || launch == null) {
            throw new IllegalArgumentException("缺少必要参数（需 --url/--md5/--target/--pid/--launch）");
        }
        return new Params(url, md5, Path.of(target), pid, Path.of(launch));
    }

    public String url() {
        return url;
    }

    public String md5() {
        return md5;
    }

    public Path target() {
        return target;
    }

    public long pid() {
        return pid;
    }

    public Path launch() {
        return launch;
    }
}
