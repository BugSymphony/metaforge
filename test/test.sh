#!/usr/bin/env bash
# ============================================================================
# 单元测试脚本: 运行 metaforge-parent 的 Maven 测试
# 用法:
#   bash test.sh                       # 跑全部测试
#   bash test.sh -pl metaforge-metamodel/metaforge-metamodel-core test   # 指定模块
#   bash test.sh -Dtest=FqnGeneratorTest test                            # 指定用例
# ============================================================================
set -eu

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/metaforge-parent"

echo ">> mvn ${*:-test} (module: metaforge-parent)"
mvn "${*:-test}"
