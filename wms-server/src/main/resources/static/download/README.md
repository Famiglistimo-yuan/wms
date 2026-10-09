# 升级包目录（FR-6）

此目录已**外置**：由 `WebMvcConfig` 代码将 `/download/**`
映射到服务端运行目录的 `./downloads/`（文件系统），升级包不入服务端 jar——发客户端新版只换
文件、无需重新构建/重启服务端 jar。本 README 仅保留占位说明。

发布流程（详见 docs/TECHNICAL_DESIGN.md §7.5）：

1. `./mvnw versions:set -DnewVersion=x.y.z -DgenerateBackupPoms=false`（版本号单一来源，version.txt 随之注入）
2. `./scripts/package-appimage.sh x.y.z`（末尾打印主 jar 的 MD5）
3. 产物主 jar（`target/jpackage/WMS[.app]/app/wms-client.jar`）拷入服务端运行目录的 `./downloads/` 下（URL 为 `/download/wms-client.jar`）
4. 版本号与 MD5 填入 `application.yaml` 的 `wms.update` 块，重启 server

**Windows 交付必须整流程重跑**：JavaFX 依赖带平台 classifier（macOS 产物是
`javafx-*-macos-aarch64.jar`，Windows 需 `-win.jar`）——不能复用 macOS 上 copy-dependencies
得到的 `target/dist/`，须在 Windows 机器从 `./mvnw clean package` 起完整执行，否则 jpackage
产物能装但启动报 `UnsatisfiedLinkError`。

打包脚本为 bash（macOS/Linux 或 Windows 的 Git Bash/WSL 下执行）。Windows 交付期若用 PowerShell，等参数 jpackage 命令为：

```powershell
jpackage --type app-image --name WMS --app-version x.y.z --vendor "rg2402_11_12_13" `
  --input <dist目录> --main-jar wms-client.jar --main-class com.wms.wmsclient.Launcher --dest target\jpackage
```

（目录结构与脚本产物一致，MD5 用 `certutil -hashfile wms-client.jar MD5`。）
