#!/usr/bin/env bash
# =============================================================================
# 认知查询 debug 脚本——输出完整原始响应 + traceId 关联 boot 日志
#
# 用法:
#   ./debug-cognition.sh <templateId> <entity_fqn> [params_json] [scope_json]
#   ./debug-cognition.sh GUIDE metaforge:1.0.0.agent.Step_CheckInventory
#   ./debug-cognition.sh DELEGATE metaforge:1.0.0.agent.Step_CheckInventory '{"task_fqn":"x"}' '{"bundles":["metaforge:1.0.0"]}'
#
# 可选环境变量:
#   HOST      默认 http://localhost:8080
#   DEPTH     默认 L3
#   BOOT_LOG  boot 日志路径，用于 traceId 关联查询（默认自动查找）
# =============================================================================

set -u

HOST="${HOST:-http://localhost:8080}"
DEPTH="${DEPTH:-L3}"
TPL="${1:?用法: debug-cognition.sh <templateId> [entity_fqn] [params_json] [scope_json]}"
ENTITY="${2:-}"
PARAMS="${3:-}"
if [ $# -ge 4 ] && [ -n "$4" ]; then
  SCOPE="$4"
else
  SCOPE='{"bundles":[],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]}'
fi

# 构造 params：
#  - 缺省 params：entity_fqn 非空 -> {"entity_fqn":"X"}；空 -> {}
#  - 显式 params 含 entity_fqn：不动
#  - 显式 {} 且 entity_fqn 非空：补 entity_fqn
#  - 其他 params：entity_fqn 非空时补 entity_fqn 前缀
case "$PARAMS" in
  '')
    if [ -n "$ENTITY" ]; then PARAMS=$(printf '{"entity_fqn":"%s"}' "$ENTITY"); else PARAMS='{}'; fi
    ;;
  *entity_fqn*) ;;
  '{}')
    if [ -n "$ENTITY" ]; then PARAMS=$(printf '{"entity_fqn":"%s"}' "$ENTITY"); fi
    ;;
  *)
    if [ -n "$ENTITY" ]; then PARAMS=$(printf '{"entity_fqn":"%s",%s' "$ENTITY" "${PARAMS#\{}"); fi
    ;;
esac

BODY=$(printf '{"scope":%s,"params":%s,"format":"JSON","cognitionDepth":"%s","agentArchetype":"EXECUTION","maxTokens":8000}' \
  "$SCOPE" "$PARAMS" "$DEPTH")

echo ">>> 请求: POST $HOST/api/v1/cognition/$TPL"
echo ">>> body: $BODY"
echo ""

RAW=$(curl -s -X POST "$HOST/api/v1/cognition/$TPL" -H 'Content-Type: application/json' -d "$BODY")

# 1) 完整原始响应（不截断）
echo "===== 完整原始响应 ====="
echo "$RAW" | python3 -m json.tool --no-ensure-ascii 2>/dev/null || echo "$RAW"

# 2) traceId + 关键摘要
TRACE=$(echo "$RAW" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("traceId",""))' 2>/dev/null)
echo ""
echo "===== traceId: $TRACE ====="

# 3) 关联 boot 日志（traceId 或算子失败信息）
if [ -n "$TRACE" ]; then
  BOOT_LOG="${BOOT_LOG:-}"
  if [ -z "$BOOT_LOG" ]; then
    # 自动查找最近的 boot 日志
    BOOT_LOG=$(ls -t /tmp/opencode/boot*.log 2>/dev/null | head -1)
  fi
  if [ -n "$BOOT_LOG" ] && [ -f "$BOOT_LOG" ]; then
    echo ""
    echo "===== boot 日志关联 (traceId=$TRACE) — $BOOT_LOG ====="
    grep "$TRACE" "$BOOT_LOG" | tail -30 || echo "  (日志中未找到该 traceId)"
    echo ""
    echo "===== 最近错误/异常 ====="
    grep -iE "exception|error|required 算子执行失败|caused by" "$BOOT_LOG" | tail -10 || echo "  (无错误)"
  else
    echo ""
    echo "未找到 boot 日志，可用: BOOT_LOG=/path/to/boot.log $0 ..."
  fi
fi
