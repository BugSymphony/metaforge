#!/usr/bin/env bash
# ============================================================================
# MetaForge DSH 插件 —— 开发环境准备
#
# 从已安装的 DeepSeek Harness 链接共享运行时（@deepseek-ai/*）到本地 node_modules，
# 供 tsc typecheck/build 使用。运行时由 dsh profile 提供这些 peer 依赖。
#
# 用法: bash setup.sh [DSH_NODE_MODULES]
#   DSH_NODE_MODULES 默认自动检测全局 dsh 安装，可显式指定
# ============================================================================
set -euo pipefail

# 定位 dsh 安装的 node_modules/@deepseek-ai
if [ -n "${DSH_NODE_MODULES:-}" ]; then
  SRC="$DSH_NODE_MODULES"
else
  # 全局 dsh 安装
  GLOBAL_DSS=$(npm root -g 2>/dev/null || echo "")
  SRC=""
  if [ -n "$GLOBAL_DSS" ]; then
    for cand in \
      "$GLOBAL_DSS/@deepseek-ai/dsh/node_modules/@deepseek-ai" \
      "$GLOBAL_DSS/@deepseek-ai"; do
      if [ -d "$cand/dsh-tools" ]; then SRC="$cand"; break; fi
    done
  fi
fi

if [ -z "$SRC" ] || [ ! -d "$SRC/dsh-tools" ]; then
  echo "[错误] 未找到 dsh 安装的 @deepseek-ai 包。请安装 dsh 或设置 DSH_NODE_MODULES。" >&2
  exit 1
fi
echo "使用 @deepseek-ai 源: $SRC"

# 安装 typescript（仅 typescript，跳过 peer 自动安装避免拉取未发布的 @deepseek-ai/*）
if [ ! -d node_modules/typescript ]; then
  npm install --no-save --legacy-peer-deps --fetch-timeout=30000 typescript >/dev/null 2>&1 || true
fi

# 链接共享运行时
mkdir -p node_modules/@deepseek-ai
for p in dsh-tools schemastery cordis; do
  ln -sfn "$SRC/$p" "node_modules/@deepseek-ai/$p"
  echo "链接 @deepseek-ai/$p -> $SRC/$p"
done

echo "开发环境就绪。运行: npm run typecheck / npm run build"
