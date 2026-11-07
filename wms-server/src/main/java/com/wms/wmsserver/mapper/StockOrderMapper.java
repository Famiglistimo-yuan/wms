package com.wms.wmsserver.mapper;

import com.wms.common.dto.OrderItemVO;
import com.wms.common.dto.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 进出仓单查询与 SP 调用（FR-2 / FR-3）。
 * <p>
 * 写路径走存储过程 rg2402_11_12_13_order_create（事务/乐观锁/校验均在其内，
 * 见 docs/sql/schema.sql SP 契约节）；读路径注解 SQL 直出 VO，不建 StockOrder 实体——
 * 写入由 SP 承担、避免与子系统的数据契约层产生 add/add 冲突（同 StatsMapper 范式）。
 */
@Mapper
public interface StockOrderMapper {

    /**
     * SP 调用：提交进出仓单。参数与 OUT 结果均经 params Map 传递：
     * IN 键 orderType / orderDate / operatorId / handlerId / remark / items(JSON 字符串)，
     * OUT 键 orderNo / code / message（SP 出口码 409xx 语义见 schema.sql）。
     */
    @Select("CALL rg2402_11_12_13_order_create("
            + "#{orderType, mode=IN, jdbcType=CHAR}, "
            + "#{orderDate, mode=IN, jdbcType=DATE}, "
            + "#{operatorId, mode=IN, jdbcType=BIGINT}, "
            + "#{handlerId, mode=IN, jdbcType=BIGINT}, "
            + "#{remark, mode=IN, jdbcType=VARCHAR}, "
            + "#{items, mode=IN, jdbcType=VARCHAR}, "
            + "#{orderNo, mode=OUT, jdbcType=VARCHAR}, "
            + "#{code, mode=OUT, jdbcType=INTEGER}, "
            + "#{message, mode=OUT, jdbcType=VARCHAR})")
    @Options(statementType = StatementType.CALLABLE)
    void callOrderCreate(Map<String, Object> params);

    /** 单据组合查询（技术方案 §6.2：dateFrom/dateTo/material/operator/remark，另含 orderType 与分页） */
    @Select("""
            <script>
            SELECT o.id         AS id,
                   o.order_no   AS orderNo,
                   o.order_type AS orderType,
                   DATE_FORMAT(o.order_date, '%Y-%m-%d') AS orderDate,
                   p1.name      AS operatorName,
                   p2.name      AS handlerName,
                   (SELECT COUNT(*) FROM rg2402_11_12_13_stock_order_item i
                     WHERE i.order_id = o.id) AS itemCount,
                   (SELECT COALESCE(SUM(i.quantity), 0) FROM rg2402_11_12_13_stock_order_item i
                     WHERE i.order_id = o.id) AS totalQuantity,
                   o.remark     AS remark
              FROM rg2402_11_12_13_stock_order o
              JOIN rg2402_11_12_13_person p1 ON p1.id = o.operator_id
              JOIN rg2402_11_12_13_person p2 ON p2.id = o.handler_id
             <where>
               <if test="dateFrom != null">o.order_date &gt;= #{dateFrom}</if>
               <if test="dateTo != null">AND o.order_date &lt;= #{dateTo}</if>
               <if test="orderType != null and orderType != ''">AND o.order_type = #{orderType}</if>
               <if test="material != null and material != ''">
                 AND EXISTS (SELECT 1 FROM rg2402_11_12_13_stock_order_item i
                              JOIN rg2402_11_12_13_material m ON m.id = i.material_id
                             WHERE i.order_id = o.id
                               AND (m.material_code LIKE CONCAT('%', #{material}, '%')
                                    OR m.name LIKE CONCAT('%', #{material}, '%')))
               </if>
               <if test="operator != null and operator != ''">AND p1.name LIKE CONCAT('%', #{operator}, '%')</if>
               <if test="remark != null and remark != ''">AND o.remark LIKE CONCAT('%', #{remark}, '%')</if>
             </where>
             ORDER BY o.id DESC
             LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<OrderVO> selectOrdersPage(@Param("dateFrom") LocalDate dateFrom,
                                   @Param("dateTo") LocalDate dateTo,
                                   @Param("material") String material,
                                   @Param("operator") String operator,
                                   @Param("remark") String remark,
                                   @Param("orderType") String orderType,
                                   @Param("offset") long offset,
                                   @Param("size") int size);

    /** 同条件计数（与 selectOrdersPage 的 WHERE 保持一致） */
    @Select("""
            <script>
            SELECT COUNT(*)
              FROM rg2402_11_12_13_stock_order o
              JOIN rg2402_11_12_13_person p1 ON p1.id = o.operator_id
              JOIN rg2402_11_12_13_person p2 ON p2.id = o.handler_id
             <where>
               <if test="dateFrom != null">o.order_date &gt;= #{dateFrom}</if>
               <if test="dateTo != null">AND o.order_date &lt;= #{dateTo}</if>
               <if test="orderType != null and orderType != ''">AND o.order_type = #{orderType}</if>
               <if test="material != null and material != ''">
                 AND EXISTS (SELECT 1 FROM rg2402_11_12_13_stock_order_item i
                              JOIN rg2402_11_12_13_material m ON m.id = i.material_id
                             WHERE i.order_id = o.id
                               AND (m.material_code LIKE CONCAT('%', #{material}, '%')
                                    OR m.name LIKE CONCAT('%', #{material}, '%')))
               </if>
               <if test="operator != null and operator != ''">AND p1.name LIKE CONCAT('%', #{operator}, '%')</if>
               <if test="remark != null and remark != ''">AND o.remark LIKE CONCAT('%', #{remark}, '%')</if>
             </where>
            </script>
            """)
    long countOrders(@Param("dateFrom") LocalDate dateFrom,
                     @Param("dateTo") LocalDate dateTo,
                     @Param("material") String material,
                     @Param("operator") String operator,
                     @Param("remark") String remark,
                     @Param("orderType") String orderType);

    /** 单条单据（详情主表行，字段口径与列表一致） */
    @Select("""
            SELECT o.id         AS id,
                   o.order_no   AS orderNo,
                   o.order_type AS orderType,
                   DATE_FORMAT(o.order_date, '%Y-%m-%d') AS orderDate,
                   p1.name      AS operatorName,
                   p2.name      AS handlerName,
                   (SELECT COUNT(*) FROM rg2402_11_12_13_stock_order_item i
                     WHERE i.order_id = o.id) AS itemCount,
                   (SELECT COALESCE(SUM(i.quantity), 0) FROM rg2402_11_12_13_stock_order_item i
                     WHERE i.order_id = o.id) AS totalQuantity,
                   o.remark     AS remark
              FROM rg2402_11_12_13_stock_order o
              JOIN rg2402_11_12_13_person p1 ON p1.id = o.operator_id
              JOIN rg2402_11_12_13_person p2 ON p2.id = o.handler_id
             WHERE o.id = #{orderId}
            """)
    OrderVO selectOrder(@Param("orderId") Long orderId);

    /** 单据明细（详情弹窗用，含物料档案信息） */
    @Select("""
            SELECT m.material_code AS materialCode,
                   m.name          AS materialName,
                   m.spec          AS spec,
                   m.unit          AS unit,
                   i.quantity      AS quantity
              FROM rg2402_11_12_13_stock_order_item i
              JOIN rg2402_11_12_13_material m ON m.id = i.material_id
             WHERE i.order_id = #{orderId}
             ORDER BY i.id
            """)
    List<OrderItemVO> selectOrderItems(@Param("orderId") Long orderId);
}
