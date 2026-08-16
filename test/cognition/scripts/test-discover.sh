#!/usr/bin/env bash
# =============================================================================
# DISCOVER 模板（元模型发现）curl 测试脚本
#
# 用法: ./test-discover.sh [--depth L3|L2] [--host http://localhost:8080]
#
# 覆盖场景:
#   1. bundle-discovery（全平台 Bundle 列表）
#   2. package-explorer（Bundle=metaforge:1.0.0 下的 3 个 Package）
#   3. entity-schema-inventory（Package=agent 的实体类型清单）
#   4. relation-schema-inventory（Package=agent 的关系类型清单）
#   5. selectOperators 无效 -> 期望 34014
#   6. 默认全部算子（无 selectOperators）
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
API="$HOST/api/v1/cognition/DISCOVER"

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
        print("  [%s]" % op.get("operatorId"))
        if isinstance(dd, list):
            for n in dd:
                inner = n.get("data", {})
                print("    - %s | has_children=%s | next=%s | instance_count=%s" % (
                    inner.get("fqn") or inner.get("schema", {}).get("fqn"),
                    n.get("has_children"), n.get("suggested_next_call"), n.get("instance_count")))
'
}

echo "DISCOVER 测试 - depth=$DEPTH host=$HOST"

request "1. bundle-discovery" '{"selectOperators":["ontological.bundle-discovery"]}'
request "2. package-explorer (Bundle=metaforge:1.0.0)" '{"selectOperators":["ontological.package-explorer"],"parent_fqn":"metaforge:1.0.0"}'
request "3. entity-schema-inventory (Package=agent)" '{"selectOperators":["ontological.entity-schema-inventory"],"parent_fqn":"metaforge:1.0.0.agent"}'
request "4. relation-schema-inventory (Package=agent)" '{"selectOperators":["ontological.relation-schema-inventory"],"parent_fqn":"metaforge:1.0.0.agent"}'
request "5. selectOperators 无效 (期望 34014)" '{"selectOperators":["ontological.unknown"]}'
request "6. 默认全部算子" '{"parent_fqn":"metaforge:1.0.0"}'

echo ""
echo "完成。"
