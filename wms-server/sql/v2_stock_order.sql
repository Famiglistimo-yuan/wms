-- =====================================================================
-- 进出仓子系统 v2 单据化改造
-- 说明：
--   1. 新建 stock_order（单据主表）+ stock_order_item（单据明细）
--   2. ALTER material 加库位、安全库存
--   3. ALTER material_io_record 加操作人、关联单号
--   4. 历史流水用 REMARK 里已有的信息不补数据，新字段允许 NULL
-- =====================================================================

USE wms_dev;

-- 1) 单据主表（入库 / 出库 共用一张表，order_type 区分）
CREATE TABLE IF NOT EXISTS stock_order (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_no      VARCHAR(32)  NOT NULL UNIQUE COMMENT '单据号 自动生成 如 RK20261030001 / CK20261030001',
    order_type    TINYINT      NOT NULL COMMENT '单据类型 1入库 2出库',
    source        VARCHAR(64)  NULL     COMMENT '入库来源(采购/调拨等) 仅入库单使用',
    operator      VARCHAR(64)  NULL     COMMENT '操作人(后端从登录态取)',
    remark        VARCHAR(255) NULL     COMMENT '整单备注',
    total_count   INT          NOT NULL DEFAULT 0 COMMENT '明细条数',
    total_num     DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '明细数量合计',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type_time (order_type, create_time),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进出仓单据主表';

-- 2) 单据明细
CREATE TABLE IF NOT EXISTS stock_order_item (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT UNSIGNED NOT NULL COMMENT '关联 stock_order.id',
    material_id   BIGINT UNSIGNED NOT NULL COMMENT '关联 material.id',
    io_num        DECIMAL(18,4) NOT NULL COMMENT '本次入库/出库数量',
    before_stock  DECIMAL(18,4) NOT NULL COMMENT '操作前库存快照',
    after_stock   DECIMAL(18,4) NOT NULL COMMENT '操作后库存快照',
    INDEX idx_order (order_id),
    INDEX idx_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单据明细(每种物料一行)';

-- 3) material 加库位 + 安全库存（允许 NULL 表示未设置）
ALTER TABLE material
    ADD COLUMN location     VARCHAR(64)  NULL COMMENT '库位' AFTER unit,
    ADD COLUMN safe_stock_num DECIMAL(18,4) NULL COMMENT '安全库存 低于该值预警' AFTER location;

-- 4) material_io_record 加 操作人 + 关联单号 + 单据类型（历史数据允许 NULL）
ALTER TABLE material_io_record
    ADD COLUMN operator    VARCHAR(64)  NULL COMMENT '操作人' AFTER remark,
    ADD COLUMN order_no    VARCHAR(32)  NULL COMMENT '关联单据号' AFTER operator,
    ADD COLUMN order_type  TINYINT      NULL COMMENT '单据类型 1入库 2出库' AFTER order_no,
    ADD INDEX idx_order_no (order_no),
    ADD INDEX idx_create_time (create_time);

-- 给现有物料补个示例库位 / 安全库存，方便看预警
UPDATE material SET location='A-01-01', safe_stock_num=50 WHERE id=1;
