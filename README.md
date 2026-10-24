# WMS — 仓库管理系统

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![build](https://github.com/Famiglistimo-yuan/wms/actions/workflows/ci.yml/badge.svg)](https://github.com/Famiglistimo-yuan/wms/actions/workflows/ci.yml)

> 《应用软件开发》课程课题三 · 客户机/服务器（C/S）架构桌面应用

JavaFX 桌面客户端 + Spring Boot REST 服务端 + MySQL 8.x 存储，实现人员/物料档案、进出仓（含一单多料）、RBAC 权限、统计报表与客户端自动升级。

---

## 技术栈

| 层 | 选型 | 版本 |
|----|------|------|
| 运行环境 | JDK | 21 LTS（任意发行版） |
| 客户端 UI | JavaFX + FXML + Scene Builder | 21.0.x LTS |
| 服务端框架 | Spring Boot（内嵌 Tomcat，端口 8080） | 4.0.8 |
| 持久层 | MyBatis-Plus（通用 CRUD + 分页） | 3.5.17（boot4 starter） |
| 数据库 | MySQL | 8.x（≥ 8.0.16） |
| 报表 | Apache POI（Excel + Word 双格式，ADR-009） | 5.5.1 |
| 鉴权 | JWT（jjwt）+ Spring Security crypto（BCrypt） | — |
| 构建 | Maven 多模块 | — |

## 模块结构

```
wms/
├── pom.xml               # 根 pom（挂 SB parent，聚合四模块并统一版本管理）
├── wms-common/           # 共享 DTO / 常量 / 错误码 / Result<T>（两端复用）
├── wms-server/           # Spring Boot 服务端（REST API）
├── wms-client/           # JavaFX 桌面客户端（FXML + Controller）
├── wms-updater/          # 独立升级器（纯 JDK + Swing——主程序无法替换自己）
├── docs/                 # 需求 / 技术方案（含 ADR）/ 开发规范 / SQL 脚本
└── scripts/              # jpackage 打包脚本、图标矢量源
```

## 快速开始

环境：JDK 21 + MySQL 8。完整步骤（含 IDE 配置）见
[`docs/CONTRIBUTING.md` §0 环境配置](docs/CONTRIBUTING.md#0-环境配置拉取代码后)。

```bash
# 1. 本地配置
cd wms-server/src/main/resources && cp application-local.yaml.example application-local.yaml

# 2. 建库 + 建表 + 种子数据
mysql -u root -p -e "CREATE DATABASE wms_dev DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mysql -u root -p wms_dev < docs/sql/schema.sql
mysql -u root -p wms_dev < docs/sql/data.sql

# 3. 编译并启动服务端
./mvnw -DskipTests clean compile
./mvnw -pl wms-server -am spring-boot:run

# 4. 启动客户端（另开终端）
./mvnw -f wms-client/pom.xml javafx:run
```

默认管理员：`admin / admin123`（种子数据，权限全开）。

## 打包与自动升级

`scripts/package-appimage.sh` 以 jpackage 产出平台 app-image（主 jar 固定名 + MD5 清单）。
服务端 `/api/version` 下发版本信息，客户端启动时后台检查，发现新版本后由 `wms-updater`
独立进程完成下载、MD5 校验、滚动 `.bak` 备份与原子替换，并自动重启主程序。
设计与取舍见技术方案 §7.5 与 ADR-008。

## 文档索引

| 文档 | 内容 |
|------|------|
| [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) | 需求整理稿（忠实转述课题原文并结构化） |
| [`docs/TECHNICAL_DESIGN.md`](docs/TECHNICAL_DESIGN.md) | 技术方案（架构、接口、数据模型、ADR） |
| [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) | 开发规范（环境配置、提交规范、分层、命名、Review） |
| [`docs/sql/`](docs/sql) | 建库脚本（11 表 + 存储过程）与种子数据 v1.1 |

## 协作

团队采用 **Collaborator 模式**（不 fork）：`main` 走 PR（Squash merge），功能分支
`feature/<模块>`、视觉资产 `ui/<界面或资产>` 等前缀见
[`docs/CONTRIBUTING.md` §2](docs/CONTRIBUTING.md)。
