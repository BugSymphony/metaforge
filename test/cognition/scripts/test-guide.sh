#!/usr/bin/env bash
# =============================================================================
# GUIDE 模板（单步执行指南）curl 测试脚本
#
# 用法: ./test-guide.sh [--depth L3|L2] [--host http://localhost:8080]
#
# 覆盖场景:
#   1. Step_CheckInventory 全景（全部算子）
#   2. protocol-detail（Cap_InventoryAPI -> CapabilityImplementsHttp 前缀查询）
#   3. rule-listing（Step_VerifyStock -> RuleAppliesTo）
#   4. decision-branch（Step_VerifyStock DECISION 类型）
#   5. entity-profile（Rule_InventoryAboveZero）
#   6. adjacent-step（Step_CheckInventory）
#   7. tool-discovery（Step_CheckInventory -> StepUsesCapability）
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
API="$HOST/api/v1/cognition/GUIDE"

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
        print("  [%s] success=%s" % (op.get("operatorId"), op.get("success")))
        if isinstance(dd, dict):
            for k, v in list(dd.items())[:5]:
                if isinstance(v, list):
                    print("      %s: list(%d)" % (k, len(v)))
                    for item in v[:2]:
                        print("        - %s" % json.dumps(item, ensure_ascii=False)[:160])
                else:
                    print("      %s: %s" % (k, v))
'
}

echo "GUIDE 测试 - depth=$DEPTH host=$HOST"

request "1. Step_CheckInventory 全景" '{"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory"}'
request "2. protocol-detail (Cap_InventoryAPI 前缀查询)" '{"selectOperators":["capability.protocol-detail"],"entity_fqn":"metaforge:1.0.0.agent.Cap_InventoryAPI"}'
request "3. rule-listing (Step_VerifyStock)" '{"selectOperators":["deontic.rule-listing"],"entity_fqn":"metaforge:1.0.0.agent.Step_VerifyStock"}'
request "4. decision-branch (Step_VerifyStock)" '{"selectOperators":["procedural.decision-branch"],"entity_fqn":"metaforge:1.0.0.agent.Step_VerifyStock"}'
request "5. entity-profile (Rule_InventoryAboveZero)" '{"selectOperators":["ontological.entity-profile"],"entity_fqn":"metaforge:1.0.0.agent.Rule_InventoryAboveZero"}'
request "6. adjacent-step (Step_CheckInventory)" '{"selectOperators":["procedural.adjacent-step"],"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory"}'
request "7. tool-discovery (Step_CheckInventory)" '{"selectOperators":["capability.tool-discovery"],"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory"}'

echo ""
echo "完成。"
