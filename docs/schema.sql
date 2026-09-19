-- =====================================================================
-- 仓库管理系统 数据契约 v1.0（Phase 1 定稿，团队评审通过 2026-09-19）
-- 前缀 rg2402_11_12_13_：班级 rg2402 + 成员座号 11/12/13
-- 环境：MySQL 8.x（≥ 8.0.16，CHECK 约束硬性依赖；本机与 CI 实测 8.4）
--       存储引擎 InnoDB，字符集 utf8mb4（MySQL 8 默认，无需逐表指定）
-- 约定：主键统一 BIGINT 自增；业务代码列另设 UNIQUE；
--       数量一律 DECIMAL，禁用 FLOAT；
--       created_at/updated_at DEFAULT CURRENT_TIMESTAMP（后者 ON UPDATE）
-- =====================================================================

-- ---------- 人员档案（FR-1-2） ----------
CREATE TABLE rg2402_11_12_13_person (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  person_code VARCHAR(20)  NOT NULL COMMENT '人员代码',
  name        VARCHAR(50)  NOT NULL COMMENT '姓名',
  gender      CHAR(1)      NOT NULL COMMENT '性别，界面单选，存原字面值',
  birth_date  DATE         NULL     COMMENT '出生日期，界面日期控件',
  id_card     VARCHAR(18)  NULL     COMMENT '身份证号，应用层正则校验为主',
  hometown    VARCHAR(100) NULL     COMMENT '籍贯',
  address     VARCHAR(200) NULL     COMMENT '家庭住址',
  phone       VARCHAR(20)  NULL     COMMENT '联系电话',
  remark      VARCHAR(500) NULL     COMMENT '其它情况',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_person_code (person_code),
  CONSTRAINT chk_person_gender CHECK (gender IN ('男', '女')),
  CONSTRAINT chk_person_idcard CHECK (
    id_card IS NULL OR REGEXP_LIKE(id_card, '^[0-9]{17}[0-9Xx]$'))
) COMMENT '人员档案';

-- ---------- 登录用户（FR-4-1，口令独立成表，BCrypt 密文） ----------
CREATE TABLE rg2402_11_12_13_user (
  id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
  person_id   BIGINT      NOT NULL COMMENT '关联人员，一人一账号',
  username    VARCHAR(30) NOT NULL COMMENT '登录用户名',
  password    VARCHAR(60) NOT NULL COMMENT 'BCrypt 密文，固定 60 字符',
  status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_person (person_id),
  UNIQUE KEY uk_user_username (username),
  CONSTRAINT chk_user_status CHECK (status IN (0, 1)),
  CONSTRAINT fk_user_person FOREIGN KEY (person_id)
    REFERENCES rg2402_11_12_13_person (id)
) COMMENT '登录用户';

-- ---------- RBAC（FR-4-3 / FR-4-7） ----------
CREATE TABLE rg2402_11_12_13_role (
  id        BIGINT      AUTO_INCREMENT PRIMARY KEY,
  role_code VARCHAR(30) NOT NULL,
  role_name VARCHAR(50) NOT NULL,
  remark    VARCHAR(200) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_code (role_code)
) COMMENT '角色';

CREATE TABLE rg2402_11_12_13_permission (
  id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
  perm_code  VARCHAR(50) NOT NULL COMMENT '权限编码=菜单项标识，如 menu.material.add',
  perm_name  VARCHAR(50) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0 COMMENT '菜单显示顺序',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_perm_code (perm_code)
) COMMENT '权限资源（每个菜单项一行，新增权限=插行，无需改代码）';

CREATE TABLE rg2402_11_12_13_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES rg2402_11_12_13_user (id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES rg2402_11_12_13_role (id)
) COMMENT '用户-角色';

CREATE TABLE rg2402_11_12_13_role_permission (
  role_id BIGINT NOT NULL,
  perm_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, perm_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES rg2402_11_12_13_role (id),
  CONSTRAINT fk_rp_perm FOREIGN KEY (perm_id) REFERENCES rg2402_11_12_13_permission (id)
) COMMENT '角色-权限';

-- ---------- 物料档案（FR-1-3） ----------
CREATE TABLE rg2402_11_12_13_material (
  id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
  material_code VARCHAR(30)   NOT NULL COMMENT '物料代码，可自动生成或手输',
  name          VARCHAR(100)  NOT NULL COMMENT '名称',
  spec          VARCHAR(100)  NULL COMMENT '规格型号',
  unit          VARCHAR(20)   NOT NULL COMMENT '计量单位，可编辑下拉：预设+自输入',
  stock         DECIMAL(12,3) NOT NULL DEFAULT 0 COMMENT '库存数量',
  remark        VARCHAR(500)  NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_material_code (material_code),
  KEY idx_material_name (name),
  CONSTRAINT chk_material_stock CHECK (stock >= 0) COMMENT '乐观锁之外的最后一道防线'
) COMMENT '物料档案';

-- ---------- 进出仓单主表（FR-2 / FR-3，主表+明细支撑一单多料） ----------
CREATE TABLE rg2402_11_12_13_stock_order (
  id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
  order_no    VARCHAR(20) NOT NULL COMMENT '单号：IN/OUT+yyyyMMdd+4位流水，SP 内生成',
  order_type  CHAR(3)     NOT NULL COMMENT 'IN=进仓 OUT=出仓，主表级保证同单同向',
  order_date  DATE        NOT NULL,
  operator_id BIGINT      NOT NULL COMMENT '操作人，取当前登录用户',
  handler_id  BIGINT      NOT NULL COMMENT '经手人，人员档案下拉',
  remark      VARCHAR(500) NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_date_type (order_date, order_type),
  KEY idx_order_operator (operator_id),
  CONSTRAINT chk_order_type CHECK (order_type IN ('IN', 'OUT')),
  CONSTRAINT fk_order_operator FOREIGN KEY (operator_id) REFERENCES rg2402_11_12_13_person (id),
  CONSTRAINT fk_order_handler  FOREIGN KEY (handler_id)  REFERENCES rg2402_11_12_13_person (id)
) COMMENT '进出仓单主表';

-- ---------- 进出仓单明细（FR-3-1，一单多料） ----------
CREATE TABLE rg2402_11_12_13_stock_order_item (
  id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
  order_id    BIGINT        NOT NULL,
  material_id BIGINT        NOT NULL,
  quantity    DECIMAL(12,3) NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_item_order_material (order_id, material_id) COMMENT '同单同物料唯一，重复录入界面合并',
  KEY idx_item_material (material_id) COMMENT '流量统计/仓库账本按物料聚合',
  CONSTRAINT chk_item_quantity CHECK (quantity > 0),
  CONSTRAINT fk_item_order FOREIGN KEY (order_id)
    REFERENCES rg2402_11_12_13_stock_order (id),  -- 刻意不用 CASCADE：删单必须走 SP 冲正库存
  CONSTRAINT fk_item_material FOREIGN KEY (material_id)
    REFERENCES rg2402_11_12_13_material (id)
) COMMENT '进出仓单明细';

-- ---------- 单号流水（SP 内行锁自增，并发安全） ----------
CREATE TABLE rg2402_11_12_13_order_seq (
  seq_prefix  CHAR(3) NOT NULL COMMENT 'IN / OUT',
  seq_date    DATE    NOT NULL,
  current_seq INT     NOT NULL DEFAULT 0,
  PRIMARY KEY (seq_prefix, seq_date)
) COMMENT '单号流水；SP 流程：INSERT IGNORE 建行 → UPDATE 自增（取行锁）→ 读值';

-- ---------- 操作日志（课题可选功能，本期做，实现为服务端 AOP 切面） ----------
CREATE TABLE rg2402_11_12_13_op_log (
  id         BIGINT        AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT        NULL,
  action     VARCHAR(50)   NOT NULL COMMENT '操作类型，如 ORDER_CREATE / PERSON_UPDATE',
  detail     VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_oplog_user_time (user_id, created_at)
) COMMENT '操作日志';

-- =====================================================================
-- 存储过程契约（FR-2-8 / FR-3-6 硬性要求：进出仓必须走 SP，
-- 事务提交/回滚、物料存在与库存非负校验、乐观锁均在 SP 内，锁与事务不分离）
--
-- 本节仅锁定签名与行为语义（签名-only 的 CREATE PROCEDURE 不可执行，
-- 故以注释承载）；事务体实现属 Phase 2（进出仓子系统），届时以完整
-- CREATE PROCEDURE 落盘（本文件此节或独立 docs/sp_orders.sql，由实现者定）。
--
-- 统一出口：OUT p_code / p_message，Service 层将 409xx 映射为 HTTP 409，
-- message 原样透传客户端弹窗：
--   0     成功
--   40901 物料不存在
--   40902 库存不足（含删进仓单时货已被消耗的拒绝场景）
--   40903 操作人/经手人不存在
--   40904 参数无效（明细空 / JSON 非法 / 数量 ≤ 0）
--   40905 单据不存在（update / delete 场景）
-- 明细入参统一 JSON 数组：[{"materialId":1,"quantity":10}, ...]
--
-- 1) 提交进出仓单（单物料=明细长度 1，多物料=长度 N，同一入口）
--    流程：锁 seq 生成单号 → 校验人员 → 逐条物料存在校验 +
--    条件更新库存（OUT: stock=stock-qty WHERE stock>=qty，影响 0 行即失败）
--    → 写主表+明细 → COMMIT；EXIT HANDLER 异常整体 ROLLBACK
--    rg2402_11_12_13_order_create(
--      IN  p_order_type CHAR(3),  IN p_order_date  DATE,
--      IN  p_operator_id BIGINT,  IN p_handler_id  BIGINT,
--      IN  p_remark     VARCHAR(500), IN p_items    JSON,
--      OUT p_order_no   VARCHAR(20), OUT p_code    INT,
--      OUT p_message    VARCHAR(200))
--
-- 2) 修改进出仓单：整单替换明细（旧明细先冲正库存再按新明细重写；
--    单号/类型/操作人不可改，保证 FR-3-4 同单一致性）
--    rg2402_11_12_13_order_update(
--      IN  p_order_id  BIGINT,   IN p_order_date  DATE,
--      IN  p_handler_id BIGINT,  IN p_remark      VARCHAR(500),
--      IN  p_items     JSON,
--      OUT p_code      INT,      OUT p_message    VARCHAR(200))
--
-- 3) 删除进出仓单：库存冲正（进仓单删→扣回，受 stock>=qty 约束，
--    货已被出仓消耗则拒绝；出仓单删→回补）
--    rg2402_11_12_13_order_delete(
--      IN  p_order_id BIGINT,
--      OUT p_code     INT, OUT p_message VARCHAR(200))
-- =====================================================================

-- 种子数据（菜单权限清单、初始管理员、常用计量单位）见 docs/seed.sql（待出）
