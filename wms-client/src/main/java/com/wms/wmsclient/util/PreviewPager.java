package com.wms.wmsclient.util;

import io.vproxy.vfx.ui.button.FusionButton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * 报表预览分页器：两个导出面板共用。持有页码/总数状态与按钮态，
 * 内置互斥、代次防回填（条件变更或重新加载后在途响应作废）与三态占位切换。
 * 面板提供取数 Callable 与「行列表/总数」取值函数，及附加回填（如账本摘要、空数据专属提示）。
 */
public final class PreviewPager<T> {

    private final TableView<T> table;
    private final FusionButton btnQuery;
    private final FusionButton btnPrev;
    private final FusionButton btnNext;
    private final Label lblPage;
    private final int pageSize;
    private final String totalLabel; // 「条明细」/「日流水」等总数单位文案
    private final Runnable loader;

    private final ObservableList<T> rows = FXCollections.observableArrayList();
    private int page = 1;
    private long total;
    private int seq;
    private boolean loading;

    public <R> PreviewPager(TableView<T> table, FusionButton btnQuery, FusionButton btnPrev, FusionButton btnNext,
                            Label lblPage, int pageSize, String totalLabel, String threadName,
                            Callable<R> fetch, Function<R, List<T>> rowsOf,
                            ToLongFunction<R> totalOf, Consumer<R> extras) {
        this.table = table;
        this.btnQuery = btnQuery;
        this.btnPrev = btnPrev;
        this.btnNext = btnNext;
        this.lblPage = lblPage;
        this.pageSize = pageSize;
        this.totalLabel = totalLabel;
        // 加载逻辑放泛型方法里，构造器的 R 在此捕获（实例字段无法持有构造器类型参数）
        this.loader = () -> run(threadName, fetch, rowsOf, totalOf, extras);
        table.setItems(rows);
    }

    /** 「预览」按钮：回第 1 页加载 */
    public void first() {
        page = 1;
        load();
    }

    /** 上一页按钮 */
    public void prev() {
        if (!loading && page > 1) {
            page--;
            load();
        }
    }

    /** 下一页按钮 */
    public void next() {
        if (!loading && page < pages()) {
            page++;
            load();
        }
    }

    /** 当前页码（面板拼取数路径的 query 用） */
    public int page() {
        return page;
    }

    /** 条件变更：清空旧数据并复位（在途响应由代次作废） */
    public void reset() {
        seq++;
        rows.clear();
        page = 1;
        total = 0;
        lblPage.setText("");
        btnPrev.setDisable(true);
        btnNext.setDisable(true);
        TableState.empty(table, "条件已变更，点击「预览」查看");
    }

    private void load() {
        if (loading) return;
        loader.run();
    }

    private long pages() {
        return Math.max(1, (total + pageSize - 1) / pageSize);
    }

    private <R> void run(String threadName, Callable<R> fetch, Function<R, List<T>> rowsOf,
                         ToLongFunction<R> totalOf, Consumer<R> extras) {
        loading = true;
        btnQuery.setDisable(true);
        btnPrev.setDisable(true);
        btnNext.setDisable(true);
        TableState.loading(table);
        int mySeq = ++seq;
        ApiTasks.run(threadName, fetch, r -> {
            loading = false;
            btnQuery.setDisable(false);
            if (mySeq != seq) return; // 期间条件已变更或已重新加载，本页作废
            rows.setAll(rowsOf.apply(r));
            total = totalOf.applyAsLong(r);
            lblPage.setText("第 " + page + "/" + pages() + " 页 · 共 " + total + " " + totalLabel);
            btnPrev.setDisable(page <= 1);
            btnNext.setDisable(page >= pages());
            TableState.empty(table, null);
            extras.accept(r); // 附加回填：可再以专属提示覆盖空占位（如「2026年9月无进出仓单据」）
        }, ex -> {
            loading = false;
            btnQuery.setDisable(false);
            if (mySeq != seq) return;
            TableState.failed(table, ExportHelper.rootMessage(ex));
            lblPage.setText("预览失败");
        });
    }
}
