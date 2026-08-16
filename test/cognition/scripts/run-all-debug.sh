#!/usr/bin/env bash
# =============================================================================
# 全部模板 × 算子 × 场景 debug 测试（依赖修复后的 debug-cognition.sh）
#
# 用法:
#   ./run-all-debug.sh                     # 默认 L3, localhost:8080
#   ./run-all-debug.sh --depth L2 --host http://localhost:8080
#
# 前置: boot 运行 + seed 已应用（entity=32/relation=41/index=82）
# 输出: 每个场景的请求行 + 完整原始响应 + traceId（用 sed 截取避免刷屏）
# =============================================================================

set -u

HOST="http://localhost:8080"
DEPTH="L3"
SC_MF='{"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]}'

while [ $# -gt 0 ]; do
  case "$1" in
    --depth) DEPTH="${2:-L3}"; shift 2 ;;
    --host) HOST="${2:-http://localhost:8080}"; shift 2 ;;
    *) shift ;;
  esac
done

run() {  # run <label> <template> <entity> [params] [scope]
  echo ""
  echo "################################################################"
  echo "### $1"
  echo "################################################################"
  HOST="$HOST" DEPTH="$DEPTH" ./debug-cognition.sh "$2" "$3" "${4:-}" "${5:-}" \
    | sed -n '1,3p;/===== 完整原始响应/,/===== traceId/p'
}

B=metaforge:1.0.0.agent

## ---------- DISCOVER ----------
run "DISCOVER 默认(无锚点)" DISCOVER "" '{}'
run "DISCOVER bundle-discovery" DISCOVER "" '{"selectOperators":["ontological.bundle-discovery"]}'
run "DISCOVER package-explorer" DISCOVER "" '{"selectOperators":["ontological.package-explorer"],"parent_fqn":"metaforge:1.0.0"}'
run "DISCOVER entity-schema-inventory" DISCOVER "" '{"selectOperators":["ontological.entity-schema-inventory"],"parent_fqn":"metaforge:1.0.0.agent"}'
run "DISCOVER relation-schema-inventory" DISCOVER "" '{"selectOperators":["ontological.relation-schema-inventory"],"parent_fqn":"metaforge:1.0.0.agent"}'
run "DISCOVER selectOperators 无效(期望34014)" DISCOVER "" '{"selectOperators":["ontological.unknown"]}'
####
##### ---------- ORIENT ----------
run "ORIENT 顶层自动发现" ORIENT "" '{}'
run "ORIENT L1分组下钻 Group_Fulfillment" ORIENT "" '{"parent_fqn":"metaforge:1.0.0.common.Group_Fulfillment"}'
run "ORIENT 域下钻 Domain_Inventory(Agent+Task)" ORIENT "" '{"parent_fqn":"metaforge:1.0.0.common.Domain_Inventory"}'
run "ORIENT level=L2" ORIENT "" '{"level":"L2"}'
run "ORIENT level=Task" ORIENT "" '{"level":"Task"}'
run "ORIENT level=Agent" ORIENT "" '{"level":"Agent"}'
run "ORIENT level=UNKNOWN(期望34013)" ORIENT "" '{"level":"UNKNOWN"}'
####
##### ---------- BRIEF ----------
run "BRIEF 全景 Task_InventoryCheck" BRIEF "$B.Task_InventoryCheck"
run "BRIEF entity-profile" BRIEF "$B.Task_InventoryCheck" "{\"entity_fqn\":\"$B.Task_InventoryCheck\",\"selectOperators\":[\"ontological.entity-profile\"]}"
run "BRIEF flow-blueprint" BRIEF "$B.Task_InventoryCheck" "{\"entity_fqn\":\"$B.Task_InventoryCheck\",\"selectOperators\":[\"procedural.flow-blueprint\"]}"
run "BRIEF adjacent-step" BRIEF "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"selectOperators\":[\"procedural.adjacent-step\"]}"
run "BRIEF rule-listing(VerifyStock)" BRIEF "$B.Step_VerifyStock" "{\"entity_fqn\":\"$B.Step_VerifyStock\",\"selectOperators\":[\"deontic.rule-listing\"]}"
run "BRIEF tool-discovery(Task)" BRIEF "$B.Task_InventoryCheck" "{\"entity_fqn\":\"$B.Task_InventoryCheck\",\"selectOperators\":[\"capability.tool-discovery\"]}"
run "BRIEF direct-link(Step)" BRIEF "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"selectOperators\":[\"relational.direct-link\"]}"
####
##### ---------- GUIDE ----------
run "GUIDE 全景 Step_CheckInventory" GUIDE "$B.Step_CheckInventory"
run "GUIDE rule-listing(VerifyStock)" GUIDE "$B.Step_VerifyStock" "{\"entity_fqn\":\"$B.Step_VerifyStock\",\"selectOperators\":[\"deontic.rule-listing\"]}"
run "GUIDE tool-discovery(Step)" GUIDE "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"selectOperators\":[\"capability.tool-discovery\"]}"
run "GUIDE protocol-detail(Cap)" GUIDE "$B.Cap_InventoryAPI" "{\"entity_fqn\":\"$B.Cap_InventoryAPI\",\"selectOperators\":[\"capability.protocol-detail\"]}"
run "GUIDE adjacent-step(Step)" GUIDE "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"selectOperators\":[\"procedural.adjacent-step\"]}"
run "GUIDE decision-branch(VerifyStock)" GUIDE "$B.Step_VerifyStock" "{\"entity_fqn\":\"$B.Step_VerifyStock\",\"selectOperators\":[\"procedural.decision-branch\"]}"
run "GUIDE direct-link(Step)" GUIDE "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"selectOperators\":[\"relational.direct-link\"]}"
####
##### ---------- FORECAST ----------
run "FORECAST 全景 Step_CheckInventory" FORECAST "$B.Step_CheckInventory"
run "FORECAST impact-forward" FORECAST "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"max_depth\":3,\"selectOperators\":[\"relational.impact-forward\"]}"
run "FORECAST impact-backward" FORECAST "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"max_depth\":3,\"selectOperators\":[\"relational.impact-backward\"]}"
run "FORECAST risk-assessment" FORECAST "$B.Task_DelegationDemo" "{\"entity_fqn\":\"$B.Task_DelegationDemo\",\"selectOperators\":[\"relational.risk-assessment\"]}"
run "FORECAST constraint-check" FORECAST "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"change_type\":\"MODIFY\",\"selectOperators\":[\"deontic.constraint-check\"]}"
run "FORECAST regression-scope" FORECAST "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"selectOperators\":[\"capability.regression-scope\"]}"
run "FORECAST neighborhood" FORECAST "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"selectOperators\":[\"relational.neighborhood\"]}"
###
##### ---------- DELEGATE ----------
run "DELEGATE 无scope(期望34005)" DELEGATE "$B.Step_CheckInventory"
run "DELEGATE scope-narrowing" DELEGATE "$B.Step_CheckInventory" "{\"entity_fqn\":\"$B.Step_CheckInventory\",\"selectOperators\":[\"governance.scope-narrowing\"]}" "$SC_MF"

echo ""
echo "全部场景执行完毕。"
