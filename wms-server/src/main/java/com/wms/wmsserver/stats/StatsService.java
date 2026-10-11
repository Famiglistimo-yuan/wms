package com.wms.wmsserver.stats;

import com.wms.common.BusinessException;
import com.wms.common.ErrorCode;
import com.wms.common.dto.MaterialFlowReport;
import com.wms.common.dto.MaterialFlowStat;
import com.wms.common.dto.MaterialOption;
import com.wms.wmsserver.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/**
 * 物料流量统计（FR-5-1）：聚合查询 + 流动量最小物料分析（分析在服务端，ADR-005）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    /** 各物料指定时间内的进出仓总数 + 流动量最小物料。日期串格式 yyyy-MM-dd，from ≤ to */
    public MaterialFlowReport materialFlow(String from, String to) {
        LocalDate fromDate = parseDate(from, "起始日期");
        LocalDate toDate = parseDate(to, "截止日期");
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "起始日期不能晚于截止日期");
        }
        List<MaterialFlowStat> items = statsMapper.selectFlowStats(fromDate, toDate);
        // 流动量 = 进 + 出，服务端算好免得客户端与报表各算一遍
        items.forEach(it -> it.setTotalFlow(it.getInQty().add(it.getOutQty())));
        items.sort(Comparator.comparing(MaterialFlowStat::getTotalFlow).reversed());
        MaterialFlowStat min = items.isEmpty() ? null : items.get(items.size() - 1);
        log.info("物料流量统计 {} ~ {}，共 {} 种物料，最小流动：{}",
                from, to, items.size(), min == null ? "无" : min.getMaterialCode());
        return MaterialFlowReport.builder().items(items).minFlowMaterial(min).build();
    }

    /** 物料下拉选项（账本面板） */
    public List<MaterialOption> materialOptions() {
        return statsMapper.selectMaterialOptions();
    }

    private LocalDate parseDate(String date, String field) {
        if (date == null || date.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, field + "不能为空");
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, field + "格式须为 yyyy-MM-dd");
        }
    }
}
