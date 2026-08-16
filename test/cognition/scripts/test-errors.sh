#!/usr/bin/env bash
# =============================================================================
# 认知引擎报错场景测试脚本——覆盖常见错误码
#
# 用法:
#   ./test-errors.sh [--host http://localhost:8080]
#
# 覆盖错误码:
#   34001 TEMPLATE_NOT_FOUND       模板不存在
#   34002 TEMPLATE_INVALID         模板定义不合法（需外部注入，默认跳过）
#   34003 INVALID_SCOPE            scope 中 bundle 不存在
#   34004 ENTITY_OUT_OF_SCOPE      entityFqn 不在 scope 内
#   34005 MISSING_SCOPE            DELEGATE 必须传 scope
#   34010 INVALID_FORMAT           format 非法
#   34012 ARCHETYPE_NOT_SUPPORTED  archetype 不被模板支持
#   34013 INVALID_LEVEL            ORIENT level 无法解析
#   34014 INVALID_OPERATOR_SELECTION selectOperators 无匹配
#   34008 UPSTREAM_UNAVAILABLE     上游不可用（难以外部触发，默认跳过）
# =============================================================================

set -u
HOST="${1:-http://localhost:8080}"
API="$HOST/api/v1/cognition"
CT="Content-Type: application/json"
SCOPE_EMPTY='{"bundles":[],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]}'

# 发送请求，断言期望错误码
# 用法: expect <期望错误码> <模板> <body_json>
expect() {
  local want="$1" tpl="$2" body="$3"
  local resp
  resp=$(curl -s -X POST "$API/$tpl" -H "$CT" -d "$body")
  local code
  code=$(echo "$resp" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code"))')
  local msg
  msg=$(echo "$resp" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("message"))')
  local trace
  trace=$(echo "$resp" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("traceId",""))')
  if [ "$code" = "$want" ]; then
    printf "  [PASS] code=%s msg=%s traceId=%s\n" "$code" "$msg" "$trace"
  else
    printf "  [FAIL] 期望 %s 实际 %s msg=%s\n" "$want" "$code" "$msg"
  fi
}

echo "===== 报错场景测试 host=$HOST ====="

echo "[34001] 模板不存在 TEMPLATE_NOT_FOUND"
expect 34001 NO_SUCH_TEMPLATE "{\"scope\":$SCOPE_EMPTY,\"params\":{},\"format\":\"JSON\"}"

echo "[34003] scope.bundles 不存在 INVALID_SCOPE"
expect 34003 DISCOVER "{\"scope\":{\"bundles\":[\"no-such-bundle\"],\"packages\":[],\"domainGroups\":[],\"domains\":[],\"entitySchemas\":[]},\"params\":{},\"format\":\"JSON\"}"

echo "[34004] entityFqn 不在 scope 内 ENTITY_OUT_OF_SCOPE"
expect 34004 BRIEF "{\"scope\":{\"bundles\":[\"metaforge:1.0.0\"],\"packages\":[],\"domainGroups\":[],\"domains\":[],\"entitySchemas\":[]},\"params\":{\"entity_fqn\":\"unknown:entity\"},\"format\":\"JSON\"}"

echo "[34005] DELEGATE 缺 scope MISSING_SCOPE"
expect 34005 DELEGATE "{\"scope\":$SCOPE_EMPTY,\"params\":{\"entity_fqn\":\"metaforge:1.0.0.agent.Step_CheckInventory\"},\"format\":\"JSON\"}"

echo "[34010] format 非法 INVALID_FORMAT"
expect 34010 DISCOVER "{\"scope\":$SCOPE_EMPTY,\"params\":{},\"format\":\"XML\"}"

echo "[34012] archetype 不被模板支持 ARCHETYPE_NOT_SUPPORTED (ORIENT 仅支持 execution/exploration)"
expect 34012 ORIENT "{\"scope\":$SCOPE_EMPTY,\"params\":{},\"format\":\"JSON\",\"agentArchetype\":\"AUDIT\"}"

echo "[34013] ORIENT level 无法解析 INVALID_LEVEL"
expect 34013 ORIENT "{\"scope\":$SCOPE_EMPTY,\"params\":{\"level\":\"UNKNOWN\"},\"format\":\"JSON\"}"

echo "[34014] selectOperators 无匹配 INVALID_OPERATOR_SELECTION"
expect 34014 BRIEF "{\"scope\":$SCOPE_EMPTY,\"params\":{\"entity_fqn\":\"metaforge:1.0.0.agent.Task_InventoryCheck\",\"selectOperators\":[\"ontological.unknown\"]},\"format\":\"JSON\"}"

echo "[34008] 上游不可用（已跳过：需模拟上游故障）"
echo "   UPSTREAM_UNAVAILABLE(34008): 算子通过 executeWithPort 捕获上游异常返回 failure；"
echo "   仅当 required=true 且全部上游不可用时触发，建议用 mock/停止上游 BC 单独验证。"

echo "完成。"
