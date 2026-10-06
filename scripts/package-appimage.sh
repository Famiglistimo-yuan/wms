#!/usr/bin/env bash
# jpackage app-image 打包（FR-6，非模块化路径——classpath 模式无 module-info，jlink 不可用）。
# 用法：./scripts/package-appimage.sh [版本号]
#   版本号省略时默认 1.0.0——jpackage 只收非 0 开头的纯数字三段，项目版本 0.1.0-SNAPSHOT 不合法；
#   发版时显式传参（./scripts/package-appimage.sh 1.0.1），并用 versions:set 同步 pom（version.txt 随之注入）
# 产物：target/jpackage/WMS.app（Mac）/ WMS/（Windows 同参数 jpackage）
# 发布流程：见 wms-server/src/main/resources/static/download/README.md
set -euo pipefail
cd "$(dirname "$0")/.."

DIST="$PWD/target/dist"   # 绝对路径：-pl wms-client 时 maven 的相对路径基准是模块 basedir，会拷错位置
APP_VERSION="${1:-1.0.0}"

# clean 必须：jar 插件 up-to-date 跳过会让 shade 把上一次的 fat jar 误当输入，
# original 滚雪球（多次无 clean 构建后 original 越来越胖）
# install 必须：下一步 copy-dependencies 单跑 -pl wms-client（无 -am），要从 .m2
# 解析 wms-common；versions:set 到新版本号后 .m2 无对应 artifact，package 不入库会解析失败
./mvnw -q -DskipTests clean install

rm -rf "$DIST" target/jpackage
mkdir -p "$DIST"

# 依赖平铺到 DIST 顶层（jpackage 只对顶层 jar 登记 classpath，子目录不递归）：
# shade 已把 wms-common、Jackson（tools.jackson.core:jackson-core/databind、
# com.fasterxml.jackson.core:jackson-annotations）打入主 jar，此处逐一剔除防重复装载与版本错位；
# JavaFX 等 native lib 依赖仍随 app/ 分发、不随升级变更（ADR-008 边界）
./mvnw -q -pl wms-client dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="$DIST"
rm -f "$DIST/wms-common"*.jar \
      "$DIST/jackson-core"*.jar \
      "$DIST/jackson-databind"*.jar \
      "$DIST/jackson-annotations"*.jar

# 主 jar 固定名（ADR-008：WMS.cfg 写死 main-jar 文件名，带版本号升级后启动器找不到）。
# 排除 sources/javadoc/original 等（防未来构建变体加入后 cp 多参报错）
MAIN_JAR_SOURCE="$(find wms-client/target -maxdepth 1 -name 'wms-client-*.jar' \
  ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name 'original-*' | head -1)"
cp "$MAIN_JAR_SOURCE" "$DIST/wms-client.jar"
# Updater 同住 app/（不参与升级替换）
cp wms-updater/target/updater.jar "$DIST/updater.jar"

jpackage \
  --type app-image \
  --name WMS \
  --app-version "$APP_VERSION" \
  --vendor "rg2402_11_12_13" \
  --input "$DIST" \
  --main-jar "wms-client.jar" \
  --main-class com.wms.wmsclient.Launcher \
  --dest target/jpackage

# 发布辅助：打印主 jar MD5（发版时填入 application.yaml 的 wms-update.md5）
MAIN_JAR="target/jpackage/WMS.app/Contents/app/wms-client.jar"
[ -f "$MAIN_JAR" ] || MAIN_JAR="target/jpackage/WMS/app/wms-client.jar"
echo ""
echo "打包完成：$(dirname "$(dirname "$MAIN_JAR")")"
echo "主 jar MD5（发版填 application.yaml → wms-update.md5）："
if command -v md5 >/dev/null; then md5 -q "$MAIN_JAR"; else certutil -hashfile "$MAIN_JAR" MD5 | head -2 | tail -1; fi
