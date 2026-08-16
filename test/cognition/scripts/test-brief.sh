#!/usr/bin/env bash
# =============================================================================
# BRIEF 模板（任务/实体全景）curl 测试脚本
#
# 用法:
#   ./test-brief.sh [--depth L3|L2] [--host http://localhost:8080]
#
# 覆盖场景:
#   1. Task_InventoryCheck 全景（全部算子）
#   2. Step_CheckInventory 全景
#   3. flow-blueprint 单独（Task 入口步解析）
#   4. tool-discovery 单独（StepUsesCapability 分层摘要）
#   5. rule-listing 单独（Step_VerifyStock 命中 RuleAppliesTo）
#   6. adjacent-step 单独
#   7. direct-link 单独
#   8. selectOperators 无效 -> 期望 34014
#   9. 未知实体 + scope 限定 -> 期望 34004
#   10. 未知实体 + 空 scope -> 当前返回 10000（无明确错误码）
# =============================================================================

set -u

HOST="http://localhost:8080"
DEPTH="L3"
API="$HOST/api/v1/cognition/BRIEF"
CONTENT_TYPE="Content-Type: application/json"
SCOPE='{"bundles":[],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]}'

# 解析参数
while [ $# -gt 0 ]; do
  case "$1" in
    --depth) DEPTH="${2:-L3}"; shift 2 ;;
    --host) HOST="${2:-http://localhost:8080}"; shift 2 ;;
    *) shift ;;
  esac
done
API="$HOST/api/v1/cognition/BRIEF"

# 发送请求并美化输出（python3 做 JSON 提取，无需 jq）
# 用法: request <label> <params_json> [scope_json]
request() {
  local label="$1" params="$2" scope="${3:-$SCOPE}"
  local body
  body=$(printf '{"scope":%s,"params":%s,"format":"JSON","cognitionDepth":"%s","agentArchetype":"EXECUTION","maxTokens":8000}' \
    "$scope" "$params" "$DEPTH")

  echo ""
  echo "===== $label ====="
  curl -s -X POST "$API" -H "$CONTENT_TYPE" -d "$body" | python3 -c '
import sys, json
d = json.load(sys.stdin)
code = d.get("code")
if code != 200:
    print("  code = %s | %s" % (code, d.get("message")))
    sys.exit(0)
data = d["data"]
dims = data.get("dimensions", {})
for op in dims:
        oid = op.get("operatorId", "")
        dd = op.get("data")
        print("  [%s] success=%s" % (oid, op.get("success")))
        if isinstance(dd, dict):
            for k, v in list(dd.items())[:6]:
                if isinstance(v, list):
                    print("      %s: list(%d)" % (k, len(v)))
                    for item in v[:2]:
                        print("        - %s" % json.dumps(item, ensure_ascii=False)[:160])
                elif isinstance(v, dict):
                    print("      %s: %s" % (k, json.dumps(v, ensure_ascii=False)[:160]))
                else:
                    print("      %s: %s" % (k, v))
        elif isinstance(dd, list):
            for item in dd[:3]:
                print("      - %s" % json.dumps(item, ensure_ascii=False)[:160])
'
}

echo "BRIEF 测试 - depth=$DEPTH host=$HOST"

request "1. Task_InventoryCheck 全景" '{"entity_fqn":"metaforge:1.0.0.agent.Task_InventoryCheck"}'
request "2. Step_CheckInventory 全景" '{"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory"}'
request "3. flow-blueprint (Task 入口步解析)" '{"selectOperators":["procedural.flow-blueprint"],"entity_fqn":"metaforge:1.0.0.agent.Task_InventoryCheck"}'
request "4. tool-discovery (StepUsesCapability)" '{"selectOperators":["capability.tool-discovery"],"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory"}'
request "5. rule-listing (Step_VerifyStock -> RuleAppliesTo)" '{"selectOperators":["deontic.rule-listing"],"entity_fqn":"metaforge:1.0.0.agent.Step_VerifyStock"}'
request "6. adjacent-step (Step_CheckInventory)" '{"selectOperators":["procedural.adjacent-step"],"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory"}'
request "7. direct-link (Step_CheckInventory)" '{"selectOperators":["relational.direct-link"],"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory"}'
request "8. selectOperators 无效 (期望 34014)" '{"selectOperators":["ontological.unknown"],"entity_fqn":"metaforge:1.0.0.agent.Task_InventoryCheck"}'
request "9. 未知实体 + scope 限定 (期望 34004)" '{"entity_fqn":"unknown:entity"}' '{"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]}'
request "10. 未知实体 + 空 scope (当前 10000)" '{"entity_fqn":"metaforge:1.0.0.agent.NoSuchAgent"}'

echo ""
echo "完成。"
