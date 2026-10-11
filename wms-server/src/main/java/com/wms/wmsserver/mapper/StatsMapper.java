package com.wms.wmsserver.mapper;

import com.wms.common.dto.MaterialFlowStat;
import com.wms.common.dto.MaterialOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计与报表聚合查询（FR-5）。只读聚合，直出 VO，不建 Material/StockOrder 实体——
 * 聚合本不需要实体，且避免与 FR-1/FR-2/3 分支在数据契约层 add/add 冲突。
 */
@Mapper
public interface StatsMapper {

    /**
     * 各物料在 [from, to] 内的进出仓数量总和。
     * 日期条件放在 JOIN 而非 WHERE：区间内无流水的物料仍保留（进出为 0），
     * 「流动量最小物料」分析才完整（呆滞物料不应凭空消失）。
     */
    @Select("""
            SELECT m.material_code AS materialCode,
                   m.name          AS name,
                   m.spec          AS spec,
                   m.unit          AS unit,
                   COALESCE(SUM(CASE WHEN o.order_type = 'IN'  THEN i.quantity END), 0) AS inQty,
                   COALESCE(SUM(CASE WHEN o.order_type = 'OUT' THEN i.quantity END), 0) AS outQty
              FROM rg2402_11_12_13_material m
              LEFT JOIN rg2402_11_12_13_stock_order_item i
                     ON i.material_id = m.id
              LEFT JOIN rg2402_11_12_13_stock_order o
                     ON o.id = i.order_id
                    AND o.order_date BETWEEN #{from} AND #{to}
             GROUP BY m.id, m.material_code, m.name, m.spec, m.unit
            """)
    List<MaterialFlowStat> selectFlowStats(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 物料下拉选项（账本面板用，代码升序便于定位） */
    @Select("""
            SELECT material_code AS materialCode, name AS name
              FROM rg2402_11_12_13_material
             ORDER BY material_code
            """)
    List<MaterialOption> selectMaterialOptions();
}
