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
     * 日期条件在派生表内过滤（谓词下推）：明细表扫描被区间限定，避免全量展开；
     * 物料表 LEFT JOIN 派生表保证零流动物料保留（呆滞物料不消失），
     * 「流动量最小物料」分析才完整。
     */
    @Select("""
            SELECT m.material_code AS materialCode,
                   m.name          AS name,
                   m.spec          AS spec,
                   m.unit          AS unit,
                   COALESCE(f.in_qty, 0)  AS inQty,
                   COALESCE(f.out_qty, 0) AS outQty
              FROM rg2402_11_12_13_material m
              LEFT JOIN (
                    SELECT i.material_id,
                           SUM(CASE WHEN o.order_type = 'IN'  THEN i.quantity END) AS in_qty,
                           SUM(CASE WHEN o.order_type = 'OUT' THEN i.quantity END) AS out_qty
                      FROM rg2402_11_12_13_stock_order_item i
                      JOIN rg2402_11_12_13_stock_order o ON o.id = i.order_id
                     WHERE o.order_date BETWEEN #{from} AND #{to}
                     GROUP BY i.material_id
                   ) f ON f.material_id = m.id
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
