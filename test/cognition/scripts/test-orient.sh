#!/usr/bin/env bash
# =============================================================================
# ORIENT 模板（业务域定位）curl 测试脚本
#
# 用法: ./test-orient.sh [--depth L3|L2] [--host http://localhost:8080]
#
# 覆盖场景:
#   1. 顶层自动发现（无 level，按 entity_type 分组）
#   2. L1 分组下钻 -> L2 域（SubjectDomainGroupContainsSubjectDomain）
#   3. 域下钻 -> Agent + Task（SubjectDomainComposesAgent/Task）
#   4. level=L2 别名精确过滤
#   5. level=Task 别名精确过滤
#   6. level=Agent 别名精确过滤
#   7. 未知 level -> 期望 34013
#   8. 跨 Bundle FQN（seed 无数据，返回空）
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
API="$HOST/api/v1/cognition/ORIENT"

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
        dd = op.get("data", {})
        grouped = dd.get("children_grouped", {})
        print("  level:", dd.get("level"))
        for t, items in grouped.items():
            print("    %s -> %s" % (t, [i.get("fqn") for i in items]))
'
}

echo "ORIENT 测试 - depth=$DEPTH host=$HOST"

request "1. 顶层自动发现" '{}'
request "2. L1 分组下钻 Group_Fulfillment" '{"parent_fqn":"metaforge:1.0.0.common.Group_Fulfillment"}'
request "3. 域下钻 Domain_Inventory" '{"parent_fqn":"metaforge:1.0.0.common.Domain_Inventory"}'
request "4. level=L2 别名 (Group_Fulfillment 下钻)" '{"parent_fqn":"metaforge:1.0.0.common.Group_Fulfillment","level":"L2"}'
request "5. level=Task 别名" '{"level":"Task"}'
request "6. level=Agent 别名" '{"level":"Agent"}'
request "7. 未知 level (期望 34013)" '{"level":"UNKNOWN"}'
request "8. 跨 Bundle codebase (无数据)" '{"parent_fqn":"codebase:1.0.0.Module_Auth","level":"codebase:1.0.0.structure.Class"}'

echo ""
echo "完成。"
