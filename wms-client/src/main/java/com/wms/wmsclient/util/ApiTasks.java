package com.wms.wmsclient.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * 后台调用的 Task 样板封装（CONTRIBUTING §3.4 JavaFX 线程规则）：
 * call 在 daemon 后台线程执行；onSuccess 收到返回值、onFailed 收到异常，
 * 两个回调由 Task 语义保证在 FX 线程执行（无需再包 Platform.runLater）。
 */
public final class ApiTasks {

    private ApiTasks() {
    }

    public static <T> void run(String threadName, Callable<T> call,
                               Consumer<T> onSuccess, Consumer<Throwable> onFailed) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return call.call();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onFailed.accept(task.getException()));
        Thread t = new Thread(task, threadName);
        t.setDaemon(true); // 关窗即退，不因在途请求僵住进程
        t.start();
    }
}
