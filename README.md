# WMS — 仓库管理系统

> 《应用软件开发》课程课题三 · 客户机/服务器（C/S）架构桌面应用

JavaFX 桌面客户端 + Spring Boot REST 服务端 + MySQL 8.x 存储，实现人员/物料档案、进出仓（含一单多料）、RBAC 权限、报表与自动升级。

---

## 技术栈

| 层 | 选型 | 版本 |
|----|------|------|
| 运行环境 | JDK | 21 LTS（任意发行版） |
| 客户端 UI | JavaFX + FXML + Scene Builder | 21.0.x LTS |
| 服务端框架 | Spring Boot（内嵌 Tomcat） | 4.0.8 |
| 持久层 | MyBatis-Plus（通用 CRUD + 分页） | 3.5.17（boot4 starter） |
| 数据库 | MySQL | 8.x（≥ 8.0.16） |
| 构建 | Maven 多模块 | — |
| 鉴权 | JWT（jjwt）+ Spring Security crypto | — |

## 模块结构

```
wms/
├── pom.xml               # 父 pom（聚合三模块）
├── wms-common/           # 共享 DTO / 常量 / 错误码 / Result<T>
├── wms-server/           # Spring Boot 服务端（REST API）
└── wms-client/           # JavaFX 桌面客户端（FXML + Controller）
```

## 快速开始

**首次拉取代码**：按 [`docs/CONTRIBUTING.md` §0 环境配置](docs/CONTRIBUTING.md#0-环境配置拉取代码后) 顺序操作即可跑起来（装 JDK/MySQL → clone → 改本地配置 → 建库 → 编译验证）。

**日常开发**：见 [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) 的分支模型、提交规范、Code Review 检查清单。

## 文档索引

| 文档 | 内容 |
|------|------|
| [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) | 需求整理稿（忠实转述课题原文并结构化） |
| [`docs/TECHNICAL_DESIGN.md`](docs/TECHNICAL_DESIGN.md) | 技术方案（架构、接口、数据模型、ADR） |
| [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) | 开发规范（环境配置、提交规范、分层、命名、Review） |

## 协作

团队采用 **Collaborator 模式**（不 fork）：仓库主分支 `main` 走 PR 合并，功能开发切 `feature/<模块>` 分支。详见 [`docs/CONTRIBUTING.md` §2](docs/CONTRIBUTING.md)。
