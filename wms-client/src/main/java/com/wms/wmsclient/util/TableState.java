package com.wms.wmsclient.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

/**
 * TableView 公共辅助：占位符三态 + 列构建。
 * 占位三态（加载中/空数据/加载失败）替代默认英文「No content in table」，
 * 避免把「尚未加载完」误报成「没有数据」。占位符仅在表格无行时渲染，有数据时调用无副作用。
 * 文字用 .subtle 样式类（浅/深两套 CSS 均有定义），主题兼容零成本。
 */
public final class TableState {

    private TableState() {
    }

    /** 建一列并加入表格（PropertyValueFactory 反射读行 Bean 的 getXxx()） */
    public static <S, V> void addCol(TableView<S> tv, String title, String prop, double w) {
        TableColumn<S, V> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        tv.getColumns().add(c);
    }

    /** 发起加载前调用 */
    public static void loading(TableView<?> tv) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(16, 16);
        Label label = new Label("加载中…");
        label.getStyleClass().add("subtle");
        HBox box = new HBox(6, spinner, label);
        box.setAlignment(Pos.CENTER);
        tv.setPlaceholder(box);
    }

    /** 加载成功后调用；hint 为 null 时显示「暂无数据」（如「没有匹配“张”的记录」） */
    public static void empty(TableView<?> tv, String hint) {
        Label label = new Label(hint == null ? "暂无数据" : hint);
        label.getStyleClass().add("subtle");
        tv.setPlaceholder(label);
    }

    /** 加载失败后调用 */
    public static void failed(TableView<?> tv, String reason) {
        Label label = new Label("加载失败：" + reason);
        label.getStyleClass().add("subtle");
        tv.setPlaceholder(label);
    }
}
