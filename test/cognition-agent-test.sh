#!/usr/bin/env bash
# ============================================================================
# agent 任务执行认知测试脚本 (cognition 层 · 业务 + agent 任务)
# 数据参考 V4__metamodel_governance_init.sql 的 metaforge Bundle 元模型:
#   agent 包 Task/ExecutionStep/Capability/ExecutionRule/DecisionRule/RiskPattern/Agent...
#   common 包 L1-L5 业务层级, protocol 包 Http/McpTool/LocalMethod
# 由 cognition-agent-seed.sql 提供 M1 实例 + 关系实例(幂等)。
#
# 用法:
#   ./cognition-agent-test.sh            # seed + 全部用例
#   ./cognition-agent-test.sh <pattern>  # seed + 匹配用例
#   ./cognition-agent-test.sh --no-seed [<pattern>]
#   ./cognition-agent-test.sh --seed-only
#   ./cognition-agent-test.sh --help
#   BASE_URL=... PGHOST=... PGPASSWORD=... ./cognition-agent-test.sh
#
# 依赖: curl, python3, psql(仅 seed)
# ============================================================================

set -u

# ---------- 参数解析 ----------
SEED=1
SEED_ONLY=0
FILTER=""
while [ $# -gt 0 ]; do
    case "$1" in
        --no-seed)   SEED=0; shift ;;
        --seed-only) SEED_ONLY=1; SEED=1; shift ;;
        --help|-h) sed -n '2,14p' "$0"; exit 0 ;;
        *) FILTER="$1"; shift ;;
    esac
done

BASE_URL="${BASE_URL:-http://localhost:8080}"
API="$BASE_URL/api/v1/cognition"
OUT_DIR="${TEST_OUT_DIR:-/tmp/cognition-agent-tests}"
SEED_SQL="${SEED_SQL:-$(dirname "$0")/cognition-agent-seed.sql}"
mkdir -p "$OUT_DIR"

PASS=0
FAIL=0
FAILED_CASES=()
J='Content-Type: application/json'

# ---------- 数据准备 ----------
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-metaforge}"
PGUSER="${PGUSER:-metaforge}"
PGPASSWORD="${PGPASSWORD:-metaforge}"

run_seed() {
    [ -f "$SEED_SQL" ] || { echo "  [FAIL] seed SQL 不存在: $SEED_SQL"; exit 1; }
    echo "  [INFO] 准备 agent 任务测试数据: $SEED_SQL"
    if ! PGPASSWORD="$PGPASSWORD" psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
            -v ON_ERROR_STOP=1 -f "$SEED_SQL" > /dev/null 2> /tmp/agent-seed.err; then
        echo "  [FAIL] 数据准备失败:"; tail -5 /tmp/agent-seed.err; exit 1
    fi
    echo "  [INFO] 数据准备完成"
}

if [ "$SEED" -eq 1 ]; then run_seed; fi
if [ "$SEED_ONLY" -eq 1 ]; then echo "  [INFO] --seed-only 完成"; exit 0; fi

# ---------- 基础工具 ----------
START_TS=$(date +%s)
should_run() { [ -z "${FILTER:-}" ] || echo "$1" | grep -q "$FILTER"; }
assert_ready() { [ -f "$1" ] && [ "$(stat -c %Y "$1" 2>/dev/null)" -ge "$START_TS" ]; }

run_case() {
    local name="$1" endpoint="$2" payload="$3"
    if ! should_run "$name"; then return; fi
    printf '\n\033[1;36m=== %s ===\033[0m\n' "$name"
    local out="$OUT_DIR/$(echo "$name" | tr '/ ' '__').json"
    curl -s -m 60 -X POST "$API$endpoint" -H "$J" -d "$payload" > "$out"
    local code
    code=$(python3 -c "import json;d=json.load(open('$out'));print(d.get('code','?'))" 2>/dev/null)
    echo "  code=$code (详见 $out)"
}

assert_ok() {
    local name="$1" file="$2" want="${3:-}"
    if ! assert_ready "$file"; then return; fi
    if [ -n "$want" ]; then
        python3 -c "
import json
d=json.load(open('$file'))
assert d.get('code')==200, 'code=%s %s' % (d.get('code'),d.get('message'))
got=set(d['data']['perspectives'].keys())
want=set('$want'.split(','))
assert want<=got, '缺少视角: %s (got %s)' % (want-got,got)
print('  [PASS]')
" 2>/dev/null && { PASS=$((PASS+1)); return; }
    else
        python3 -c "
import json
d=json.load(open('$file'))
assert d.get('code')==200, 'code=%s %s' % (d.get('code'),d.get('message'))
print('  [PASS]')
" 2>/dev/null && { PASS=$((PASS+1)); return; }
    fi
    echo "  [FAIL] $name"
    python3 -c "import json;d=json.load(open('$file'));print('  msg:',d.get('message'))" 2>/dev/null
    FAIL=$((FAIL+1)); FAILED_CASES+=("$name")
}

assert_err() {
    local name="$1" file="$2" want="$3"
    if ! assert_ready "$file"; then return; fi
    local got
    got=$(python3 -c "import json;d=json.load(open('$file'));print(d.get('code'))" 2>/dev/null)
    if [ "$got" = "$want" ]; then PASS=$((PASS+1)); echo "  [PASS] code=$got"; else
        echo "  [FAIL] $name 期望 $want 实际 $got"; FAIL=$((FAIL+1)); FAILED_CASES+=("$name"); fi
}

# 校验视角数据非空且含 name/description
check_nonempty() {
    local file="$1" key="$2"
    python3 -c "
import json
d=json.load(open('$file'))
assert d.get('code')==200
v=d['data']['perspectives']['$key']
def find(o):
    if isinstance(o,dict):
        if 'name' in o and o['name'] is None: raise AssertionError('缺 name')
        for x in o.values(): find(x)
    elif isinstance(o,list):
        for x in o: find(x)
find(v)
print('  [PASS] $key 数据完整')
" 2>/dev/null || return 1
}

# ============================================================================
# 数据集
# ============================================================================
META="metaforge:1.0.0"
STEP_CHECK="order:1.0.0.Step_CheckInventory"
STEP_PAY="order:1.0.0.Step_CheckPayment"
TASK_ORDER="metaforge:1.0.0.agent.Task_OrderFulfillment"
TASK_INV="metaforge:1.0.0.agent.Task_InventoryCheck"
AGENT_ORDER="metaforge:1.0.0.agent.Agent_OrderBot"
CAP_ORDER="order:1.0.0.Cap_OrderValidator"
RULE_INV="order:1.0.0.Rule_InventoryAboveZero"
DOMAIN_ORDER="metaforge:1.0.0.common.Domain_Order"

# ============================================================================
# 第一部分: agent 任务模板
# ============================================================================

echo; echo "======================================================"
echo " 第一部分: agent 任务模板 (task-brief / step-guide / sub-task / navigate)"
echo "======================================================"

run_case "A1 task-brief metaforge(L3)" "/task-brief" "{\"bundleFqns\":[\"$META\"],\"cognitionDepth\":\"L3\",\"maxTokens\":50000}"
assert_ok "A1" "$OUT_DIR/A1_task-brief_metaforge(L3).json" "capability_catalog,flow_blueprint,decision_matrix,constraint_set"

run_case "A2 step-guide ExecutionStep" "/step-guide" "{\"entityFqn\":\"$STEP_CHECK\",\"cognitionDepth\":\"L3\"}"
assert_ok "A2" "$OUT_DIR/A2_step-guide_ExecutionStep.json" "entity_profile,constraint_set,capability_catalog,decision_matrix,impact_trace,relationship_graph"

run_case "A3 step-guide 支付步骤" "/step-guide" "{\"entityFqn\":\"$STEP_PAY\",\"cognitionDepth\":\"L3\"}"
assert_ok "A3" "$OUT_DIR/A3_step-guide_支付步骤.json" "entity_profile,relationship_graph"

run_case "A4 step-guide Task 实体" "/step-guide" "{\"entityFqn\":\"$TASK_ORDER\",\"cognitionDepth\":\"L3\"}"
assert_ok "A4" "$OUT_DIR/A4_step-guide_Task_实体.json" "entity_profile,relationship_graph"

run_case "A5 sub-task INHERITED(库存校验)" "/sub-task-brief" "{\"entryEntityFqn\":\"$TASK_INV\",\"scopeMode\":\"INHERITED\",\"cognitionDepth\":\"L2\"}"
assert_ok "A5" "$OUT_DIR/A5_sub-task_INHERITED(库存校验).json" "entity_profile,constraint_set,capability_catalog,decision_matrix,prerequisite_chain,relationship_graph"

run_case "A6 sub-task PURE(履约任务)" "/sub-task-brief" "{\"entryEntityFqn\":\"$TASK_ORDER\",\"scopeMode\":\"PURE\"}"
assert_ok "A6" "$OUT_DIR/A6_sub-task_PURE(履约任务).json" "entity_profile"

run_case "A7 navigate 业务域" "/navigate" "{\"bundleFqns\":[\"$META\"],\"level\":\"L1\",\"expand\":\"lazy\"}"
assert_ok "A7" "$OUT_DIR/A7_navigate_业务域.json" "domain_navigation"

run_case "A8 prompt 格式(agent)" "/task-brief" "{\"bundleFqns\":[\"$META\"],\"format\":\"prompt\",\"cognitionDepth\":\"L2\"}"
if assert_ready "$OUT_DIR/A8_prompt_格式(agent).json"; then
python3 -c "
import json
d=json.load(open('$OUT_DIR/A8_prompt_格式(agent).json'))
sub=d.get('data') or {}
assert d.get('code')==200 and sub.get('format')=='prompt' and len(sub.get('content') or '')>0
print('  [PASS] prompt', len(sub['content']), 'chars')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] A8"; FAIL=$((FAIL+1)); FAILED_CASES+=("A8"); }
fi

# ============================================================================
# 第二部分: agent 任务视角逐一验证
# ============================================================================

echo; echo "======================================================"
echo " 第二部分: agent 任务视角逐一验证 (metaforge bundle)"
echo "======================================================"

# bundle 级: 能力/流程/决策/规则/实例/Schema/导航
for p in capability_catalog flow_blueprint decision_matrix constraint_set instance_catalog schema_inventory domain_navigation; do
  run_case "B-$p" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"perspectives\":[\"$p\"],\"cognitionDepth\":\"L3\"}"
  assert_ok "bundle 视角 $p" "$OUT_DIR/B-$p.json" "$p"
done

# 实体级: 组成/关系/影响/前置/定位 (以 Step_CheckInventory 为锚)
for p in composition_tree relationship_graph impact_trace prerequisite_chain domain_location; do
  run_case "E-$p" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"entityFqn\":\"$STEP_CHECK\",\"perspectives\":[\"$p\"],\"cognitionDepth\":\"L3\"}"
  assert_ok "实体视角 $p" "$OUT_DIR/E-$p.json" "$p"
done

# ============================================================================
# 第三部分: 业务 × agent 任务联动 (跨 Bundle)
# ============================================================================

echo; echo "======================================================"
echo " 第三部分: 业务 × agent 任务联动 (跨 Bundle)"
echo "======================================================"

run_case "C1 order+metaforge 联动" "/cognition-guidance" "{\"bundleFqns\":[\"order:1.0.0\",\"$META\"],\"perspectives\":[\"constraint_set\",\"flow_blueprint\",\"capability_catalog\",\"decision_matrix\"],\"cognitionDepth\":\"L3\"}"
python3 -c "
import json
d=json.load(open('$OUT_DIR/C1_order+metaforge_联动.json'))
assert d.get('code')==200
anchors=frozenset(a['bundleFqn'] for a in d['data']['contextMeta']['dataVersionAnchors'])
assert anchors>=frozenset({'order','metaforge'}), anchors
print('  [PASS] 跨 Bundle 锚点:', sorted(anchors))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] C1"; FAIL=$((FAIL+1)); FAILED_CASES+=("C1"); }

run_case "C2 order 业务任务" "/task-brief" "{\"bundleFqns\":[\"order:1.0.0\"],\"cognitionDepth\":\"L2\"}"
assert_ok "C2" "$OUT_DIR/C2_order_业务任务.json" "entity_profile,constraint_set"

run_case "C3 Task 组成结构" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"entityFqn\":\"$TASK_ORDER\",\"perspectives\":[\"composition_tree\",\"relationship_graph\"],\"cognitionDepth\":\"L3\"}"
assert_ok "C3" "$OUT_DIR/C3_Task_组成结构.json" "composition_tree,relationship_graph"

run_case "C4 Agent 关系图谱" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"entityFqn\":\"$AGENT_ORDER\",\"perspectives\":[\"relationship_graph\",\"entity_profile\"],\"cognitionDepth\":\"L3\"}"
assert_ok "C4" "$OUT_DIR/C4_Agent_关系图谱.json" "relationship_graph,entity_profile"

run_case "C5 Capability 实体画像" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"entityFqn\":\"$CAP_ORDER\",\"perspectives\":[\"entity_profile\",\"composition_tree\"],\"cognitionDepth\":\"L3\"}"
assert_ok "C5" "$OUT_DIR/C5_Capability_实体画像.json" "entity_profile,composition_tree"

run_case "C6 Rule 约束集" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"entityFqn\":\"$RULE_INV\",\"perspectives\":[\"constraint_set\",\"impact_trace\"],\"cognitionDepth\":\"L3\"}"
assert_ok "C6" "$OUT_DIR/C6_Rule_约束集.json" "constraint_set,impact_trace"

run_case "C7 业务域定位" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"entityFqn\":\"$DOMAIN_ORDER\",\"perspectives\":[\"domain_location\",\"domain_navigation\"],\"cognitionDepth\":\"L3\"}"
# domain_navigation 为 BUNDLE scope, 在 ENTITY_LEVEL 下会被跳过(设计行为)
python3 -c "
import json
d=json.load(open('$OUT_DIR/C7_业务域定位.json'))
assert d.get('code')==200
p=d['data']['perspectives']
assert 'domain_location' in p, '缺 domain_location'
assert 'domain_navigation' not in p, 'domain_navigation 不应出现在实体级'
assert 'domain_navigation' in (d['data']['contextMeta'].get('skippedPerspectives') or []), 'domain_navigation 应被跳过'
print('  [PASS] domain_location 生效, domain_navigation 正确跳过')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] C7"; FAIL=$((FAIL+1)); FAILED_CASES+=("C7"); }

run_case "C8 全 14 视角(agent)" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"entityFqn\":\"$STEP_CHECK\",\"perspectives\":[\"entity_profile\",\"domain_location\",\"composition_tree\",\"relationship_graph\",\"constraint_set\",\"capability_catalog\",\"flow_blueprint\",\"decision_matrix\",\"impact_trace\",\"prerequisite_chain\",\"domain_navigation\",\"instance_catalog\",\"bundle_directory\",\"schema_inventory\"],\"cognitionDepth\":\"L3\"}"
python3 -c "
import json
d=json.load(open('$OUT_DIR/C8_全_14_视角(agent).json'))
assert d.get('code')==200
cm=d['data']['contextMeta']
n=len(d['data']['perspectives'])
print('  [PASS] 应用视角数=%d skipped=%s' % (n, cm.get('skippedPerspectives')))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] C8"; FAIL=$((FAIL+1)); FAILED_CASES+=("C8"); }

# ============================================================================
# 第四部分: 数据完整性 (agent 元数据均含 name/description/属性)
# ============================================================================

echo; echo "======================================================"
echo " 第四部分: 数据完整性校验"
echo "======================================================"

run_case "V1 entity_profile(Step)" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"entityFqn\":\"$STEP_CHECK\",\"perspectives\":[\"entity_profile\"],\"cognitionDepth\":\"L3\"}"
python3 -c "
import json
d=json.load(open('$OUT_DIR/V1_entity_profile(Step).json'))
p=d['data']['perspectives']['entity_profile']
assert p.get('name') and p.get('description'), '缺 name/description'
assert p.get('content') is not None and len(p.get('content'))>0, '缺 content(属性)'
assert len(p.get('schemaAttributes') or [])>0, '缺 schemaAttributes'
print('  [PASS] %s | attrs=%d' % (p.get('name'), len(p['schemaAttributes'])))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] V1"; FAIL=$((FAIL+1)); FAILED_CASES+=("V1"); }

run_case "V2 capability_catalog" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"perspectives\":[\"capability_catalog\"],\"cognitionDepth\":\"L3\"}"
python3 -c "
import json
d=json.load(open('$OUT_DIR/V2_capability_catalog.json'))
items=d['data']['perspectives']['capability_catalog']['capabilities']
assert len(items)>0, '无能力'
for c in items:
    assert c.get('name') and c.get('description'), '能力缺 name/description'
    assert c.get('interfaceSpec') and c.get('callMethod'), '能力缺接口/调用方式'
print('  [PASS] %d 个能力均含 name/description/接口/调用方式' % len(items))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] V2"; FAIL=$((FAIL+1)); FAILED_CASES+=("V2"); }

run_case "V3 flow_blueprint" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"perspectives\":[\"flow_blueprint\"],\"cognitionDepth\":\"L3\"}"
python3 -c "
import json
d=json.load(open('$OUT_DIR/V3_flow_blueprint.json'))
steps=d['data']['perspectives']['flow_blueprint']['steps']
assert len(steps)>=2, '流程步骤过少'
for s in steps:
    assert s.get('name') and s.get('description'), '步骤缺 name/description'
    assert s.get('sequenceOrder')>0, '步骤缺顺序'
print('  [PASS] %d 个流程步骤均含 name/description/顺序' % len(steps))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] V3"; FAIL=$((FAIL+1)); FAILED_CASES+=("V3"); }

run_case "V4 decision_matrix" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"perspectives\":[\"decision_matrix\"],\"cognitionDepth\":\"L3\"}"
python3 -c "
import json
d=json.load(open('$OUT_DIR/V4_decision_matrix.json'))
pts=d['data']['perspectives']['decision_matrix']['decisionPoints']
assert len(pts)>0, '无决策点'
for p in pts:
    assert p.get('decisionName') and p.get('recommendation'), '决策点缺 name/推荐'
print('  [PASS] %d 个决策点均含 name/推荐' % len(pts))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] V4"; FAIL=$((FAIL+1)); FAILED_CASES+=("V4"); }

run_case "V5 instance_catalog(agent)" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"perspectives\":[\"instance_catalog\"],\"cognitionDepth\":\"L3\"}"
python3 -c "
import json
d=json.load(open('$OUT_DIR/V5_instance_catalog(agent).json'))
items=d['data']['perspectives']['instance_catalog']['entities']
assert len(items)>0, '无实例'
for e in items:
    assert e.get('name'), '实例缺 name: %s' % e.get('fqn')
    assert e.get('entitySchemaFqn'), '实例缺 schema: %s' % e.get('fqn')
print('  [PASS] %d 个 agent 实例均含 name/schema' % len(items))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] V5"; FAIL=$((FAIL+1)); FAILED_CASES+=("V5"); }

run_case "V6 schema_inventory(agent)" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"perspectives\":[\"schema_inventory\"],\"cognitionDepth\":\"L3\"}"
python3 -c "
import json
d=json.load(open('$OUT_DIR/V6_schema_inventory(agent).json'))
items=d['data']['perspectives']['schema_inventory']['schemas']
assert len(items)>=19, 'agent Schema 数量不足: %d' % len(items)
for s in items:
    assert s.get('name') and s.get('description'), 'schema 缺 name/description'
print('  [PASS] %d 个 Schema 均含 name/description' % len(items))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] V6"; FAIL=$((FAIL+1)); FAILED_CASES+=("V6"); }

# ============================================================================
# 第五部分: 错误场景
# ============================================================================

echo; echo "======================================================"
echo " 第五部分: 错误场景"
echo "======================================================"

run_case "E1 未知模板" "/not-a-template" "{\"bundleFqns\":[\"$META\"]}"
assert_err "未知模板" "$OUT_DIR/E1_未知模板.json" "34001"

run_case "E2 幽灵 agent 实体" "/step-guide" "{\"entityFqn\":\"ghost:1.0.0.agent.Task_Ghost\"}"
assert_err "幽灵实体" "$OUT_DIR/E2_幽灵_agent_实体.json" "34004"

run_case "E3 空 bundle" "/task-brief" "{\"bundleFqns\":[]}"
assert_err "空 bundle" "$OUT_DIR/E3_空_bundle.json" "34003"

# ============================================================================
# 第六部分: 幂等 / 并发
# ============================================================================

echo; echo "======================================================"
echo " 第六部分: 幂等 / 并发"
echo "======================================================"

run_case "I1 幂等(r1)" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"perspectives\":[\"capability_catalog\",\"flow_blueprint\",\"decision_matrix\"],\"cognitionDepth\":\"L3\"}"
sleep 1
run_case "I1 幂等(r2)" "/cognition-guidance" "{\"bundleFqns\":[\"$META\"],\"perspectives\":[\"capability_catalog\",\"flow_blueprint\",\"decision_matrix\"],\"cognitionDepth\":\"L3\"}"
if assert_ready "$OUT_DIR/I1_幂等(r1).json"; then
python3 -c "
import json
d1=json.load(open('$OUT_DIR/I1_幂等(r1).json'))
d2=json.load(open('$OUT_DIR/I1_幂等(r2).json'))
assert json.dumps(d1['data']['perspectives'],sort_keys=True)==json.dumps(d2['data']['perspectives'],sort_keys=True)
print('  [PASS] 两次输出一致')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] 幂等"; FAIL=$((FAIL+1)); FAILED_CASES+=("幂等"); }
fi

echo "  [INFO] 并发 5×task-brief(metaforge)..."
CONCURRENT_PASS=0
for i in 1 2 3 4 5; do
  ( curl -s -m 60 -X POST "$API/task-brief" -H "$J" -d "{\"bundleFqns\":[\"$META\"],\"cognitionDepth\":\"L3\"}" | python3 -c "import json,sys;print(json.load(sys.stdin)['code'])" > "$OUT_DIR/conc_$i.code" ) &
done
wait
for i in 1 2 3 4 5; do [ "$(cat "$OUT_DIR/conc_$i.code")" = "200" ] && CONCURRENT_PASS=$((CONCURRENT_PASS+1)); done
if [ "$CONCURRENT_PASS" -eq 5 ]; then PASS=$((PASS+1)); echo "  [PASS] 并发 5 次全部 200"; else
  echo "  [FAIL] 并发 $CONCURRENT_PASS/5"; FAIL=$((FAIL+1)); FAILED_CASES+=("并发"); fi

# ============================================================================
# 第七部分: metaforge-cli 消费端（US1 任务认知 MVP）
# 验证 metaforge-pro.sh 的 NL→FQN 推测 + task-brief 消费链路（FR-NL/FR-OUT/FR-ERR）。
# 依赖: metaforge-pro.sh 脚本（REPO_ROOT/.metaforge/scripts/）；META_FORGE_SERVER_URL
# ============================================================================

echo; echo "======================================================"
echo " 第七部分: metaforge-cli US1 任务认知 (MVP)"
echo "======================================================"

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || (cd "$(dirname "$0")/.." && pwd))
MF_CLI="${MF_CLI:-$REPO_ROOT/.metaforge/scripts/metaforge-pro.sh}"
if [ ! -x "$MF_CLI" ]; then
    echo "  [SKIP] 未找到 metaforge-pro.sh: $MF_CLI（跳过 US1 场景）"
else
    # US1-1: 显式 --bundles → task-brief json 返回（code=200 + 版本锚）
    if should_run "US1"; then
        printf '\n\033[1;36m=== US1-1 task-brief 显式 bundle ===\033[0m\n'
        OUT_US1="$OUT_DIR/US1_task-brief.json"
        if "$MF_CLI" cognition execute task-brief --bundles "$META" --format json > "$OUT_US1" 2>/dev/null; then
            python3 -c "
import json
d=json.load(open('$OUT_US1'))
assert d.get('code')==200, 'code=%s' % d.get('code')
cm=d['data']['contextMeta']
assert 'dataVersionAnchors' in cm or 'data_version_anchors' in cm, '缺版本锚'
print('  [PASS] task-brief code=200 + 版本锚')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] US1-1"; FAIL=$((FAIL+1)); FAILED_CASES+=("US1-1"); }
        fi

        # US1-2: 缺 --bundles → 必填校验中止（退出码 2 + 中文用法提示）
        printf '\n\033[1;36m=== US1-2 task-brief 缺 --bundles ===\033[0m\n'
        if "$MF_CLI" cognition execute task-brief > /dev/null 2>&1; then
            echo "  [FAIL] US1-2 应中止但成功"; FAIL=$((FAIL+1)); FAILED_CASES+=("US1-2")
        else
            rc=$?
            if [ "$rc" -eq 2 ]; then PASS=$((PASS+1)); echo "  [PASS] US1-2 必填校验中止 rc=2"
            else echo "  [FAIL] US1-2 rc=$rc 期望 2"; FAIL=$((FAIL+1)); FAILED_CASES+=("US1-2"); fi
        fi

        # US1-3: prompt 格式 → 输出可直接注入的 Markdown
        printf '\n\033[1;36m=== US1-3 task-brief prompt ===\033[0m\n'
        OUT_US3="$OUT_DIR/US1_task-brief.prompt"
        if "$MF_CLI" cognition execute task-brief --bundles "$META" --format prompt > "$OUT_US3" 2>/dev/null; then
            if grep -q "^#" "$OUT_US3" && [ -s "$OUT_US3" ]; then PASS=$((PASS+1)); echo "  [PASS] US1-3 prompt Markdown"; else
                echo "  [FAIL] US1-3 非 Markdown 或空"; FAIL=$((FAIL+1)); FAILED_CASES+=("US1-3"); fi
        else echo "  [FAIL] US1-3"; FAIL=$((FAIL+1)); FAILED_CASES+=("US1-3"); fi
    fi
fi

# ============================================================================
# 第八部分: metaforge-cli US2 实体即时指导
# 验证 step-guide 的 --entity-fqn 必填校验、34004 归属失败候选展示、实体级视角。
# ============================================================================

echo; echo "======================================================"
echo " 第八部分: metaforge-cli US2 实体即时指导"
echo "======================================================"

if [ ! -x "$MF_CLI" ]; then
    echo "  [SKIP] 未找到 metaforge-pro.sh: $MF_CLI（跳过 US2 场景）"
else
    # US2-1: 有效实体 → step-guide 返回实体级视角（含 impact_trace）
    if should_run "US2"; then
        printf '\n\033[1;36m=== US2-1 step-guide 有效实体 ===\033[0m\n'
        OUT_US2="$OUT_DIR/US2_step-guide.json"
        if "$MF_CLI" cognition execute step-guide --entity-fqn "$STEP_CHECK" --format json > "$OUT_US2" 2>/dev/null; then
            python3 -c "
import json
d=json.load(open('$OUT_US2'))
assert d.get('code')==200, 'code=%s' % d.get('code')
pers=d['data']['perspectives']
for p in ('entity_profile','constraint_set','capability_catalog','decision_matrix','impact_trace','relationship_graph'):
    assert p in pers, '缺视角 %s' % p
print('  [PASS] step-guide 6 视角齐备')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] US2-1"; FAIL=$((FAIL+1)); FAILED_CASES+=("US2-1"); }
        fi

        # US2-2: 缺 --entity-fqn → 必填校验中止（退出码 2 + 中文用法提示）
        printf '\n\033[1;36m=== US2-2 step-guide 缺 --entity-fqn ===\033[0m\n'
        if "$MF_CLI" cognition execute step-guide > /dev/null 2>&1; then
            echo "  [FAIL] US2-2 应中止但成功"; FAIL=$((FAIL+1)); FAILED_CASES+=("US2-2")
        else
            rc=$?
            if [ "$rc" -eq 2 ]; then PASS=$((PASS+1)); echo "  [PASS] US2-2 必填校验中止 rc=2"
            else echo "  [FAIL] US2-2 rc=$rc 期望 2"; FAIL=$((FAIL+1)); FAILED_CASES+=("US2-2"); fi
        fi

        # US2-3: 幽灵实体 → 34004 归属失败 + 候选列表展示（stderr）
        printf '\n\033[1;36m=== US2-3 step-guide 34004 归属失败 ===\033[0m\n'
        ERR_US2="$OUT_DIR/US2_34004.err"
        if "$MF_CLI" cognition execute step-guide --entity-fqn ghost:1.0.0.Task_X > /dev/null 2> "$ERR_US2"; then
            echo "  [FAIL] US2-3 应失败但成功"; FAIL=$((FAIL+1)); FAILED_CASES+=("US2-3")
        else
            rc=$?
            if [ "$rc" -eq 1 ] && grep -q "归属校验失败" "$ERR_US2" && grep -q "候选列表" "$ERR_US2"; then
                PASS=$((PASS+1)); echo "  [PASS] US2-3 34004 归属失败 + 候选列表 rc=$rc"
            else
                echo "  [FAIL] US2-3 rc=$rc 或提示缺失"; FAIL=$((FAIL+1)); FAILED_CASES+=("US2-3")
            fi
        fi

        # US2-4: prompt 格式输出 Markdown
        printf '\n\033[1;36m=== US2-4 step-guide prompt ===\033[0m\n'
        OUT_US4="$OUT_DIR/US2_step-guide.prompt"
        if "$MF_CLI" cognition execute step-guide --entity-fqn "$STEP_CHECK" --format prompt > "$OUT_US4" 2>/dev/null; then
            if [ -s "$OUT_US4" ]; then PASS=$((PASS+1)); echo "  [PASS] US2-4 prompt 输出"
            else echo "  [FAIL] US2-4 空输出"; FAIL=$((FAIL+1)); FAILED_CASES+=("US2-4"); fi
        else echo "  [FAIL] US2-4"; FAIL=$((FAIL+1)); FAILED_CASES+=("US2-4"); fi
    fi
fi

# ============================================================================
# 第九部分: metaforge-cli US3 平台发现 / 领域导航
# ============================================================================

echo; echo "======================================================"
echo " 第九部分: metaforge-cli US3 平台发现 / 领域导航"
echo "======================================================"

if [ ! -x "$MF_CLI" ]; then
    echo "  [SKIP] 未找到 metaforge-pro.sh: $MF_CLI（跳过 US3 场景）"
else
    if should_run "US3"; then
        printf '\n\033[1;36m=== US3-1 catalog Bundle 清单 ===\033[0m\n'
        OUT="$OUT_DIR/US3_catalog.json"
        if "$MF_CLI" cognition execute bundle-catalog --format json > "$OUT" 2>/dev/null; then
            python3 -c "
import json
d=json.load(open('$OUT'))
assert d.get('code')==200, 'code=%s' % d.get('code')
print('  [PASS] US3-1 catalog 返回')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] US3-1"; FAIL=$((FAIL+1)); FAILED_CASES+=("US3-1"); }
        else echo "  [FAIL] US3-1"; FAIL=$((FAIL+1)); FAILED_CASES+=("US3-1"); fi

        printf '\n\033[1;36m=== US3-2 navigate 缺 --bundles 中止 ===\033[0m\n'
        if "$MF_CLI" cognition execute navigate > /dev/null 2>&1; then
            echo "  [FAIL] US3-2 应中止但成功"; FAIL=$((FAIL+1)); FAILED_CASES+=("US3-2")
        else
            rc=$?
            if [ "$rc" -eq 2 ]; then PASS=$((PASS+1)); echo "  [PASS] US3-2 navigate 必填校验 rc=2"
            else echo "  [FAIL] US3-2 rc=$rc"; FAIL=$((FAIL+1)); FAILED_CASES+=("US3-2"); fi
        fi

        printf '\n\033[1;36m=== US3-3 navigate 下钻 + 分页 ===\033[0m\n'
        OUT="$OUT_DIR/US3_navigate.json"
        if "$MF_CLI" cognition execute navigate --bundles "$META" --page-size 10 --format json > "$OUT" 2>/dev/null; then
            python3 -c "
import json
d=json.load(open('$OUT'))
assert d.get('code')==200, 'code=%s' % d.get('code')
print('  [PASS] US3-3 navigate 下钻')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] US3-3"; FAIL=$((FAIL+1)); FAILED_CASES+=("US3-3"); }
        else echo "  [FAIL] US3-3"; FAIL=$((FAIL+1)); FAILED_CASES+=("US3-3"); fi
    fi
fi

# ============================================================================
# 第十部分: metaforge-cli US4 子任务认知
# ============================================================================

echo; echo "======================================================"
echo " 第十部分: metaforge-cli US4 子任务认知"
echo "======================================================"

if [ ! -x "$MF_CLI" ]; then
    echo "  [SKIP] 未找到 metaforge-pro.sh: $MF_CLI（跳过 US4 场景）"
else
    if should_run "US4"; then
        printf '\n\033[1;36m=== US4-1 subtask 缺参中止 ===\033[0m\n'
        if "$MF_CLI" cognition execute sub-task-brief > /dev/null 2>&1; then
            echo "  [FAIL] US4-1 应中止但成功"; FAIL=$((FAIL+1)); FAILED_CASES+=("US4-1")
        else
            rc=$?
            if [ "$rc" -eq 2 ]; then PASS=$((PASS+1)); echo "  [PASS] US4-1 必填校验 rc=2"
            else echo "  [FAIL] US4-1 rc=$rc"; FAIL=$((FAIL+1)); FAILED_CASES+=("US4-1"); fi
        fi

        printf '\n\033[1;36m=== US4-2 subtask INHERITED ===\033[0m\n'
        OUT="$OUT_DIR/US4_subtask.json"
        if "$MF_CLI" cognition execute sub-task-brief --bundles "$META" --entry-entity "$TASK_ORDER" --scope-mode INHERITED --format json > "$OUT" 2>/dev/null; then
            python3 -c "
import json
d=json.load(open('$OUT'))
assert d.get('code')==200, 'code=%s' % d.get('code')
print('  [PASS] US4-2 subtask INHERITED')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] US4-2"; FAIL=$((FAIL+1)); FAILED_CASES+=("US4-2"); }
        else echo "  [FAIL] US4-2"; FAIL=$((FAIL+1)); FAILED_CASES+=("US4-2"); fi
    fi
fi

# ============================================================================
# 第十一部分: metaforge-cli US5 自由视角组合
# ============================================================================

echo; echo "======================================================"
echo " 第十一部分: metaforge-cli US5 自由视角组合"
echo "======================================================"

if [ ! -x "$MF_CLI" ]; then
    echo "  [SKIP] 未找到 metaforge-pro.sh: $MF_CLI（跳过 US5 场景）"
else
    if should_run "US5"; then
        printf '\n\033[1;36m=== US5-1 guidance 视角组合透传 ===\033[0m\n'
        OUT="$OUT_DIR/US5_guidance.json"
        if "$MF_CLI" cognition execute cognition-guidance --bundles "$META" --perspectives schema_inventory,instance_catalog --format json > "$OUT" 2>/dev/null; then
            python3 -c "
import json
d=json.load(open('$OUT'))
assert d.get('code')==200, 'code=%s' % d.get('code')
print('  [PASS] US5-1 guidance 组合查询')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] US5-1"; FAIL=$((FAIL+1)); FAILED_CASES+=("US5-1"); }
        else echo "  [FAIL] US5-1"; FAIL=$((FAIL+1)); FAILED_CASES+=("US5-1"); fi
    fi
fi

# ============================================================================
# 第十二部分: metaforge-cli US7 认知新鲜度（版本锚）
# ============================================================================

echo; echo "======================================================"
echo " 第十二部分: metaforge-cli US7 认知新鲜度"
echo "======================================================"

if [ ! -x "$MF_CLI" ]; then
    echo "  [SKIP] 未找到 metaforge-pro.sh: $MF_CLI（跳过 US7 场景）"
else
    if should_run "US7"; then
        printf '\n\033[1;36m=== US7-1 输出含版本锚 ===\033[0m\n'
        OUT="$OUT_DIR/US7_anchors.json"
        if "$MF_CLI" cognition execute task-brief --bundles "$META" --format json > "$OUT" 2>/dev/null; then
            python3 -c "
import json
d=json.load(open('$OUT'))
cm=d['data'].get('contextMeta') or d['data'].get('context_meta') or {}
anchors=cm.get('dataVersionAnchors') or cm.get('data_version_anchors') or {}
assert len(anchors)>0, '缺版本锚'
print('  [PASS] US7-1 版本锚存在')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] US7-1"; FAIL=$((FAIL+1)); FAILED_CASES+=("US7-1"); }
        else echo "  [FAIL] US7-1"; FAIL=$((FAIL+1)); FAILED_CASES+=("US7-1"); fi
    fi
fi

# ============================================================================
# 汇总
# ============================================================================

echo; echo "======================================================"
echo " 测试汇总"
echo "======================================================"
echo " 通过: $PASS  失败: $FAIL"
if [ "$FAIL" -gt 0 ]; then
    echo " 失败用例:"; for c in "${FAILED_CASES[@]}"; do echo "   - $c"; done
    exit 1
fi
echo " 全部通过。输出文件: $OUT_DIR"
