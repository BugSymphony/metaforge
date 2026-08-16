#!/usr/bin/env bash
# =============================================================================
# FORECAST 模板（变更影响链路）curl 测试脚本
#
# 用法: ./test-forecast.sh [--depth L3|L2] [--host http://localhost:8080]
#
# 覆盖场景:
#   1. 全景（neighborhood + impact-forward + impact-backward + risk + constraint + rule + regression）
#   2. impact-forward 单独（正向影响扩散）
#   3. impact-backward 单独（反向依赖溯源）
#   4. risk-assessment（风险评级，高影响链锚点）
#   5. constraint-check（规则冲突检测）
#   6. regression-scope（回归范围建议）
#   7. max_depth 参数透传
# =============================================================================

set -u

HOST="http://localhost:8080"
DEPTH="L3"
SCOPE='{"bundles":[],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]}'

while [ $# -gt 0 ]; do
  case "$1" in
    --depth) DEPTH="${2:-L3}"; shift 2 ;;
    --host) HOST="${2:-http://localhost:8080}"; shift 2 ;;
    *) shift ;;
  esac
done
API="$HOST/api/v1/cognition/FORECAST"

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
for op in d["data"]["dimensions"]:
        dd = op.get("data")
        print("  [%s] success=%s keys=%s" % (op.get("operatorId"), op.get("success"), list(dd.keys()) if isinstance(dd, dict) else type(dd).__name__))
        if isinstance(dd, dict):
            for k, v in dd.items():
                if isinstance(v, list):
                    print("      %s: list(%d)" % (k, len(v)))
                elif isinstance(v, dict):
                    print("      %s: %s" % (k, json.dumps(v, ensure_ascii=False)[:120]))
                else:
                    print("      %s: %s" % (k, v))
'
}

echo "FORECAST 测试 - depth=$DEPTH host=$HOST"

request "1. 全景 (Step_CheckInventory)" '{"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory","max_depth":3}'
request "2. impact-forward 单独" '{"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory","max_depth":3,"selectOperators":["relational.impact-forward"]}'
request "3. impact-backward 单独" '{"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory","max_depth":3,"selectOperators":["relational.impact-backward"]}'
request "4. risk-assessment (Task_DelegationDemo)" '{"entity_fqn":"metaforge:1.0.0.agent.Task_DelegationDemo","selectOperators":["relational.risk-assessment"]}'
request "5. constraint-check (Step_CheckInventory)" '{"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory","change_type":"MODIFY","selectOperators":["deontic.constraint-check"]}'
request "6. regression-scope (Step_CheckInventory)" '{"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory","selectOperators":["capability.regression-scope"]}'
request "7. max_depth=2 (Rule_InventoryAboveZero)" '{"entity_fqn":"metaforge:1.0.0.agent.Rule_InventoryAboveZero","max_depth":2}'

echo ""
echo "完成。"
