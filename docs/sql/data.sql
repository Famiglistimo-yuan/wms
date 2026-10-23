-- =====================================================================
-- 仓库管理系统 种子数据 v1.1（依赖 docs/sql/schema.sql，在空库上执行）
-- 内容：菜单权限清单（16 项）+ 初始管理员 + 测试用户体系（权限分档/
--       allow−deny 混合/停用）+ 物料 10 种 + 跨四个月进出仓历史单据
-- 说明：计量单位为客户端可编辑下拉的预设项（技术方案 §7.1），不入库
-- 初始口令：全部用户均为 admin123（BCrypt 密文，首次登录后应修改）
-- 账目平衡：物料期初库存为 0，当前库存 = Σ进仓明细 − Σ出仓明细，
--          仓库账本/月度报表/流量统计可基于本数据逐笔核对
-- 执行：单事务包裹，任一语句失败则不 COMMIT、连接断开整体回滚；
--   mysql --default-character-set=utf8mb4 -u <用户> -p <库名> < docs/sql/data.sql
--   （连接字符集非 utf8mb4 时 Φ/× 等符号可能乱码入库；MySQL 8 客户端默认即 utf8mb4）
-- =====================================================================

START TRANSACTION;

-- ---------- 菜单权限清单（FR-4：每个菜单项一个资源，新增功能=插行） ----------
INSERT INTO rg2402_11_12_13_permission (id, perm_code, perm_name, sort_order) VALUES
  (1,  'menu.person.view',     '人员档案查询', 10),
  (2,  'menu.person.add',      '人员档案增加', 11),
  (3,  'menu.person.edit',     '人员档案修改', 12),
  (4,  'menu.person.delete',   '人员档案删除', 13),
  (5,  'menu.material.view',   '物料档案查询', 20),
  (6,  'menu.material.add',    '物料档案增加', 21),
  (7,  'menu.material.edit',   '物料档案修改', 22),
  (8,  'menu.material.delete', '物料档案删除', 23),
  (9,  'menu.stock.in',        '进仓录入',     30),
  (10, 'menu.stock.out',       '出仓录入',     31),
  (11, 'menu.stock.query',     '进出仓单查询', 32),
  (12, 'menu.auth.user',       '用户管理',     40),
  (13, 'menu.auth.grant',      '权限授予',     41),
  (14, 'menu.stats.flow',      '物料流量统计', 50),
  (15, 'menu.stats.monthly',   '月度进出仓单', 51),
  (16, 'menu.stats.ledger',    '仓库账本',     52);

-- ---------- 人员档案（8 人，字段覆盖度分档） ----------
INSERT INTO rg2402_11_12_13_person
  (id, person_code, name, gender, birth_date, id_card, hometown, address, phone, remark) VALUES
  (1, 'P0001', '系统管理员', '男', NULL, NULL, NULL, NULL, NULL, '内置初始人员'),
  (2, 'P0002', '张伟',       '男', '1985-03-12', '110101198503120011', '北京市',
   '北京市朝阳区建国路88号', '13801010002', NULL),
  (3, 'P0003', '李娜',       '女', '1992-07-25', '310101199207250026', '上海市',
   '上海市浦东新区张江路5号', '13902020003', '统计报表负责人'),
  (4, 'P0004', '王强',       '男', '1988-11-03', NULL, NULL, NULL, NULL, NULL),
  (5, 'P0005', '刘敏',       '女', '1990-05-18', '440101199005180045', '广东省广州市',
   '广州市天河区中山大道西10号', '13703030005', NULL),
  (6, 'P0006', '陈磊',       '男', '1994-09-08', '11010119940908491X', NULL,
   '北京市海淀区知春路2号', '13604040006', NULL),  -- 身份证以 X 结尾，覆盖正则末位分支
  (7, 'P0007', '赵芳',       '女', '1996-12-30', NULL, NULL, NULL, NULL, '账号已停用'),
  (8, 'P0008', '周杰',       '男', NULL, NULL, NULL, NULL, NULL, '仅作经手人，无登录账号');

-- ---------- 登录用户（6 个，口令均为 admin123） ----------
INSERT INTO rg2402_11_12_13_user (id, person_id, username, password, status) VALUES
  (1, 1, 'admin',     '$2a$10$W6Scx1oVlyQs1fdb1ros3OKvP0M5VP.CkRQ32iQBssouY32xGQqHi', 1),
  (2, 2, 'zhangwei',  '$2a$10$W6Scx1oVlyQs1fdb1ros3OKvP0M5VP.CkRQ32iQBssouY32xGQqHi', 1),
  (3, 3, 'lina',      '$2a$10$W6Scx1oVlyQs1fdb1ros3OKvP0M5VP.CkRQ32iQBssouY32xGQqHi', 1),
  (4, 4, 'wangqiang', '$2a$10$W6Scx1oVlyQs1fdb1ros3OKvP0M5VP.CkRQ32iQBssouY32xGQqHi', 1),
  (5, 6, 'chenlei',   '$2a$10$W6Scx1oVlyQs1fdb1ros3OKvP0M5VP.CkRQ32iQBssouY32xGQqHi', 1),
  (6, 7, 'zhaofang',  '$2a$10$W6Scx1oVlyQs1fdb1ros3OKvP0M5VP.CkRQ32iQBssouY32xGQqHi', 0);  -- 停用，登录应被拒

-- ---------- 角色（FR-4-3，角色仅作标签不参与权限继承） ----------
INSERT INTO rg2402_11_12_13_role (id, role_code, role_name, remark) VALUES
  (1, 'admin',        '系统管理员', '拥有全部菜单权限'),
  (2, 'keeper',       '仓管员',     '负责物料档案与进出仓录入'),
  (3, 'statistician', '统计员',     '负责统计与报表导出');

INSERT INTO rg2402_11_12_13_user_role (user_id, role_id) VALUES
  (1, 1), (2, 2), (3, 3);

-- 角色权限仅作数据示范；实际生效以 user_permission 直授为准
INSERT INTO rg2402_11_12_13_role_permission (role_id, perm_id) VALUES
  (2, 5), (2, 6), (2, 7), (2, 9), (2, 10), (2, 11),
  (3, 5), (3, 14), (3, 15), (3, 16);

-- ---------- 用户直接权限（有效权限 = allow − deny） ----------
-- admin：全部 16 项 allow
INSERT INTO rg2402_11_12_13_user_permission (user_id, perm_id, effect) VALUES
  (1,1,'allow'),(1,2,'allow'),(1,3,'allow'),(1,4,'allow'),(1,5,'allow'),(1,6,'allow'),(1,7,'allow'),
  (1,8,'allow'),(1,9,'allow'),(1,10,'allow'),(1,11,'allow'),(1,12,'allow'),(1,13,'allow'),
  (1,14,'allow'),(1,15,'allow'),(1,16,'allow');
-- zhangwei：仓管员（查询/物料档案/进出仓录入）
INSERT INTO rg2402_11_12_13_user_permission (user_id, perm_id, effect) VALUES
  (2,1,'allow'),(2,5,'allow'),(2,6,'allow'),(2,7,'allow'),
  (2,9,'allow'),(2,10,'allow'),(2,11,'allow');
-- lina：统计员（物料查询+三项报表）
INSERT INTO rg2402_11_12_13_user_permission (user_id, perm_id, effect) VALUES
  (3,5,'allow'),(3,14,'allow'),(3,15,'allow'),(3,16,'allow');
-- wangqiang：最小权限（仅查询）
INSERT INTO rg2402_11_12_13_user_permission (user_id, perm_id, effect) VALUES
  (4,5,'allow'),(4,11,'allow');
-- chenlei：allow−deny 混合，可增改物料但禁止删除
INSERT INTO rg2402_11_12_13_user_permission (user_id, perm_id, effect) VALUES
  (5,5,'allow'),(5,6,'allow'),(5,7,'allow'),(5,11,'allow'),(5,8,'deny');

-- ---------- 物料档案（10 种，当前库存与下方单据明细严格平衡） ----------
INSERT INTO rg2402_11_12_13_material (id, material_code, name, spec, unit, stock, remark) VALUES
  (1,  'M0001', '十字槽螺丝',   'M6×20 镀锌',      '个', 950.000, NULL),
  (2,  'M0002', '六角螺母',     'M6 不锈钢',       '个', 900.000, NULL),
  (3,  'M0003', '深沟球轴承',   '6204-2RS',        '个', 200.000, NULL),
  (4,  'M0004', '无缝钢管',     'Φ25×1.5mm',       '米', 480.000, '按米发放，数量可为小数'),
  (5,  'M0005', '防锈油漆',     '铁红 中量级',     '箱', 55.000,  '易燃，远离火源'),
  (6,  'M0006', '工业齿轮油',   'L-CKD 46号',      '桶', 35.000,  NULL),
  (7,  'M0007', '帆布手套',     '加厚耐磨',        '副', 20.000,  NULL),
  (8,  'M0008', '水砂纸',       '120目 230×280',   '包', 220.000, NULL),
  (9,  'M0009', '三相异步电机', 'Y112M-4 4kW',     '台', 15.000,  '单价高，流动量最小'),
  (10, 'M0010', '橡胶密封圈',   'Φ50 耐油',        '个', 600.000, NULL);

-- ---------- 进出仓历史单据（2026-07 ~ 2026-10，期初库存为 0） ----------
-- 库存核算示例：M0001 = 1000+500−200−350 = 950，全表同法逐笔平衡
INSERT INTO rg2402_11_12_13_stock_order
  (id, order_no, order_type, order_date, operator_id, handler_id, remark) VALUES
  (1,  'IN202607010001',  'IN',  '2026-07-01', 1, 8, '建账进仓：五金件'),
  (2,  'IN202607010002',  'IN',  '2026-07-01', 1, 8, '建账进仓：油化品'),
  (3,  'IN202607010003',  'IN',  '2026-07-01', 1, 8, '建账进仓：劳保与设备'),
  (4,  'OUT202607150001', 'OUT', '2026-07-15', 1, 2, '车间领料'),
  (5,  'IN202608050001',  'IN',  '2026-08-05', 2, 3, NULL),
  (6,  'OUT202608200001', 'OUT', '2026-08-20', 2, 3, '暑期检修领料'),
  (7,  'IN202609100001',  'IN',  '2026-09-10', 2, 4, NULL),
  (8,  'OUT202609220001', 'OUT', '2026-09-22', 2, 4, NULL),
  (9,  'IN202610080001',  'IN',  '2026-10-08', 2, 5, '四季度备料'),
  (10, 'OUT202610180001', 'OUT', '2026-10-18', 2, 5, NULL);

INSERT INTO rg2402_11_12_13_stock_order_item (order_id, material_id, quantity) VALUES
  -- 1 建账：五金
  (1, 1, 1000.000), (1, 2, 1000.000), (1, 3, 200.000), (1, 4, 500.000),
  -- 2 建账：油化
  (2, 5, 50.000), (2, 6, 30.000),
  -- 3 建账：劳保与设备
  (3, 7, 100.000), (3, 8, 200.000), (3, 9, 10.000), (3, 10, 500.000),
  -- 4 出仓
  (4, 1, 200.000), (4, 3, 20.000), (4, 7, 50.000),
  -- 5 进仓
  (5, 1, 500.000), (5, 3, 100.000), (5, 8, 100.000),
  -- 6 出仓
  (6, 2, 300.000), (6, 4, 120.500), (6, 5, 10.000),
  -- 7 进仓
  (7, 5, 30.000), (7, 6, 20.000), (7, 9, 5.000),
  -- 8 出仓
  (8, 1, 350.000), (8, 3, 80.000), (8, 6, 15.000),
  -- 9 进仓
  (9, 2, 200.000), (9, 4, 100.500), (9, 10, 100.000),
  -- 10 出仓
  (10, 5, 15.000), (10, 7, 30.000), (10, 8, 80.000);

-- ---------- 单号流水（与上方历史单据严格对齐，防 SP 后续生成单号撞 uk_order_no） ----------
INSERT INTO rg2402_11_12_13_order_seq (seq_prefix, seq_date, current_seq) VALUES
  ('IN',  '2026-07-01', 3),
  ('OUT', '2026-07-15', 1),
  ('IN',  '2026-08-05', 1),
  ('OUT', '2026-08-20', 1),
  ('IN',  '2026-09-10', 1),
  ('OUT', '2026-09-22', 1),
  ('IN',  '2026-10-08', 1),
  ('OUT', '2026-10-18', 1);

COMMIT;

-- 操作日志由服务端 AOP 切面运行时写入，不做种子数据
