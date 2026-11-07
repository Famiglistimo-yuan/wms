package com.wms.wmsserver.stock.controller;

import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.common.PageResult;
import com.wms.common.Result;
import com.wms.common.dto.OrderDetailVO;
import com.wms.common.dto.OrderVO;
import com.wms.common.dto.PersonOptionVO;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import com.wms.wmsserver.auth.interceptor.JwtInterceptor;
import com.wms.wmsserver.stock.dto.OrderCreateDTO;
import com.wms.wmsserver.stock.service.StockOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 进出仓单 REST 接口（FR-2 / FR-3，技术方案 §6.2）。
 * <p>
 * 权限：查询类固定 menu.stock.query；提交接口进/出共用一入口，无法用单一
 * {@code @RequirePermission} 表达，按 orderType 动态校验 menu.stock.in / menu.stock.out
 * （权限集由 JwtInterceptor 写入 request attribute）。
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockOrderController {

    private final StockOrderService stockOrderService;

    /**
     * 提交进出仓单（单物料/多物料同一接口，明细数组长度 ≥ 1）。
     * 事务/乐观锁在存储过程内；409xx 映射 409，message 原样透传。
     */
    @PostMapping("/orders")
    public Result<String> create(@Valid @RequestBody OrderCreateDTO dto,
                                 HttpServletRequest request) {
        String required = "OUT".equals(dto.getOrderType()) ? "menu.stock.out" : "menu.stock.in";
        requirePermission(request, required);
        Long userId = (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        return Result.success(stockOrderService.create(dto, userId));
    }

    /** 单据组合查询：dateFrom/dateTo/material/operator/remark/orderType 均可选，分页 page+size */
    @GetMapping("/orders")
    @RequirePermission("menu.stock.query")
    public Result<PageResult<OrderVO>> query(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String orderType,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(stockOrderService.query(dateFrom, dateTo, material,
                operator, remark, orderType, page, size));
    }

    /** 单据详情（主表行 + 明细列表，含物料档案） */
    @GetMapping("/orders/{id}")
    @RequirePermission("menu.stock.query")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(stockOrderService.detail(id));
    }

    /** 经手人下拉选项：进出仓面板共用，持有任一进出仓权限即可加载 */
    @GetMapping("/person-options")
    public Result<List<PersonOptionVO>> personOptions(HttpServletRequest request) {
        List<String> permissions = permissions(request);
        if (!permissions.contains("menu.stock.in") && !permissions.contains("menu.stock.out")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限：menu.stock.in / menu.stock.out");
        }
        return Result.success(stockOrderService.personOptions());
    }

    /** 从 JWT attribute 取权限集（动态鉴权场景，见类注释） */
    private void requirePermission(HttpServletRequest request, String required) {
        if (!permissions(request).contains(required)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限：" + required);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> permissions(HttpServletRequest request) {
        List<String> permissions = (List<String>) request.getAttribute(JwtInterceptor.ATTR_PERMISSIONS);
        return permissions == null ? List.of() : permissions;
    }
}
