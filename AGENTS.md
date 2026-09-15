# AGENTS.md

> 面向 AI 编程助手的仓库上下文，工具自动读取。人类文档：README.md、docs/CONTRIBUTING.md。

## 项目概述

仓库管理系统（C/S 架构）：JavaFX 21 桌面客户端 + Spring Boot 4.0.8 REST 服务端 + MySQL 8 + MyBatis-Plus 3.5.17。三 Maven 模块：

| 模块 | 职责 |
|------|------|
| `wms-common` | 共享 DTO、常量、错误码、`Result<T>`（两端复用，不依赖 Spring/JavaFX） |
| `wms-server` | Spring Boot 服务端（REST API，内嵌 Tomcat，端口 8080） |
| `wms-client` | JavaFX 客户端（FXML + Controller，classpath 项目，无 module-info.java） |

## 常用命令

```bash
mvn -DskipTests clean compile      # 构建验证（改完代码必须跑）
cd wms-server && mvn spring-boot:run   # 启动服务端
cd wms-client && mvn javafx:run    # 启动客户端
```

## 代码规则（强制）

- **服务端分层**：Controller（校验+DTO 转换）→ Service（业务）→ Mapper（持久化），禁止跨层调用、禁止 Controller 写 SQL
- **客户端禁止任何 JDBC/数据库访问**（课题硬性要求），数据一律走 HTTP 调 server
- **JavaFX 线程规则**：HTTP 请求放后台 Task，UI 更新必须 `Platform.runLater`
- **Lombok**：用法与禁用清单见 docs/CONTRIBUTING.md §3.5（`@Data` 不上 Entity/Controller/Service/Mapper）
- **REST 路径、错误码、分页/时间格式**：一律以 docs/TECHNICAL_DESIGN.md §6 为准，不得私造
- **命名前缀**：前缀已定为 `rg2402_11_12_13_`（已配置 MyBatis-Plus table-prefix）——数据表/存储过程名直接拼前缀；「程序名」以 FXML 视图文件名承载（docs/CONTRIBUTING.md §5.1），Java 类名不加前缀

## 禁区（不要做）

- **git 写操作必须先经人工确认**：commit、push、merge、rebase、分支创建/删除、`git reset` 等一律先向人说明将要执行的命令并获得同意；只读命令（status/log/diff/branch）不受限
- **提交/推送前必须自查敏感信息**：暂存区不得含密码、令牌、个人日志或本机路径；本仓库为 **public**，内容一旦推入即永久公开（git 历史不可撤回）。已知排除项（application-local.yaml / .workbuddy/ / HANDOFF.md）之外，凡新增文件先 `git diff --staged` 过目再提交
- **不要修改/提交** `application-local.yaml`（含个人密码，已被 .gitignore 排除）；模板是 `application-local.yaml.example`
- **不要引入新依赖**：技术栈清单见 docs/TECHNICAL_DESIGN.md §3，新增依赖需团队共识
- **不要提交** `target/`、`.idea/`、`.workbuddy/`（后两者不在公有 .gitignore，靠 .git/info/exclude 本地排除）
- **不要动** MyBatis-Plus 坐标：必须用 `mybatis-plus-spring-boot4-starter`，不可与官方 `mybatis-spring-boot-starter` 共存
- **不要恢复** client 的 `module-info.java`（已刻意删除，classpath 模式是团队决策）

## 提交与分支

- Conventional Commits：`type(scope): 中文描述`，type/scope 清单见 docs/CONTRIBUTING.md §1
- 分支：`feature/<模块>` / `fix/<东西>` 小写中划线；main 走 PR 合并
