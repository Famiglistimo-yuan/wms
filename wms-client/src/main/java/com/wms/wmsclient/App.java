package com.wms.wmsclient;

import com.wms.common.dto.LoginResponse;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.scene.HomeScene;
import com.wms.wmsclient.scene.MenuScene;
import com.wms.wmsclient.scene.PanelScene;
import com.wms.wmsclient.update.UpdateService;
import com.wms.wmsclient.util.TokenStore;
import com.wms.wmsclient.theme.ThemeManager;
import io.vproxy.vfx.control.globalscreen.GlobalScreenUtils;
import io.vproxy.vfx.manager.task.TaskManager;
import io.vproxy.vfx.ui.button.FusionButton;
import io.vproxy.vfx.ui.scene.VScene;
import io.vproxy.vfx.ui.scene.VSceneGroup;
import io.vproxy.vfx.ui.scene.VSceneHideMethod;
import io.vproxy.vfx.ui.scene.VSceneShowMethod;
import io.vproxy.vfx.ui.stage.VStage;
import io.vproxy.vfx.ui.stage.VStageInitParams;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 客户端入口（ADR-010 vfx 骨架）：VStage 单窗口 + VSceneGroup 场景切换 + 抽屉菜单。
 * 登录/主页/子程序面板均为 MAIN 场景；菜单点击仍经统一入口 showPanel 切换（ADR-007 约定保留）。
 */
public class App extends Application {

    private VStage vStage;
    /** MAIN 场景组：登录/主页/子程序面板在其中带动画切换 */
    private VSceneGroup sceneGroup;
    private MenuScene menuScene;
    private HomeScene homeScene;
    private VScene loginScene;
    private FusionButton menuBtn;
    /** 面板场景登记表（每次 showPanel 重建场景，保证数据新鲜，与原 setCenter 重载 FXML 行为一致） */
    private final Map<String, VScene> panelScenes = new HashMap<>();
    /** 同面板重进时退役的旧场景（vfx 禁止移除正在显示的 MAIN 场景，推迟到下次切换时清理） */
    private final List<VScene> retiredScenes = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) throws IOException {
        // 主题注入（ADR-010）：按落盘偏好选浅/深，必须早于任何 vfx 组件创建（vfx Theme 仅可注入一次）
        ThemeManager.init();
        homeScene = new HomeScene();
        menuScene = new MenuScene(this);
        vStage = new VStage(primaryStage, new VStageInitParams().setInitialScene(homeScene)) {
            @Override
            public void close() {
                TaskManager.get().terminate();
                GlobalScreenUtils.unregister();
                super.close();
            }
        };
        sceneGroup = vStage.getSceneGroup();

        // 抽屉菜单挂 ROOT 场景组（左侧滑出）；「菜单」按钮放窗口左上角标题栏旁，登录场景下隐藏
        vStage.getRootSceneGroup().addScene(menuScene, VSceneHideMethod.TO_LEFT);
        menuBtn = new FusionButton("菜单");
        menuBtn.setPrefSize(64, VStage.TITLE_BAR_HEIGHT + 1);
        menuBtn.setLayoutX(-2);
        menuBtn.setLayoutY(-1);
        menuBtn.setOnAction(e -> vStage.getRootSceneGroup().show(menuScene, VSceneShowMethod.FROM_LEFT));
        vStage.getRoot().getContentPane().getChildren().add(menuBtn);

        // 窗口图标（Windows/Linux 任务栏；macOS 的 Dock 图标由 jpackage 的 .icns 提供）
        var iconUrl = getClass().getResource("/icons/wms.png");
        if (iconUrl != null) {
            vStage.getStage().getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        // 401 统一回调：任何业务接口返回 401 自动清态回登录场景（兜底，原逻辑不变）
        ApiClient.setOnUnauthorized(code -> Platform.runLater(() -> {
            // 登录接口口令错误也返回 401：未登录态的 401 属于登录失败，由 LoginController 弹窗提示
            if (SessionContext.get() == null) {
                return;
            }
            clearSession();
            try {
                showLogin();
            } catch (IOException e) {
                System.err.println("会话过期后回登录窗失败：" + e.getMessage());
            }
        }));

        // token 落盘恢复（FR-4/FR-6）：有存档则免重登直接进主页
        if (!restoreSession()) {
            showLogin();
        }

        vStage.setTitle("仓库管理系统");
        vStage.getStage().setWidth(1000);
        vStage.getStage().setHeight(680);
        vStage.getStage().centerOnScreen();
        vStage.show();
        // FR-6：后台检查新版本（失败静默，不阻塞登录；确有新版弹窗征求同意）
        UpdateService.checkAsync(false);
    }

    /** 主窗口引用；「更多」面板等浮层用它做 initOwner 与定位基准 */
    public Stage getStage() {
        return vStage.getStage();
    }

    /** 清空登录态（内存会话 + ApiClient token + 落盘存档） */
    public void clearSession() {
        SessionContext.reset();
        ApiClient.clearAuth();
        TokenStore.clear();
    }

    /**
     * 恢复落盘登录态。有存档时先进主页（体验优先），再后台异步调 /api/auth/me：
     *   - 成功 → 用数据库最新权限集刷新 SessionContext + TokenStore，重跑菜单显隐
     *   - 401 → ApiClient 回调已自动清态回登录场景
     *   - 网络异常 → 静默（下次业务请求自然 401 兜底）
     */
    private boolean restoreSession() {
        LoginResponse saved = TokenStore.load();
        if (saved == null || saved.getToken() == null || saved.getToken().isEmpty()) {
            return false;
        }
        SessionContext.set(saved);
        ApiClient.auth(saved.getToken());
        showMain();
        startVerifyTask();
        return true;
    }

    /** 后台异步校验 token 有效性 + 刷新权限集（成功则覆盖会话并重跑菜单显隐；失败静默，401 走兜底回调） */
    private void startVerifyTask() {
        Task<LoginResponse> verifyTask = new Task<>() {
            @Override
            protected LoginResponse call() throws Exception {
                String json = ApiClient.get("/api/auth/me");
                return ApiClient.getResultData(json, LoginResponse.class);
            }
        };
        verifyTask.setOnSucceeded(e -> Platform.runLater(() -> {
            LoginResponse fresh = verifyTask.getValue();
            // 用数据库最新权限集覆盖落盘陈旧值（管理员收回权限后自动同步）
            SessionContext.set(fresh);
            TokenStore.save(fresh);
            // 菜单显隐按最新权限集重跑
            if (menuScene != null) {
                menuScene.refreshUser();
                menuScene.applyMenuPermissions();
            }
        }));
        verifyTask.setOnFailed(e -> {
            // 401：ApiClient.setOnUnauthorized 已清态回登录场景；
            // 其他异常（网络抖动等）：静默，下次业务请求自然 401 兜底
        });
        // daemon：关窗即退，不因后台校验未完成而僵住进程
        Thread t = new Thread(verifyTask, "restore-session-verify");
        t.setDaemon(true);
        t.start();
    }

    /** 登录场景 */
    public void showLogin() throws IOException {
        purgeRetiredScenes();
        if (loginScene == null) {
            loginScene = new PanelScene(this, "rg2402_11_12_13_login");
            sceneGroup.addScene(loginScene);
        }
        menuBtn.setVisible(false);
        sceneGroup.show(loginScene, VSceneShowMethod.FROM_LEFT);
    }

    /** 主界面（主页 + 抽屉菜单），登录成功 / 会话恢复后进入 */
    public void showMain() {
        homeScene.refreshUser();
        menuScene.refreshUser();
        menuScene.applyMenuPermissions();
        menuBtn.setVisible(true);
        sceneGroup.show(homeScene, VSceneShowMethod.FROM_RIGHT);
    }

    /**
     * 切换子程序面板（ADR-007：全应用单窗口导航入口；ADR-010：机制由 BorderPane setCenter
     * 改为 VScene 带动画切换）。
     *
     * @param fxmlName resources/fxml/ 下文件名（含前缀、不含扩展名），
     *                 如 "rg2402_11_12_13_person-manage"
     */
    public void showPanel(String fxmlName) throws IOException {
        purgeRetiredScenes();
        VScene old = panelScenes.remove(fxmlName);
        if (old != null) {
            if (old == sceneGroup.getCurrentMainScene()) {
                retiredScenes.add(old);
            } else {
                sceneGroup.removeScene(old);
            }
        }
        VScene scene = new PanelScene(this, fxmlName);
        panelScenes.put(fxmlName, scene);
        sceneGroup.addScene(scene);
        sceneGroup.show(scene, VSceneShowMethod.FROM_RIGHT);
    }

    /** 清理退役面板场景；仍是 currentMainScene 的（切换动画未完成）留到下次再清 */
    private void purgeRetiredScenes() {
        for (Iterator<VScene> it = retiredScenes.iterator(); it.hasNext(); ) {
            VScene retired = it.next();
            if (retired != sceneGroup.getCurrentMainScene()) {
                sceneGroup.removeScene(retired);
                it.remove();
            }
        }
    }

    /** 收起抽屉菜单 */
    public void hideMenu() {
        vStage.getRootSceneGroup().hide(menuScene, VSceneHideMethod.TO_LEFT);
    }
}
