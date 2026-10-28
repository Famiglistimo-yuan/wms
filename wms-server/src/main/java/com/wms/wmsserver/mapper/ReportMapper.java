package com.wms.wmsserver.mapper;

import com.wms.wmsserver.reports.dto.LedgerRow;
import com.wms.wmsserver.reports.dto.MaterialBrief;
import com.wms.wmsserver.reports.dto.MonthlyOrderRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 报表数据查询（FR-5-2 / FR-5-3）。只读直出行 DTO，同 StatsMapper 不建实体。
 * 区间条件用 [from, to) 半开区间而非 YEAR()/MONTH() 函数，可走 idx_order_date_type 索引。
 */
@Mapper
public interface ReportMapper {

    /** 某月全部进出仓单明细，按单号顺序（FR-5-2 课题硬性要求） */
    @Select("""
            SELECT o.order_no     AS orderNo,
                   o.order_type   AS orderType,
                   o.order_date   AS orderDate,
                   p1.name        AS operatorName,
                   p2.name        AS handlerName,
                   m.material_code AS materialCode,
                   m.name         AS materialName,
                   m.unit         AS unit,
                   i.quantity     AS quantity,
                   o.remark       AS remark
              FROM rg2402_11_12_13_stock_order o
              JOIN rg2402_11_12_13_stock_order_item i ON i.order_id = o.id
              JOIN rg2402_11_12_13_person p1 ON p1.id = o.operator_id
              JOIN rg2402_11_12_13_person p2 ON p2.id = o.handler_id
              JOIN rg2402_11_12_13_material m ON m.id = i.material_id
             WHERE o.order_date >= #{from} AND o.order_date < #{to}
             ORDER BY o.order_no, i.id
            """)
    List<MonthlyOrderRow> selectMonthlyOrders(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 某月进出仓单明细总数（预览分页用；与 selectMonthlyOrders 同 JOIN 同条件） */
    @Select("""
            SELECT COUNT(*)
              FROM rg2402_11_12_13_stock_order o
              JOIN rg2402_11_12_13_stock_order_item i ON i.order_id = o.id
              JOIN rg2402_11_12_13_person p1 ON p1.id = o.operator_id
              JOIN rg2402_11_12_13_person p2 ON p2.id = o.handler_id
              JOIN rg2402_11_12_13_material m ON m.id = i.material_id
             WHERE o.order_date >= #{from} AND o.order_date < #{to}
            """)
    long countMonthlyOrders(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 某月进出仓单明细分页（页面预览用；ORDER BY 与导出一致，翻到的页即导出文件里对应的段） */
    @Select("""
            SELECT o.order_no     AS orderNo,
                   o.order_type   AS orderType,
                   o.order_date   AS orderDate,
                   p1.name        AS operatorName,
                   p2.name        AS handlerName,
                   m.material_code AS materialCode,
                   m.name         AS materialName,
                   m.unit         AS unit,
                   i.quantity     AS quantity,
                   o.remark       AS remark
              FROM rg2402_11_12_13_stock_order o
              JOIN rg2402_11_12_13_stock_order_item i ON i.order_id = o.id
              JOIN rg2402_11_12_13_person p1 ON p1.id = o.operator_id
              JOIN rg2402_11_12_13_person p2 ON p2.id = o.handler_id
              JOIN rg2402_11_12_13_material m ON m.id = i.material_id
             WHERE o.order_date >= #{from} AND o.order_date < #{to}
             ORDER BY o.order_no, i.id
             LIMIT #{size} OFFSET #{offset}
            """)
    List<MonthlyOrderRow> selectMonthlyOrdersPage(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                                  @Param("offset") long offset, @Param("size") int size);

    /** 物料摘要（账本报表头 + 期初反推需要当前库存） */
    @Select("""
            SELECT id, material_code AS materialCode, name, spec, unit, stock
              FROM rg2402_11_12_13_material
             WHERE material_code = #{materialCode}
            """)
    MaterialBrief selectMaterialBrief(@Param("materialCode") String materialCode);

    /** 某物料 [from, to) 内按日期聚合的进/出数量（FR-5-3 各日期行） */
    @Select("""
            SELECT o.order_date AS orderDate,
                   COALESCE(SUM(CASE WHEN o.order_type = 'IN'  THEN i.quantity END), 0) AS inQty,
                   COALESCE(SUM(CASE WHEN o.order_type = 'OUT' THEN i.quantity END), 0) AS outQty
              FROM rg2402_11_12_13_stock_order_item i
              JOIN rg2402_11_12_13_stock_order o ON o.id = i.order_id
             WHERE i.material_id = #{materialId}
               AND o.order_date >= #{from} AND o.order_date < #{to}
             GROUP BY o.order_date
             ORDER BY o.order_date
            """)
    List<LedgerRow> selectLedgerRows(@Param("materialId") Long materialId,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 自 from 起（含）某物料的净变动：进为正、出为负。账本期初 = 当前库存 − 该值（期初反推口径） */
    @Select("""
            SELECT COALESCE(SUM(CASE WHEN o.order_type = 'IN' THEN i.quantity ELSE -i.quantity END), 0)
              FROM rg2402_11_12_13_stock_order_item i
              JOIN rg2402_11_12_13_stock_order o ON o.id = i.order_id
             WHERE i.material_id = #{materialId} AND o.order_date >= #{from}
            """)
    BigDecimal selectNetFlowSince(@Param("materialId") Long materialId, @Param("from") LocalDate from);
}
