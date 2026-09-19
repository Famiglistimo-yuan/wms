-- =====================================================================
-- 仓库管理系统 种子数据 v1.0（依赖 docs/schema.sql，在空库上执行）
-- 内容：菜单权限清单（13 项）+ 初始管理员（人员/用户/角色/授权关系）
-- 说明：计量单位为客户端可编辑下拉的预设项（技术方案 §7.1），不入库
-- 初始口令：admin123（BCrypt 密文，首次登录后应修改；演示环境可保留）
-- =====================================================================

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
  (13, 'menu.auth.grant',      '权限授予',     41);

-- 进出仓程序覆盖单物料与多物料场景（同一界面入口，明细 1 条即单物料）；
-- 统计与报表菜单（流量统计/月度单/仓库账本）随子系统五开发时补插行

-- ---------- 初始管理员 ----------
INSERT INTO rg2402_11_12_13_person (id, person_code, name, gender) VALUES
  (1, 'P0001', '系统管理员', '男');

INSERT INTO rg2402_11_12_13_user (id, person_id, username, password, status) VALUES
  (1, 1, 'admin',
   '$2a$10$W6Scx1oVlyQs1fdb1ros3OKvP0M5VP.CkRQ32iQBssouY32xGQqHi',
   1);

INSERT INTO rg2402_11_12_13_role (id, role_code, role_name, remark) VALUES
  (1, 'admin', '系统管理员', '拥有全部菜单权限');

INSERT INTO rg2402_11_12_13_user_role (user_id, role_id) VALUES (1, 1);

-- admin 角色 ↔ 全部 13 项权限
INSERT INTO rg2402_11_12_13_role_permission (role_id, perm_id) VALUES
  (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),
  (1,8),(1,9),(1,10),(1,11),(1,12),(1,13);
