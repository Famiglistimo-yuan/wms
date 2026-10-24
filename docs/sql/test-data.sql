-- =====================================================================
-- 仓库管理系统 大数据量性能测试数据 test-data.sql
-- 目的：验证「大量用户大量数据时功能实现是否仍有较好性能」
--       （课题质量要求；技术方案 §10 阶段 3 既定的 test-data.sql）
-- 前置：空库已按序执行 schema.sql → data.sql（本脚本叠加在种子之上）
-- 执行：mysql -u root -p wms_dev < docs/sql/test-data.sql   （约半分钟内）
-- 规模：人员 10,000 + 登录用户 100 + 物料 1,000
--       + 进出仓单 100,000（进/出各 5 万，一单一料）+ 明细 100,000
-- 窗口：2025-07-01 ~ 2026-06-30（完整 12 个月，避开种子单据 2026-07 起
--       的单号段，防撞 uk_order_no）
-- 守恒：每物料「配对进出」——第 i 笔出仓数量 ≤ 第 i 笔进仓且晚一天，
--       任意时点库存 ≥ 0；期末 stock = Σ进 − Σ出，账实一致（与 data.sql
--       同口径，账本/流量统计可逐笔核对）
-- 幂等：非幂等，重复执行因 person_code 唯一键冲突报错中止（天然防重）；
--       清理见文末注释块
-- =====================================================================

-- ---------- 1. 人员档案 10,000 人（TP00001..，高频姓名组合便于 LIKE 压测） ----------
INSERT INTO rg2402_11_12_13_person
  (person_code, name, gender, birth_date, id_card, hometown, address, phone, remark)
WITH nums AS (
  SELECT d1.i + d2.i * 10 + d3.i * 100 + d4.i * 1000 + 1 AS n
  FROM (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d1
  JOIN (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d2
  JOIN (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d3
  JOIN (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d4
)
SELECT
  CONCAT('TP', LPAD(n, 5, '0')),
  CONCAT(ELT(1 + n % 20, '张','王','李','赵','刘','陈','杨','黄','周','吴',
                            '徐','孙','马','朱','胡','郭','何','林','罗','郑'),
         ELT(1 + n % 15, '伟','芳','娜','敏','静','丽','强','磊','军','洋',
                         '勇','艳','杰','涛','明')),
  IF(n % 9 < 5, '男', '女'),
  DATE_ADD('1960-01-01', INTERVAL (n * 37) % 15000 DAY),
  CONCAT('35010019', LPAD(n, 8, '0'), '01'),
  ELT(1 + n % 8, '北京市','上海市','广东省广州市','浙江省杭州市','江苏省南京市',
                 '四川省成都市','湖北省武汉市','福建省福州市'),
  CONCAT(ELT(1 + n % 8, '北京市','上海市','广州市','杭州市','南京市','成都市','武汉市','福州市'),
         '测试区压测路', n, '号'),
  CONCAT('138', LPAD(n, 8, '0')),
  NULL
FROM nums;

-- ---------- 2. 登录用户 100 个（perf001..100，口令同种子 admin123） ----------
INSERT INTO rg2402_11_12_13_user (person_id, username, password, status)
SELECT id, CONCAT('perf', LPAD(ROW_NUMBER() OVER (ORDER BY id), 3, '0')),
       '$2a$10$W6Scx1oVlyQs1fdb1ros3OKvP0M5VP.CkRQ32iQBssouY32xGQqHi', 1
FROM rg2402_11_12_13_person
WHERE person_code LIKE 'TP%'
ORDER BY id
LIMIT 100;

-- 授予查询 + 进出仓 + 统计报表权限（并发登录/录单演示可用）
INSERT INTO rg2402_11_12_13_user_permission (user_id, perm_id, effect)
SELECT u.id, p.id, 'allow'
FROM rg2402_11_12_13_user u
JOIN rg2402_11_12_13_permission p
  ON p.id IN (1, 5, 9, 10, 11, 14, 15, 16)
WHERE u.username LIKE 'perf%';

-- ---------- 3. 物料档案 1,000 种（TM0001..，名称/规格含可模糊命中词） ----------
INSERT INTO rg2402_11_12_13_material (material_code, name, spec, unit, stock, remark)
WITH nums AS (
  SELECT d1.i + d2.i * 10 + d3.i * 100 + 1 AS n   -- 1..1000
  FROM (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d1
  JOIN (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d2
  JOIN (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d3
)
SELECT
  CONCAT('TM', LPAD(n, 4, '0')),
  CONCAT(ELT(1 + n % 12, '轴承','螺栓','垫圈','阀门','法兰','密封件',
                          '电机','传感器','继电器','开关','线缆','接头'), LPAD(n, 4, '0'), '型'),
  CONCAT('规格-', 10 + n % 90, '系列'),
  ELT(1 + n % 8, '件','套','公斤','吨','升','米','毫米','个'),
  0,   -- 库存先置 0，第 6 步按明细重算（账实一致）
  '性能测试数据'
FROM nums;

-- ---------- 4. 进出仓单 100,000 张（1000 物料 × 50 组配对进出） ----------
-- 组 p 的进仓日 = 2025-07-01 + floor((p-1)*7.3) 天 + 物料序号%7（错开铺满 12 个月），
-- 出仓日 = 进仓日 + 1；出仓量(30~100) ≤ 进仓量(100~500)，保库存非负
INSERT INTO rg2402_11_12_13_stock_order
  (order_no, order_type, order_date, operator_id, handler_id, remark)
WITH
pairs AS (  -- 1..50
  SELECT d1.i + d2.i * 10 + 1 AS p
  FROM (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d1
  JOIN (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) d2
),
mats AS (
  SELECT id AS mat_id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS m
  FROM rg2402_11_12_13_material
  WHERE material_code LIKE 'TM%'
),
opers AS (
  SELECT id AS pid, ROW_NUMBER() OVER (ORDER BY id) - 1 AS r
  FROM rg2402_11_12_13_person
  WHERE person_code LIKE 'TP%'
),
typ AS (SELECT 'IN' AS t UNION ALL SELECT 'OUT'),
gen AS (
  SELECT
    ty.t AS order_type,
    DATE_ADD(
      DATE_ADD('2025-07-01', INTERVAL FLOOR((pa.p - 1) * 365 / 50) + ma.m % 7 DAY),
      INTERVAL IF(ty.t = 'OUT', 1, 0) DAY) AS order_date,
    CASE WHEN ty.t = 'IN'
         THEN 100 + ((ma.m * 17 + pa.p * 7) % 401)     -- 100 ~ 500
         ELSE 30 + ((ma.m * 3 + pa.p) % 71) END AS qty, -- 30 ~ 100
    ma.mat_id, pa.p, ma.m
  FROM mats ma CROSS JOIN pairs pa CROSS JOIN typ ty
)
SELECT
  CONCAT(g.order_type, DATE_FORMAT(g.order_date, '%Y%m%d'),
         LPAD(ROW_NUMBER() OVER (PARTITION BY g.order_type, g.order_date
                                 ORDER BY g.mat_id), 4, '0')),
  g.order_type, g.order_date,
  op.pid, hd.pid, '性能测试'
FROM gen g
JOIN opers op ON op.r =  (g.m + g.p)     % 10000
JOIN opers hd ON hd.r = (g.m + g.p * 2) % 10000;

-- ---------- 5. 明细 100,000 行（按单号回联主表取 id；单号表达式与上一步完全一致） ----------
INSERT INTO rg2402_11_12_13_stock_order_item (order_id, material_id, quantity)
WITH
pairs AS (
  SELECT d1.i + d2.i * 10 + 1 AS p
  FROM (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d1
  JOIN (SELECT 0 i UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) d2
),
mats AS (
  SELECT id AS mat_id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS m
  FROM rg2402_11_12_13_material
  WHERE material_code LIKE 'TM%'
),
typ AS (SELECT 'IN' AS t UNION ALL SELECT 'OUT'),
gen AS (
  SELECT
    ty.t AS order_type,
    DATE_ADD(
      DATE_ADD('2025-07-01', INTERVAL FLOOR((pa.p - 1) * 365 / 50) + ma.m % 7 DAY),
      INTERVAL IF(ty.t = 'OUT', 1, 0) DAY) AS order_date,
    CASE WHEN ty.t = 'IN'
         THEN 100 + ((ma.m * 17 + pa.p * 7) % 401)
         ELSE 30 + ((ma.m * 3 + pa.p) % 71) END AS qty,
    ma.mat_id, pa.p, ma.m
  FROM mats ma CROSS JOIN pairs pa CROSS JOIN typ ty
),
numbered AS (
  SELECT
    CONCAT(g.order_type, DATE_FORMAT(g.order_date, '%Y%m%d'),
           LPAD(ROW_NUMBER() OVER (PARTITION BY g.order_type, g.order_date
                                   ORDER BY g.mat_id), 4, '0')) AS order_no,
    g.mat_id, g.qty
  FROM gen g
)
SELECT o.id, n.mat_id, n.qty
FROM numbered n
JOIN rg2402_11_12_13_stock_order o ON o.order_no = n.order_no;

-- ---------- 6. 库存重算：stock = Σ进 − Σ出（含种子物料，全表账实一致） ----------
UPDATE rg2402_11_12_13_material m
JOIN (
  SELECT i.material_id,
         SUM(CASE WHEN o.order_type = 'IN' THEN i.quantity ELSE -i.quantity END) AS net
  FROM rg2402_11_12_13_stock_order_item i
  JOIN rg2402_11_12_13_stock_order o ON o.id = i.order_id
  GROUP BY i.material_id
) t ON t.material_id = m.id
SET m.stock = t.net;

-- ---------- 7. 单号流水回写（每天取 MAX，防 SP 后续生成撞 uk_order_no） ----------
INSERT INTO rg2402_11_12_13_order_seq (seq_prefix, seq_date, current_seq)
SELECT order_type, order_date, COUNT(*)
FROM rg2402_11_12_13_stock_order
GROUP BY order_type, order_date
ON DUPLICATE KEY UPDATE current_seq = GREATEST(current_seq, VALUES(current_seq));

-- ---------- 8. 灌入校验（行数 + 账实一致，最后一条应返回 0 行） ----------
SELECT '人员' AS 项, COUNT(*) AS 行数 FROM rg2402_11_12_13_person WHERE person_code LIKE 'TP%'
UNION ALL SELECT '登录用户', COUNT(*) FROM rg2402_11_12_13_user WHERE username LIKE 'perf%'
UNION ALL SELECT '物料',     COUNT(*) FROM rg2402_11_12_13_material WHERE material_code LIKE 'TM%'
UNION ALL SELECT '进出仓单', COUNT(*) FROM rg2402_11_12_13_stock_order WHERE remark = '性能测试'
UNION ALL SELECT '明细',     COUNT(*) FROM rg2402_11_12_13_stock_order_item i
  JOIN rg2402_11_12_13_stock_order o ON o.id = i.order_id WHERE o.remark = '性能测试';

SELECT m.material_code, m.stock AS 账面, IFNULL(t.net, 0) AS 明细净额
FROM rg2402_11_12_13_material m
LEFT JOIN (
  SELECT i.material_id,
         SUM(CASE WHEN o.order_type = 'IN' THEN i.quantity ELSE -i.quantity END) AS net
  FROM rg2402_11_12_13_stock_order_item i
  JOIN rg2402_11_12_13_stock_order o ON o.id = i.order_id
  GROUP BY i.material_id
) t ON t.material_id = m.id
WHERE m.stock <> IFNULL(t.net, 0);   -- 应 0 行

-- ---------- 9. 性能探针（配合 EXPLAIN 观察索引命中，记录耗时入报告） ----------
-- 单字查人（姓名高频字，大结果集）：   SELECT COUNT(*) FROM rg2402_11_12_13_person WHERE name LIKE '%伟%';
-- 物料模糊（名称/规格）：              SELECT * FROM rg2402_11_12_13_material WHERE name LIKE '%轴承%' OR spec LIKE '%50%';
-- 深分页（单据表第 4501 页）：          SELECT * FROM rg2402_11_12_13_stock_order ORDER BY id LIMIT 20 OFFSET 90000;
-- 月度聚合（10 万单取一个月）：         SELECT o.order_type, COUNT(*), SUM(i.quantity)
--                                      FROM rg2402_11_12_13_stock_order o
--                                      JOIN rg2402_11_12_13_stock_order_item i ON i.order_id = o.id
--                                      WHERE o.order_date BETWEEN '2025-09-01' AND '2025-09-30'
--                                      GROUP BY o.order_type;
-- 账本聚合（单物料全年逐日）：          SELECT o.order_date, o.order_type, i.quantity
--                                      FROM rg2402_11_12_13_stock_order_item i
--                                      JOIN rg2402_11_12_13_stock_order o ON o.id = i.order_id
--                                      WHERE i.material_id = 500 AND o.order_date >= '2026-01-01'
--                                      ORDER BY o.order_date;
-- 组合查询（日期+物料+操作人）：        SELECT COUNT(*) FROM rg2402_11_12_13_stock_order
--                                      WHERE order_date BETWEEN '2026-03-01' AND '2026-03-31'
--                                        AND operator_id = 500;

-- =====================================================================
-- 清理（如需重新灌入，按序执行后重跑本脚本；第 6 步的库存重算再执行一次）
-- DELETE FROM rg2402_11_12_13_stock_order_item WHERE order_id IN
--   (SELECT id FROM (SELECT id FROM rg2402_11_12_13_stock_order WHERE remark = '性能测试') x);
-- DELETE FROM rg2402_11_12_13_stock_order WHERE remark = '性能测试';
-- DELETE FROM rg2402_11_12_13_user_permission WHERE user_id IN
--   (SELECT id FROM (SELECT id FROM rg2402_11_12_13_user WHERE username LIKE 'perf%') x);
-- DELETE FROM rg2402_11_12_13_user WHERE username LIKE 'perf%';
-- DELETE FROM rg2402_11_12_13_person WHERE person_code LIKE 'TP%';
-- DELETE FROM rg2402_11_12_13_material WHERE material_code LIKE 'TM%';
-- DELETE FROM rg2402_11_12_13_order_seq
--   WHERE seq_date BETWEEN '2025-07-01' AND '2026-06-30' AND current_seq > 3;
-- =====================================================================
