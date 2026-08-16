#!/usr/bin/env bash
# =============================================================================
# DELEGATE 模板（子任务上下文委派）curl 测试脚本
#
# 用法: ./test-delegate.sh [--depth L3|L2] [--host http://localhost:8080]
#
# 覆盖场景:
#   1. 不传 scope -> 期望 34005（scope 必填）
#   2. Step_CheckInventory 委派（scope 收窄三层 + delegated updated_scope）
#   3. scope-narrowing 单独（蓝图/实体/Schema 三层收窄）
#   4. Task_InventoryCheck 委派（含 scope）
# =============================================================================

set -u

HOST="http://localhost:8080"
DEPTH="L3"
SCOPE='{"bundles":[],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]}'
DELEGATE_SCOPE='{"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]}'

while [ $# -gt 0 ]; do
  case "$1" in
    --depth) DEPTH="${2:-L3}"; shift 2 ;;
    --host) HOST="${2:-http://localhost:8080}"; shift 2 ;;
    *) shift ;;
  esac
done
API="$HOST/api/v1/cognition/DELEGATE"

request() {
  local label="$1" params="$2" scope="${3:-$SCOPE}"
  local body
  body=$(printf '{"scope":%s,"params":%s,"format":"JSON","cognitionDepth":"%s","agentArchetype":"EXECUTION","maxTokens":8000}' \
    "$scope" "$params" "$DEPTH")
  echo ""
  echo "===== $label ====="
  curl -s -X POST "$API" -H 'Content-Type: application/json' -d "$body" | python3 -c '
import sys, json
d = json.load(sys.stdin)
if d.get("code") != 200:
    print("  code = %s | %s" % (d.get("code"), d.get("message"))); sys.exit(0)
data = d["data"]
print("  updatedScope:", json.dumps(data.get("updatedScope"), ensure_ascii=False))
for op in data["dimensions"]:
        dd = op.get("data")
        print("  [%s] success=%s" % (op.get("operatorId"), op.get("success")))
        if isinstance(dd, dict):
            for k, v in list(dd.items())[:6]:
                if isinstance(v, list):
                    print("      %s: list(%d)" % (k, len(v)))
                    for item in v[:3]:
                        print("        - %s" % json.dumps(item, ensure_ascii=False)[:150])
                else:
                    print("      %s: %s" % (k, v))
'
}

echo "DELEGATE 测试 - depth=$DEPTH host=$HOST"

request "1. 不传 scope (期望 34005)" '{}'
request "2. Step_CheckInventory 委派 (含 scope)" '{"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory"}' "$DELEGATE_SCOPE"
request "3. scope-narrowing 单独" '{"selectOperators":["governance.scope-narrowing"],"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory"}' "$DELEGATE_SCOPE"
request "4. Task_InventoryCheck 委派 (含 scope)" '{"entity_fqn":"metaforge:1.0.0.agent.Task_InventoryCheck"}' "$DELEGATE_SCOPE"

echo ""
echo "完成。"
