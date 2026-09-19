# WMS 开发规范

> 适用于《应用软件开发》课题三仓库管理系统（JavaFX 客户端 + Spring Boot 服务端）。
> 本文档与《docs/TECHNICAL_DESIGN.md》配套：技术方案定「做什么」，本文档定「怎么写、怎么协作」。技术内容冲突时以技术方案为准，协作规范以本文档为准。

---

## 0. 环境配置（拉取代码后）

新队友 clone 仓库后，按本节顺序操作即可跑起来。Mac 与 Windows 步骤一致，仅安装包格式不同。

### 0.1 必装工具

| 工具 | 版本 | Mac | Windows |
|------|------|-----|---------|
| JDK | 21（LTS，任意发行版） | Oracle OpenJDK / Temurin aarch64 dmg | Oracle OpenJDK / Temurin x64 msi |
| MySQL | 8.x（≥ 8.0.16） | Homebrew 或官网 dmg | 官网 msi |
| IntelliJ IDEA | Ultimate 或 Community | 官网 dmg | 官网 msi |
| Scene Builder | 21+（仅客户端开发需要） | Gluon 官网 dmg | Gluon 官网 msi |
| Git | 任意近期版本 | 官网 pkg 或 `brew install git` | 官网安装器 |

### 0.2 clone 与导入

```bash
git clone <仓库 HTTPS 地址> wms
cd wms
```

用 IDEA `File → Open` 选中 `wms/` 根目录（不是子模块），IDEA 会检测到 `pom.xml` 弹出「Load Maven Project」，**点 Load**（不要 Skip）。等待依赖下载与索引完成（首次约 3-5 分钟）。

### 0.3 配置本地数据库连接（敏感，不入库）

```bash
cd wms-server/src/main/resources
cp application-local.yaml.example application-local.yaml
```

用 IDEA 打开 `application-local.yaml`，改两个字段：

- `url` 里的 `wms_dev` → 你想用的库名（默认 `wms_dev` 即可，需先建库）
- `password: 你的本地MySQL密码` → 你本地 MySQL root 密码

> 该文件已被 `.gitignore` 排除，不会被推送到远程，密码安全。

### 0.4 建库

用任意 MySQL 客户端（Navicat / IDEA Database 工具 / 命令行）连上本地 MySQL，执行：

```sql
CREATE DATABASE wms_dev DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

`utf8mb4` 是必须的（开发规范 §8.2：避免 Windows 默认 GBK 中文乱码，比 `utf8` 更全）。

### 0.5 验证编译

在 IDEA 终端或 Maven 面板跑：

```bash
./mvnw -DskipTests clean compile
```

期望末尾出现 `BUILD SUCCESS`。失败常见原因：
- JDK 版本不对：`java -version` 确认是 21
- 依赖下载失败：检查网络 / Maven 镜像（国内推荐阿里云镜像）
- MyBatis-Plus 拉不下来：确认是 `mybatis-plus-spring-boot4-starter` 3.5.17（Spring Boot 4 专用，详见 docs/TECHNICAL_DESIGN.md §3）

### 0.6 启动验证

- **服务端**：IDEA 里打开 `wms-server/src/main/java/com/wms/wmsserver/WmsServerApplication.java`，点左侧绿箭头 Run。期望看到 Spring Boot 横幅 + `Tomcat started on port 8080` + HikariCP 连上 MySQL。
- **客户端**：`wms-client` 模块下执行 `mvn javafx:run`，或 IDEA 里运行入口类 `com.wms.wmsclient.Launcher`（不要 Run `HelloApplication`，它没有 main 方法；直接 `java -cp` 会报 `JavaFX runtime components are missing`）。

两端都跑通即环境就绪，可以开始按子系统分工开发了。

> 期末交付：在 Windows 机器上用 `jpackage` 打包 exe/msi（需下载一次 JavaFX SDK 解压供 jlink 组装运行时）。

## 1. Git 提交规范（Conventional Commits）

### 1.1 提交信息格式

```
<type>(<scope>): <subject>

[可选正文：为什么改、怎么改]
[可选尾注：BREAKING CHANGE 等]
```

示例：

```
feat(client): 物料档案界面支持分页与模糊查询

- 新增 MaterialViewController 分页加载
- 关键字输入防抖 300ms 后触发查询
```

### 1.2 type 清单

| type | 含义 | 示例 |
|------|------|------|
| `feat` | 新功能 | 新增登录界面、多物料出仓接口 |
| `fix` | 修 bug | 修复并发出仓超卖问题 |
| `docs` | 仅文档变更 | 更新接口文档、CONTRIBUTING |
| `style` | 代码格式（不影响逻辑） | 调整缩进、删多余空行 |
| `refactor` | 重构（既非新增也非修复） | 抽取公共分页工具类 |
| `perf` | 性能优化 | 加索引、分页优化 |
| `test` | 测试相关 | 补充 Service 层单元测试 |
| `build` | 构建/依赖变更 | 升级 Spring Boot 版本 |
| `chore` | 杂项（不涉及 src 与 test） | 更新 .gitignore |
| `revert` | 回滚某次提交 | revert: 回滚 feat(client): xxx |

### 1.3 scope 约定

| scope | 覆盖范围 |
|-------|---------|
| `server` | `wms-server/` 服务端模块 |
| `client` | `wms-client/` JavaFX 客户端模块 |
| `common` | `wms-common/` 共享 DTO 模块 |
| `db` | 数据库脚本（建表、测试数据） |
| `docs` | `docs/` 文档 |

### 1.4 写作规则

1. **subject 用中文**，一行不超过 50 字；type、scope 保持英文。
2. **一个提交只做一件事**：功能 + 格式化 + 改配置混在一个提交里会导致 review 困难，拆开提交。
3. **破坏性变更**在 type 后加 `!` 并在正文说明，如 `feat(client)!: 登录态存储从文件迁移到 Preferences`。
4. **禁止提交**：`target/`、`.idea/`、`application-local.yaml`、日志文件（已被 .gitignore 覆盖，误提交时用 `git rm --cached` 移除）。
5. 提交前 `git status` + `git diff --staged` 自查一遍，避免误带调试代码（`System.out.println`、写死的密码、注释掉的整段代码）。

## 2. 分支与合并规范

| 分支 | 用途 | 规则 |
|------|------|------|
| `main` | 交付基线 | 保护分支，禁止直接 push，只通过 PR 合入；任何时刻 `main` 必须可编译、可启动 |
| `feature/<模块>` | 功能开发 | 命名如 `feature/material-manage`、`feature/stock-out`，小写中划线分隔 |
| `fix/<问题>` | 修 bug | 如 `fix/login-token-expired` |
| `hotfix/*` | 紧急修复 | 仅期末演示期使用，从 `main` 拉出，修完合回 `main` |

操作约定：

1. **PR 前自测**：服务端改动要能 `mvn spring-boot:run` 启动；客户端改动要能 `mvn javafx:run` 打开对应界面。
2. **合并方式一律 Squash and merge**（仓库设置已限定为唯一方式）；允许合并自己的 PR，质量由 CI 必需检查（`build` 通过才能合并）兜底；涉及事务、权限等核心逻辑的 PR 仍建议交叉 review。
3. **PR 标题即最终 squash 合并的提交标题**，按 Conventional Commits 格式写。
4. 每周至少一次把 `main` 合回自己的 feature 分支（`git merge main`），避免期末集成地狱——对应技术方案 §10 集成联调阶段。
5. 里程碑打 tag：`v0.1.0`（第 3 周末登录跑通）、`v0.5.0`（第 5 周末必选功能完成）、`v1.0.0`（第 6 周交付版）。

## 3. Java 代码规范

### 3.1 命名

| 对象 | 规则 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase；分层后缀必须体现 | `MaterialService`、`StockOrderController` |
| 方法/变量 | lowerCamelCase，方法用动词开头 | `submitStockOrder()`、`stockQty` |
| 常量 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| 包名 | 全小写单数 | `com.wms.server.service` |

### 3.2 服务端分层职责（强制）

| 层 | 允许做 | 禁止做 |
|----|--------|--------|
| Controller | 参数接收、`@Valid` 校验、DTO ↔ VO 转换、调 Service | 业务逻辑、SQL、直接操作 Mapper |
| Service | 业务规则、单号生成、组装并调用存储过程 | 拼 SQL 字符串、操作 `HttpRequest` |
| Mapper | SQL / MyBatis-Plus CRUD | 业务判断（if 扣库存逻辑不写在 SQL 里） |

### 3.3 客户端分层职责（强制）

| 层 | 允许做 | 禁止做 |
|----|--------|--------|
| FXML + Controller | 界面渲染、输入正则校验、调 HTTP 客户端、弹窗提示 | 业务规则（库存够不够由服务端说了算）、任何 JDBC/SQL |
| HTTP 客户端层 | 组装请求、反序列化、携带 JWT | 界面弹窗（把异常抛回 Controller 处理） |

### 3.4 通用约定

1. **JavaFX 线程规则**：所有界面更新必须在 JavaFX Application Thread（`Platform.runLater`）；HTTP 请求放在 `Task`/后台线程，禁止在 UI 线程发请求卡死界面。
2. **不写魔法数字/字符串**：错误码、权限码、单位列表统一定义在 `wms-common` 常量类。
3. **注释**：解释「为什么」，不复述代码；删除即删干净，**不留大段注释掉的死代码入库**（本地调试时可临时注释）。
4. 每个类头保留作者注释 `@author 姓名`，方便课程评分追溯分工。
5. 方法超过 80 行考虑拆分；类超过 400 行考虑拆职责。

### 3.5 Lombok 使用约定

服务端、客户端模块统一引入 Lombok，常用注解与适用对象如下：

| 注解 | 用在哪 | 不用的场景 |
|------|-------|----------|
| `@Data` | DTO / VO / 普通 POJO（人员档案、物料档案） | Controller、Service、Mapper、实体 Entity（避免与 MyBatis-Plus `@TableName` 等冲突，且 `equals/hashCode` 含关联字段时易出 bug） |
| `@Builder` | DTO / VO（构造复杂入参） | 简单 POJO |
| `@NoArgsConstructor` / `@AllArgsConstructor` | 配合 `@Data` 或 Jackson 反序列化需要无参构造时 | — |
| `@RequiredArgsConstructor` | Controller / Service（构造器注入，`final` 字段） | 字段无需注入时 |
| `@Slf4j` | 任意需要日志的类（替换 §4.5 手写 Logger 声明） | — |

禁止用法：

1. 不允许在 Entity 上同时用 `@Data` 和 `@EqualsAndHashCode(callSuper = true)` 处理继承，循环引用风险。
2. `@Builder.Default` 用在带默认值的字段上必须显式标注，否则 Lombok 不会生成默认值。
3. POJO 字段名与 JSON 字段名不一致时，**优先用 Jackson `@JsonProperty`**，不要靠 Lombok 自动生成的 setter 名。

## 4. 服务端专项规范

1. **统一响应**：所有接口返回 `Result<T>`（技术方案 §6.1 的 code/message/data），禁止 Controller 直接返回裸对象或裸 Map。
2. **异常**：业务异常统一抛 `BusinessException(code, message)`，由 `@RestControllerAdvice` 全局处理；禁止在 Controller 里 try-catch 后自己拼 JSON。
3. **事务**：进出仓等多表写入走存储过程，事务在 SP 内；其余应用层多表写入的方法必须显式标注 `@Transactional(rollbackFor = Exception.class)`，单表简单 CRUD 可不标。
4. **DTO 命名**：入参 `XxxCreateDTO` / `XxxQueryDTO`，出参 `XxxVO`；实体类（`Xxx`）不得直接作为接口出参。
5. **日志**：类上加 `@Slf4j`（Lombok，见 §3.5）即可使用 `log.info` / `log.error`，无需手写 `private static final Logger`；关键业务动作记 info（下单成功、登录成功），异常记 error 带上下文；**禁止 `System.out.println` 提交入库**。
6. **接口鉴权**：新增接口默认加 `@RequirePermission("menu.xxx")`，公开接口需在组内说明原因。

## 5. 客户端专项规范（FXML）

1. **文件命名（承载课题「程序名」前缀）**：`resources/fxml/` 下文件名 = 前缀 `rg2402_11_12_13_` + 功能名小写中划线，如 `rg2402_11_12_13_login.fxml`、`rg2402_11_12_13_material-manage.fxml`。课题的「程序名」硬性要求以 FXML 文件名承载——每个功能窗口即一个程序单元，打开 resources 目录即可逐个核对前缀；Java 类名保持标准驼峰、不加前缀（映射口径见需求文档 §6）。
2. **Controller 命名**：与 FXML 对应：`LoginController`、`MaterialManageController`。
3. **fx:id 命名**：控件类型缩写 + 用途驼峰：`tfKeyword`（TextField）、`tvMaterial`（TableView）、`btnSearch`、`dpDate`（DatePicker）。
4. **FXML 只放结构**：不写 `onAction="#handle"` 以外的任何逻辑；事件方法名 `on + 动作`：`onSearchClicked()`、`onSubmitClicked()`。
5. **样式统一**：颜色、间距、字号只写在 `resources/styles.css`，FXML 禁止内联写死样式值（技术方案 §11 界面美观度风险的应对）。
6. **动态内容在 Controller 里填**：表格列、图表数据等运行期数据用 Java 代码填充，不硬编码进 FXML。

## 6. REST API 规范

1. URL 路径**以技术方案 §6.2 接口清单为准**（既有清单内 `stock/orders` 嵌套与 `material-flow` 中划线并存，不再回头统一）；新增接口沿用清单内同类资源的既有风格，先在 SpringDoc 补文档再开发。
2. HTTP 方法语义：查 GET、增 POST、改 PUT、删 DELETE，禁止用 GET 做写操作。
3. 分页参数统一 `page`（从 1 起）+ `size`（默认 20、上限 100），与技术方案 §6.3 一致。
4. 时间字段 JSON 统一 `yyyy-MM-dd HH:mm:ss` 字符串，两端 Jackson 配置保持一致。
5. 错误码沿用技术方案 §6.1 的 0/400/401/403/409/500，不得私造新码；需要新码先在本文档登记。
6. 存储过程内部出口码 409xx（40901 物料不存在 / 40902 库存不足 / 40903 人员不存在 /
   40904 参数无效 / 40905 单据不存在，见 `docs/sql/schema.sql` SP 契约）属数据库层业务码，
   不是新 HTTP 错误码：Service 层统一将 409xx 映射为 HTTP 409，message 原样透传客户端弹窗；
   新增 SP 出口码时在 schema.sql 契约注释中登记，不改 HTTP 码表。

## 7. 数据库规范

1. **命名规范（课题硬性要求，见需求文档 §6）**：程序、数据表、存储过程均以「班级名＋座号」为前缀。本组前缀为 **`rg2402_11_12_13_`**（班级 rg2402，成员座号 11/12/13；含尾部下划线，与对象名分隔）。主界面程序对应 `rg2402_11_12_13_main.fxml`（映射口径见 §5.1）；存储过程名在 `schema.sql` 中直接带前缀；Java 类名不加。
2. 表名前缀 `rg2402_11_12_13_`（与 MyBatis-Plus `table-prefix` 一致）+ 小写下划线；字段小写下划线，靠 MyBatis-Plus 驼峰映射对接 Java。
3. 每张表必备 `id BIGINT AUTO_INCREMENT PRIMARY KEY`；业务唯一键（人员代码、物料代码、单号）加唯一索引。
4. 建表脚本入库：`docs/sql/schema.sql`（结构）与 `docs/sql/data.sql`（初始数据），**禁止只在本地 Navicat 里改表**——他人无法复现你的环境。
5. 改表结构 = 提交 `schema.sql` 变更 + 在群里同步一次，避免他人拉代码后启动报错。
6. 10 万条测试数据脚本单独放 `docs/sql/test-data.sql`，不与初始数据混放。

## 8. 跨平台协作规范（Mac + Windows 混合团队）

1. 换行符：Mac `core.autocrlf=input`，Windows `core.autocrlf=true`，仓库根 `.gitattributes`（`* text=auto`）。
2. 编码：一律 UTF-8（Maven `project.build.sourceEncoding=UTF-8`）；MySQL 连接串带 `characterEncoding=utf8`。
3. 路径：代码中用 `Path` / `ClassLoader.getResource()`，禁止拼盘符和写死正斜杠分隔符。
4. 敏感配置：本地数据库密码只放 `application-local.yaml`（不入库），模板 `application-local.yaml.example` 入库供队友复制。

## 9. Code Review 检查清单

合并 PR 前逐条过（Reviewer 使用）：

- [ ] 提交信息符合 Conventional Commits
- [ ] 分层职责没有越界（Controller 无业务逻辑、客户端无 SQL）
- [ ] 无硬编码密码 / IP / 调试输出
- [ ] 新增接口有权限注解、有 SpringDoc 文档
- [ ] 涉及写库的方法有事务注解或走存储过程
- [ ] 界面更新没有阻塞 UI 线程
- [ ] `schema.sql` 与代码实体一致（若涉及改表）
- [ ] `main` 合入后本地能编译启动

## 元信息

- 目标读者：WMS 开发小组全体成员
- 生效版本：v1.0（2026-09-09）
- 维护方式：通过 PR 修改本文档，改动需组内确认
