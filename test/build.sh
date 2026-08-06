#!/usr/bin/env bash
# ============================================================================
# 开发编译脚本: 编译 metaforge-parent Maven 多模块项目
# 用法:
#   bash build.sh                # 默认: clean package -DskipTests
#   bash build.sh compile        # 仅编译
#   bash build.sh test           # 编译并跑测试
#   bash build.sh package        # 打包
# ============================================================================
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/metaforge-parent"

echo ">> mvn ${*:-clean package -DskipTests} (module: metaforge-parent)"
mvn "${@:-clean package -DskipTests}"
