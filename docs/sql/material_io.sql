CREATE TABLE IF NOT EXISTS material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    material_code VARCHAR(64) NOT NULL UNIQUE COMMENT '物料编码',
    material_name VARCHAR(128) NOT NULL COMMENT '物料名称',
    spec VARCHAR(128) COMMENT '规格型号',
    unit VARCHAR(32) COMMENT '单位',
    stock_num DECIMAL(10,2) DEFAULT 0.00 COMMENT '当前库存数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删 1已删'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料基础表';

CREATE TABLE IF NOT EXISTS material_io_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    material_id BIGINT NOT NULL COMMENT '物料ID，关联material表',
    io_type TINYINT NOT NULL COMMENT '出入库类型：1入库，2出库',
    io_num DECIMAL(10,2) NOT NULL COMMENT '出入库数量',
    before_stock DECIMAL(10,2) NOT NULL COMMENT '操作前库存',
    after_stock DECIMAL(10,2) NOT NULL COMMENT '操作后库存',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    FOREIGN KEY (material_id) REFERENCES material(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料出入库记录表';
