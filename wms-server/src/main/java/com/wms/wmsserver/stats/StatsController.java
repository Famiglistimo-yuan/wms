package com.wms.wmsserver.stats;

import com.wms.common.Result;
import com.wms.common.dto.MaterialFlowReport;
import com.wms.common.dto.MaterialOption;
import com.wms.wmsserver.auth.annotation.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计接口（FR-5-1）。技术方案 §6.2：
 *   GET /api/stats/material-flow?from=&to=      物料流量统计（图表数据）
 *   GET /api/stats/material-options             物料下拉选项（账本面板）
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/material-flow")
    @RequirePermission("menu.stats.flow")
    public Result<MaterialFlowReport> materialFlow(@RequestParam String from, @RequestParam String to) {
        return Result.success(statsService.materialFlow(from, to));
    }

    @GetMapping("/material-options")
    @RequirePermission("menu.stats.ledger")
    public Result<List<MaterialOption>> materialOptions() {
        return Result.success(statsService.materialOptions());
    }
}
