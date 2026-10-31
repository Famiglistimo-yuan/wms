package com.wms.wmsserver.materialio.controller;

import com.wms.common.Result;
import com.wms.wmsserver.materialio.dto.CreateOrderDTO;
import com.wms.wmsserver.materialio.dto.IoRecordQuery;
import com.wms.wmsserver.materialio.entity.Material;
import com.wms.wmsserver.materialio.entity.StockOrder;
import com.wms.wmsserver.materialio.service.MaterialIoRecordService;
import com.wms.wmsserver.materialio.service.MaterialService;
import com.wms.wmsserver.materialio.service.StockOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 子系统2 —— 进出仓单据化 REST 接口。
 * <p>
 * 接口分类：
 * <pre>
 * 【物料档案】
 *   GET    /api/material/list          全量物料列表（含库位/安全库存）
 *   POST   /api/material/add           新增物料
 *   POST   /api/material/update        修改物料档案
 *
 * 【入/出库单 —— 核心】
 *   POST   /api/material/order/create   创建单据（入库/出库 共用，orderType=1入库/2出库）
 *   GET    /api/material/order/list    单据列表（orderType 可选筛选）
 *   GET    /api/material/order/{id}    单据详情（主表 + 明细，含物料档案）
 *
 * 【库存查询】
 *   GET    /api/material/stock          实时库存（含库位/安全库存/预警标记 safeStockLow）
 *
 * 【流水台账】
 *   GET    /api/material/io-records          全部流水（旧，兼容）
 *   POST   /api/material/io-records/page     分页 + 条件筛选（物料/时间/单据类型）
 * </pre>
 */
@RestController
@RequestMapping("/api/material")
@RequiredArgsConstructor
public class MaterialIoController {

    private final MaterialService materialService;
    private final MaterialIoRecordService recordService;
    private final StockOrderService stockOrderService;

    // ==================== 物料档案 ====================

    /** 全量物料列表（含新增的 location / safeStockNum 字段） */
    @GetMapping("/list")
    public Result<List<Material>> listAll() {
        return Result.success(materialService.listAll());
    }

    @PostMapping("/add")
    public Result<String> addMaterial(@RequestBody Material material) {
        materialService.addMaterial(material);
        return Result.success("新增物料成功");
    }

    @PostMapping("/update")
    public Result<String> updateMaterial(@RequestBody Material material) {
        materialService.updateMaterial(material);
        return Result.success("修改物料成功");
    }

    // ==================== 单据（核心） ====================

    /**
     * 创建单据（入库 / 出库 共用一个端点，orderType 区分）。
     * <pre>
     * body:
     * {
     *   "orderType": 1,        // 1入库 2出库
     *   "source": "采购入库",    // 仅入库单
     *   "remark": "2026-10-30 补货",
     *   "operator": "admin",
     *   "items": [{"materialId":1,"ioNum":100}]
     * }
     * </pre>
     * 事务：出库先整体校验库存，任意一条不足整单驳回。全部通过后一次性改库存+写流水+写单据。
     */
    @PostMapping("/order/create")
    public Result<StockOrder> createOrder(@RequestBody CreateOrderDTO dto,
                                          jakarta.servlet.http.HttpServletRequest request) {
        // operator 从 JWT 拦截器写的 request attribute 里取（"wms.username"），兜底 "system"
        String operator = (String) request.getAttribute(
                com.wms.wmsserver.auth.interceptor.JwtInterceptor.ATTR_USERNAME);
        if (operator == null || operator.isBlank()) operator = "system";
        StockOrder order = stockOrderService.createOrder(dto, operator);
        return Result.success(order);
    }

    /** 单据列表（按创建时间倒序，可按类型筛选） */
    @GetMapping("/order/list")
    public Result<List<StockOrder>> listOrders(@RequestParam(required = false) Integer orderType) {
        return Result.success(stockOrderService.listOrders(orderType));
    }

    /** 单据详情：{order: {...}, items: [{materialCode, materialName, ioNum, beforeStock, afterStock}, ...]} */
    @GetMapping("/order/{id}")
    public Result<Map<String, Object>> orderDetail(@PathVariable Long id) {
        return Result.success(stockOrderService.getOrderDetail(id));
    }

    // ==================== 库存查询（含预警） ====================

    /**
     * 实时库存视图：
     * 复用 /list 结果，由服务层额外计算 safeStockLow 标记。
     * 这里直接返回 listAll 即可，前端根据 safeStockNum 判断预警。
     */
    @GetMapping("/stock")
    public Result<List<Material>> stockQuery() {
        return Result.success(materialService.listAll());
    }

    // ==================== 流水台账 ====================

    /** 旧接口兼容：全部流水，可选 ioType 筛选 */
    @GetMapping("/io-records")
    public Result<?> listIoRecords(@RequestParam(required = false) Integer ioType) {
        if (ioType == null) return Result.success(recordService.listAll());
        return Result.success(recordService.listByIoType(ioType));
    }

    /**
     * 流水分页 + 条件筛选（POST 方便 body 传时间范围）。
     * <pre>
     * body: {
     *   "page": 1, "pageSize": 20,
     *   "materialId": null,
     *   "orderType": 1,
     *   "startTime": "2026-10-01T00:00:00",
     *   "endTime":   "2026-10-30T23:59:59"
     * }
     * 返回: {total: N, page:1, pageSize:20, records:[...]}
     * </pre>
     */
    @PostMapping("/io-records/page")
    public Result<Map<String, Object>> pageIoRecords(@RequestBody IoRecordQuery query) {
        return Result.success(recordService.pageQuery(query));
    }
}
