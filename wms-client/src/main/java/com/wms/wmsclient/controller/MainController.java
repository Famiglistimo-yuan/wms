package com.wms.wmsclient.controller;

import com.wms.wmsclient.App;
import com.wms.wmsclient.SessionContext;
import com.wms.wmsclient.http.ApiClient;
import com.wms.wmsclient.update.UpdateService;
import com.wms.wmsclient.util.TokenStore;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

/**
 * 主界面骨架：按 SessionContext 里的权限码显隐菜单项（FR-4-4）。
 * 每个 MenuItem 对应一个权限码（data.sql 里 13 项），无权则 setVisible(false) + setDisable(true) 双保险。
 */
public class MainController {

    private final App app;

    @FXML
    private Label lblUser;

    // 顶层 Menu（整组菜单项权限不足则隐藏整个菜单）
    @FXML
    private Menu menuPerson;
    @FXML
    private Menu menuMaterial;
    @FXML
    private Menu menuStock;
    @FXML
    private Menu menuAuth;

    // ====== 人员档案菜单项 ======
    @FXML private MenuItem miPersonView;
    @FXML private MenuItem miPersonAdd;
    @FXML private MenuItem miPersonEdit;
    @FXML private MenuItem miPersonDelete;

    // ====== 物料档案菜单项 ======
    @FXML private MenuItem miMaterialView;
    @FXML private MenuItem miMaterialAdd;
    @FXML private MenuItem miMaterialEdit;
    @FXML private MenuItem miMaterialDelete;

    // ====== 进出仓菜单项 ======
    @FXML private MenuItem miStockIn;
    @FXML private MenuItem miStockOut;
    @FXML private MenuItem miStockQuery;

    // ====== 系统管理菜单项 ======
    @FXML private MenuItem miAuthUser;
    @FXML private MenuItem miAuthGrant;

    public MainController(App app) {
        this.app = app;
    }

    @FXML
    protected void initialize() {
        // 显示当前用户
        String realName = SessionContext.getRealName();
        String username = SessionContext.getUsername();
        lblUser.setText("当前用户：" + (realName != null ? realName : username));

        // 按权限显隐菜单项
        applyMenuPermissions();
    }

    /** 遍历所有菜单项，根据权限码显隐 + 禁用 */
    private void applyMenuPermissions() {
        // 人员档案
        applyPerm(miPersonView, "menu.person.view");
        applyPerm(miPersonAdd, "menu.person.add");
        applyPerm(miPersonEdit, "menu.person.edit");
        applyPerm(miPersonDelete, "menu.person.delete");
        // 顶层菜单：整组没有可见项则隐藏
        hideMenuIfEmpty(menuPerson);

        // 物料档案
        applyPerm(miMaterialView, "menu.material.view");
        applyPerm(miMaterialAdd, "menu.material.add");
        applyPerm(miMaterialEdit, "menu.material.edit");
        applyPerm(miMaterialDelete, "menu.material.delete");
        hideMenuIfEmpty(menuMaterial);

        // 进出仓
        applyPerm(miStockIn, "menu.stock.in");
        applyPerm(miStockOut, "menu.stock.out");
        applyPerm(miStockQuery, "menu.stock.query");
        hideMenuIfEmpty(menuStock);

        // 系统管理
        applyPerm(miAuthUser, "menu.auth.user");
        applyPerm(miAuthGrant, "menu.auth.grant");
        // 退出登录所有人都可见，不 applyPerm
        hideMenuIfEmpty(menuAuth);
    }

    /** 单项：无权则 hide + disable 双保险 */
    private void applyPerm(MenuItem item, String permCode) {
        boolean has = SessionContext.hasPermission(permCode);
        item.setVisible(has);
        item.setDisable(!has);
    }

    /** 整组：没有任何可见项则隐藏整个 Menu */
    private void hideMenuIfEmpty(Menu menu) {
        boolean anyVisible = menu.getItems().stream().anyMatch(MenuItem::isVisible);
        menu.setVisible(anyVisible);
    }

    // ====== 以下是 FXML 里 MenuItem 绑定的 onAction 处理（命名按开发规范 §5.4：on + 动作 + Clicked）======

    @FXML
    protected void onPersonViewClicked() {
        showPlaceholder("人员档案查询");
    }

    @FXML
    protected void onPersonAddClicked() {
        showPlaceholder("人员档案增加");
    }

    @FXML
    protected void onPersonEditClicked() {
        showPlaceholder("人员档案修改");
    }

    @FXML
    protected void onPersonDeleteClicked() {
        showPlaceholder("人员档案删除");
    }

    @FXML
    protected void onMaterialViewClicked() {
        showPlaceholder("物料档案查询");
    }

    @FXML
    protected void onMaterialAddClicked() {
        showPlaceholder("物料档案增加");
    }

    @FXML
    protected void onMaterialEditClicked() {
        showPlaceholder("物料档案修改");
    }

    @FXML
    protected void onMaterialDeleteClicked() {
        showPlaceholder("物料档案删除");
    }

    @FXML
    protected void onStockInClicked() {
        showPlaceholder("进仓录入");
    }

    @FXML
    protected void onStockOutClicked() {
        showPlaceholder("出仓录入");
    }

    @FXML
    protected void onStockQueryClicked() {
        showPlaceholder("进出仓单查询");
    }

    @FXML
    protected void onAuthUserClicked() {
        showPanelSafe("rg2402_11_12_13_user-manage");
    }

    @FXML
    protected void onAuthGrantClicked() {
        showPanelSafe("rg2402_11_12_13_role-grant");
    }

    @FXML
    protected void onPlaceholderClicked() {
        // 旧的占位入口，保留兼容
        new Alert(Alert.AlertType.INFORMATION, "该功能将在对应子系统开发中开放").showAndWait();
    }

    @FXML
    protected void onLogoutClicked() {
        // 清登录态（内存 + 落盘存档）
        SessionContext.reset();
        ApiClient.clearAuth();
        TokenStore.clear();
        try {
            app.showLogin();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "返回登录失败：" + e.getMessage()).showAndWait();
        }
    }

    @FXML
    protected void onCheckUpdateClicked() {
        UpdateService.checkAsync(true);
    }

    private void showPlaceholder(String featureName) {
        new Alert(Alert.AlertType.INFORMATION, featureName + " 将在对应子系统开发中开放").showAndWait();
    }

    /** 安全切换面板，异常弹窗 */
    private void showPanelSafe(String fxmlName) {
        try {
            app.showPanel(fxmlName);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "无法打开 " + fxmlName + "：" + e.getMessage()).showAndWait();
        }
    }
}
