# 升级包目录（FR-6）

此目录存放客户端升级包 `wms-client.jar`（构建产物，**二进制不入库**，本 README 仅占位建目录）。

发布流程（详见 docs/TECHNICAL_DESIGN.md §7.5）：

1. `./mvnw versions:set -DnewVersion=x.y.z -DgenerateBackupPoms=false`（版本号单一来源，version.txt 随之注入）
2. `./scripts/package-appimage.sh`（末尾打印主 jar 的 MD5）
3. 产物主 jar（`target/jpackage/WMS[.app]/app/wms-client.jar`）拷入本目录
4. 版本号与 MD5 填入 `application.yaml` 的 `wms-update` 块，重启 server
