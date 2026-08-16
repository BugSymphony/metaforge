#!/usr/bin/env bash
# ============================================================================
# agent-cognition 个人测试脚本
# 覆盖: 6 个模板 × 14 个视角 × 多个数据集 × 深度/原型/作用域/格式 × 错误场景
#
# 用法:
#   ./cognition-test.sh            # 运行全部(先自动 seed 测试数据)
#   ./cognition-test.sh <pattern>  # 只运行名称匹配 <pattern> 的用例(同样先 seed)
#   ./cognition-test.sh --no-seed [<pattern>]   # 跳过数据准备
#   ./cognition-test.sh --seed-only            # 只准备数据,不跑用例
#   ./cognition-test.sh --help
#   BASE_URL=http://localhost:8080 ./cognition-test.sh
#   PGHOST=localhost PGPORT=5432 PGDATABASE=metaforge PGUSER=metaforge PGPASSWORD=metaforge \
#       ./cognition-test.sh --seed-only
#
# 数据准备: 幂等 seed SQL (cognition-seed.sql), 补齐 erp/order 两个测试 Bundle
#   的 Bundle/Version/Package/EntitySchema/RelationSchema/M1实例/关系实例。
#   缺什么补什么, 可重复执行。
#
# 依赖: curl, python3, psql (仅 --seed/seed-only 需要 psql)
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
        --help|-h)
            sed -n '2,14p' "$0"
            exit 0
            ;;
        *) FILTER="$1"; shift ;;
    esac
done

BASE_URL="${BASE_URL:-http://localhost:8080}"
API="$BASE_URL/api/v1/cognition"
OUT_DIR="${TEST_OUT_DIR:-/tmp/cognition-tests}"
SEED_SQL="${SEED_SQL:-$(dirname "$0")/cognition-seed.sql}"
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
    if [ ! -f "$SEED_SQL" ]; then
        echo "  [FAIL] seed SQL 不存在: $SEED_SQL"
        exit 1
    fi
    echo "  [INFO] 准备测试数据: $SEED_SQL"
    if ! PGPASSWORD="$PGPASSWORD" psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
            -v ON_ERROR_STOP=1 -f "$SEED_SQL" > /dev/null 2> /tmp/seed.err; then
        echo "  [FAIL] 数据准备失败:"
        cat /tmp/seed.err | tail -5
        exit 1
    fi
    echo "  [INFO] 数据准备完成"
}

if [ "$SEED" -eq 1 ]; then
    run_seed
fi

if [ "$SEED_ONLY" -eq 1 ]; then
    echo "  [INFO] --seed-only: 数据已就绪, 退出"
    exit 0
fi

# ---------- 基础工具 ----------

START_TS=$(date +%s)

should_run() {
    [ -z "${FILTER:-}" ] || echo "$1" | grep -q "$FILTER"
}

# 断言仅当响应文件为本轮新生成时才执行（过滤模式下避免陈旧文件误判）
assert_ready() {
    [ -f "$1" ] && [ "$(stat -c %Y "$1" 2>/dev/null)" -ge "$START_TS" ]
}

run_case() {
    local name="$1"; shift
    if ! should_run "$name"; then
        return
    fi
    printf '\n\033[1;36m=== %s ===\033[0m\n' "$name"
    local out="$OUT_DIR/$(echo "$name" | tr '/ ' '__').json"
    # shellcheck disable=SC2068
    curl -s -m 60 -X POST "$API$1" -H "$J" -d "$2" > "$out"
    local rc=$?
    if [ $rc -ne 0 ]; then
        echo "  [FAIL] curl 异常 rc=$rc"
        FAIL=$((FAIL+1)); FAILED_CASES+=("$name")
        return
    fi
    local code
    code=$(python3 -c "import json,sys;d=json.load(open('$out'));print(d.get('code',d.get('status','?')))" 2>/dev/null)
    echo "  code=$code  (详见 $out)"
}

# code=200 且包含指定视角
assert_ok() {
    local name="$1" file="$2" want_perspectives="${3:-}"
    if ! assert_ready "$file"; then
        return
    fi
    if [ $# -gt 3 ]; then
        # 有自定义校验函数
        # shellcheck disable=SC2068
        "$4" "$file" ${5:-} ${6:-} || { echo "  [FAIL] $name"; FAIL=$((FAIL+1)); FAILED_CASES+=("$name"); return; }
        PASS=$((PASS+1)); return
    fi
    if [ -z "$want_perspectives" ]; then
        python3 -c "
import json,sys
d=json.load(open('$file'))
assert d.get('code')==200, 'code=%s %s' % (d.get('code'), d.get('message'))
print('  [PASS]')
" 2>/dev/null && { PASS=$((PASS+1)); return; }
    else
        python3 -c "
import json,sys
d=json.load(open('$file'))
assert d.get('code')==200, 'code=%s %s' % (d.get('code'), d.get('message'))
perspectives=d['data']['perspectives']
want=set('$want_perspectives'.split(','))
got=set(perspectives.keys())
assert want <= got, '缺少视角: %s (got %s)' % (want-got, got)
print('  [PASS]')
" 2>/dev/null && { PASS=$((PASS+1)); return; }
    fi
    echo "  [FAIL] $name"
    python3 -c "import json;d=json.load(open('$file'));print('  msg:',d.get('message'));print('  perspectives:',d.get('data',{}).get('perspectives',{}).keys() if 'data' in d else None)" 2>/dev/null
    FAIL=$((FAIL+1)); FAILED_CASES+=("$name")
}

# 断言指定错误码
assert_err() {
    local name="$1" file="$2" want_code="$3"
    if ! assert_ready "$file"; then
        return
    fi
    local got
    got=$(python3 -c "import json;d=json.load(open('$file'));print(d.get('code'))" 2>/dev/null)
    if [ "$got" = "$want_code" ]; then
        PASS=$((PASS+1)); echo "  [PASS] code=$got"
    else
        echo "  [FAIL] $name 期望 code=$want_code 实际=$got"
        FAIL=$((FAIL+1)); FAILED_CASES+=("$name")
    fi
}

# ---------- 数据集 ----------
ERP="erp:1.0.0"
ORDER="order:1.0.0"
ORDER_ENTITY="order:1.0.0.pkg_order.Order_001"
ORDER_ITEM="order:1.0.0.pkg_order.Item_003"
ERP_ENTITY="Order_100"
MULTI="[\"$ERP\",\"$ORDER\"]"

# ============================================================================
# 第一部分: 6 个模板
# ============================================================================

echo; echo "======================================================"
echo " 第一部分: 模板测试 (6 个模板)"
echo "======================================================"

run_case "T1 bundle-catalog" "/bundle-catalog" "{\"bundleFqns\":$MULTI}"
assert_ok "bundle-catalog" "$OUT_DIR/T1_bundle-catalog.json" "bundle_directory,domain_navigation"

run_case "T2 cognition-guidance" "/cognition-guidance" "{\"bundleFqns\":$MULTI,\"cognitionDepth\":\"L3\"}"
assert_ok "cognition-guidance" "$OUT_DIR/T2_cognition-guidance.json" ""

run_case "T3 task-brief(L2)" "/task-brief" "{\"bundleFqns\":[\"$ERP\"],\"cognitionDepth\":\"L2\"}"
assert_ok "task-brief L2" "$OUT_DIR/T3_task-brief(L2).json" "entity_profile,constraint_set,capability_catalog,flow_blueprint,decision_matrix,prerequisite_chain"

run_case "T4 task-brief(L3)" "/task-brief" "{\"bundleFqns\":[\"$ERP\"],\"cognitionDepth\":\"L3\",\"maxTokens\":30000}"
assert_ok "task-brief L3" "$OUT_DIR/T4_task-brief(L3).json" ""

run_case "T5 step-guide(order)" "/step-guide" "{\"entityFqn\":\"$ORDER_ENTITY\",\"cognitionDepth\":\"L3\"}"
assert_ok "step-guide order" "$OUT_DIR/T5_step-guide(order).json" "entity_profile,constraint_set,capability_catalog,decision_matrix,impact_trace,relationship_graph"

run_case "T6 step-guide(erp)" "/step-guide" "{\"entityFqn\":\"$ERP_ENTITY\",\"cognitionDepth\":\"L3\"}"
# erp 实体为裸 FQN(Order_100),无法推导 bundle 前缀 -> 期望 34004
assert_err "step-guide erp(裸FQN->34004)" "$OUT_DIR/T6_step-guide(erp).json" "34004"

run_case "T7 navigate" "/navigate" "{\"bundleFqns\":[\"$ERP\"],\"level\":\"L1\",\"pageSize\":5,\"expand\":\"lazy\"}"
assert_ok "navigate" "$OUT_DIR/T7_navigate.json" "domain_navigation"

run_case "T8 sub-task INHERITED" "/sub-task-brief" "{\"entryEntityFqn\":\"$ORDER_ENTITY\",\"scopeMode\":\"INHERITED\",\"cognitionDepth\":\"L2\"}"
assert_ok "sub-task INHERITED" "$OUT_DIR/T8_sub-task_INHERITED.json" "entity_profile,constraint_set,capability_catalog,decision_matrix,prerequisite_chain,relationship_graph"

run_case "T9 sub-task PURE" "/sub-task-brief" "{\"entryEntityFqn\":\"$ORDER_ENTITY\",\"scopeMode\":\"PURE\"}"
assert_ok "sub-task PURE" "$OUT_DIR/T9_sub-task_PURE.json" "entity_profile"

run_case "T10 prompt 格式" "/task-brief" "{\"bundleFqns\":[\"$ERP\"],\"format\":\"prompt\",\"cognitionDepth\":\"L2\"}"
if assert_ready "$OUT_DIR/T10_prompt_格式.json"; then
python3 -c "
import json
d=json.load(open('$OUT_DIR/T10_prompt_格式.json'))
assert d.get('code')==200, d.get('message')
sub=d.get('data') or {}
assert sub.get('format')=='prompt' and sub.get('content'), 'prompt 输出缺失'
assert len(sub['content'])>0, 'prompt 内容为空'
print('  [PASS] prompt content', len(sub['content']), 'chars')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] T10 prompt 格式"; FAIL=$((FAIL+1)); FAILED_CASES+=("T10 prompt 格式"); }
fi

# ============================================================================
# 第二部分: 14 个视角逐一测试 (bundle 级 + 实体级)
# ============================================================================

echo; echo "======================================================"
echo " 第二部分: 14 个视角逐一测试"
echo "======================================================"

# bundle 级视角 (BUNDLE/BOTH scope)
bundle_perspectives=(
  "entity_profile"
  "constraint_set"
  "capability_catalog"
  "flow_blueprint"
  "decision_matrix"
  "prerequisite_chain"
  "domain_navigation"
  "instance_catalog"
  "bundle_directory"
  "schema_inventory"
)
for p in "${bundle_perspectives[@]}"; do
  run_case "P-B $p" "/cognition-guidance" "{\"bundleFqns\":[\"$ERP\"],\"perspectives\":[\"$p\"],\"cognitionDepth\":\"L3\"}"
  assert_ok "bundle 视角 $p" "$OUT_DIR/P-B_$p.json" "$p"
done

# 实体级视角 (ENTITY scope)
entity_perspectives=(
  "domain_location"
  "composition_tree"
  "relationship_graph"
  "impact_trace"
)
for p in "${entity_perspectives[@]}"; do
  run_case "P-E $p" "/cognition-guidance" "{\"bundleFqns\":[\"$ERP\"],\"entityFqn\":\"$ERP_ENTITY\",\"perspectives\":[\"$p\"],\"cognitionDepth\":\"L3\"}"
  assert_ok "实体视角 $p" "$OUT_DIR/P-E_$p.json" "$p"
done

# ============================================================================
# 第三部分: 深度 / 原型 / 作用域 / 多 Bundle
# ============================================================================

echo; echo "======================================================"
echo " 第三部分: 深度 / 原型 / 作用域 / 多 Bundle"
echo "======================================================"

for depth in L1 L2 L3; do
  run_case "D $depth" "/task-brief" "{\"bundleFqns\":[\"$ERP\"],\"cognitionDepth\":\"$depth\"}"
  if ! assert_ready "$OUT_DIR/D_$depth.json"; then continue; fi
  # 校验视角数上限
  python3 -c "
import json,sys
d=json.load(open(sys.argv[1]))
depth=sys.argv[2]
assert d.get('code')==200
n=len(d['data']['perspectives'])
limits={'L1':3,'L2':7,'L3':14}
assert n<=limits[depth], depth+' 视角数 %d 超限' % n
print('  [PASS] L'+depth+' 视角数='+str(n))
" "$OUT_DIR/D_$depth.json" "$depth" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] depth $depth"; FAIL=$((FAIL+1)); FAILED_CASES+=("depth $depth"); }
done

for a in execution exploration audit orchestration; do
  run_case "A $a" "/task-brief" "{\"bundleFqns\":[\"$ERP\"],\"agentArchetype\":\"$a\",\"cognitionDepth\":\"L3\"}"
  assert_ok "archetype $a" "$OUT_DIR/A_$a.json" ""
done

# 多 Bundle 数据版本锚点
run_case "M multi-bundle" "/cognition-guidance" "{\"bundleFqns\":$MULTI,\"perspectives\":[\"schema_inventory\",\"constraint_set\"],\"cognitionDepth\":\"L3\"}"
if assert_ready "$OUT_DIR/M_multi-bundle.json"; then
python3 -c "
import json
d=json.load(open('$OUT_DIR/M_multi-bundle.json'))
assert d.get('code')==200
anchors=d['data']['contextMeta']['dataVersionAnchors']
b=frozenset(a['bundleFqn'] for a in anchors)
assert b>=frozenset({'erp','order'}), '多 bundle 锚点缺失: %s' % b
print('  [PASS] anchors:', sorted(b))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] multi-bundle 锚点"; FAIL=$((FAIL+1)); FAILED_CASES+=("multi-bundle"); }
fi

# 版本化 FQN 归一化
run_case "N versioned-fqn" "/task-brief" "{\"bundleFqns\":[\"$ORDER\",\"order:0.0.1\"],\"cognitionDepth\":\"L2\"}"
assert_ok "versioned-fqn" "$OUT_DIR/N_versioned-fqn.json" ""

# ============================================================================
# 第四部分: 数据完整性校验 (name/description/attributes)
# ============================================================================

echo; echo "======================================================"
echo " 第四部分: 数据完整性 (名称/描述/属性)"
echo "======================================================"

check_profile() {
    python3 -c "
import json,sys
d=json.load(open('$1'))
p=d['data']['perspectives']['entity_profile']
assert p.get('name'), '缺 name'
assert p.get('entitySchemaFqn'), '缺 entitySchemaFqn'
assert p.get('content') is not None, '缺 content(属性)'
assert len(p.get('schemaAttributes') or [])>0, '缺 schemaAttributes'
for a in (p.get('schemaAttributes') or []):
    assert a.get('name'), 'schema 属性缺 name'
    assert 'description' in a or a.get('type'), 'schema 属性缺 type/description'
print('  [PASS] name=%s attrs=%d' % (p.get('name'), len(p.get('schemaAttributes'))))
" 2>/dev/null || return 1
}

check_schemas() {
    python3 -c "
import json,sys
d=json.load(open('$1'))
items=d['data']['perspectives']['schema_inventory']['schemas']
assert len(items)>0, '无 schema'
for s in items:
    assert s.get('name'), 'schema 缺 name: %s' % s.get('schemaFqn')
    assert s.get('description'), 'schema 缺 description: %s' % s.get('schemaFqn')
print('  [PASS] %d schemas 均有 name/description' % len(items))
" 2>/dev/null || return 1
}

check_instances() {
    python3 -c "
import json,sys
d=json.load(open('$1'))
items=d['data']['perspectives']['instance_catalog']['entities']
assert len(items)>0, '无实例'
for e in items:
    assert e.get('name'), '实例缺 name: %s' % e.get('fqn')
    assert e.get('entitySchemaFqn'), '实例缺 entitySchemaFqn: %s' % e.get('fqn')
print('  [PASS] %d 实例均有 name/entitySchemaFqn' % len(items))
" 2>/dev/null || return 1
}

check_bundles() {
    python3 -c "
import json,sys
d=json.load(open('$1'))
items=d['data']['perspectives']['bundle_directory']['bundles']
assert len(items)>0, '无 bundle'
for b in items:
    assert b.get('name'), 'bundle 缺 name: %s' % b.get('fqn')
    assert b.get('description'), 'bundle 缺 description: %s' % b.get('fqn')
print('  [PASS] %d bundles 均有 name/description' % len(items))
" 2>/dev/null || return 1
}

check_constraints() {
    python3 -c "
import json,sys
d=json.load(open('$1'))
hb=d['data']['perspectives']['constraint_set']['hardBoundaries']
assert len(hb)>0, '无硬边界'
for c in hb:
    assert c.get('fieldName'), '硬边界缺 fieldName'
print('  [PASS] %d 硬边界均有 fieldName(属性)' % len(hb))
" 2>/dev/null || return 1
}

run_case "V entity_profile" "/cognition-guidance" "{\"bundleFqns\":[\"$ERP\"],\"entityFqn\":\"$ERP_ENTITY\",\"perspectives\":[\"entity_profile\"],\"cognitionDepth\":\"L3\"}"
assert_ok "完整性 entity_profile" "$OUT_DIR/V_entity_profile.json" "" check_profile

run_case "V schema_inventory" "/cognition-guidance" "{\"bundleFqns\":[\"$ERP\"],\"perspectives\":[\"schema_inventory\"],\"cognitionDepth\":\"L3\"}"
assert_ok "完整性 schema_inventory" "$OUT_DIR/V_schema_inventory.json" "" check_schemas

run_case "V instance_catalog" "/cognition-guidance" "{\"bundleFqns\":[\"$ERP\"],\"perspectives\":[\"instance_catalog\"],\"cognitionDepth\":\"L3\"}"
assert_ok "完整性 instance_catalog" "$OUT_DIR/V_instance_catalog.json" "" check_instances

run_case "V bundle_directory" "/cognition-guidance" "{\"bundleFqns\":[\"$ERP\"],\"perspectives\":[\"bundle_directory\"],\"cognitionDepth\":\"L3\"}"
assert_ok "完整性 bundle_directory" "$OUT_DIR/V_bundle_directory.json" "" check_bundles

run_case "V constraint_set" "/cognition-guidance" "{\"bundleFqns\":[\"$ERP\"],\"perspectives\":[\"constraint_set\"],\"cognitionDepth\":\"L3\"}"
assert_ok "完整性 constraint_set" "$OUT_DIR/V_constraint_set.json" "" check_constraints

# ============================================================================
# 第五部分: 错误场景
# ============================================================================

echo; echo "======================================================"
echo " 第五部分: 错误场景"
echo "======================================================"

run_case "E1 未知模板" "/not-a-template" "{\"bundleFqns\":[\"$ERP\"]}"
assert_err "未知模板" "$OUT_DIR/E1_未知模板.json" "34001"

run_case "E2 幽灵实体" "/step-guide" "{\"entityFqn\":\"ghost:1.0.0.pkg.FakeEntity\"}"
assert_err "幽灵实体" "$OUT_DIR/E2_幽灵实体.json" "34004"

run_case "E3 非法 FQN" "/task-brief" "{\"bundleFqns\":[\"bad fqn!!!\"]}"
# 非法 FQN 应返回校验错误
if assert_ready "$OUT_DIR/E3_非法_FQN.json"; then
python3 -c "
import json
d=json.load(open('$OUT_DIR/E3_非法_FQN.json'))
assert d.get('code') in (34002,34003,34004,10000), 'code=%s' % d.get('code')
print('  [PASS] code=%s (%s)' % (d.get('code'), d.get('message')))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] E3 非法 FQN"; FAIL=$((FAIL+1)); FAILED_CASES+=("E3 非法 FQN"); }
fi

run_case "E4 空 bundle" "/task-brief" "{\"bundleFqns\":[]}"
assert_err "空 bundle" "$OUT_DIR/E4_空_bundle.json" "34003"

run_case "E5 非法 scope_mode" "/sub-task-brief" "{\"entryEntityFqn\":\"$ORDER_ENTITY\",\"scopeMode\":\"BAD\"}"
assert_err "非法 scope_mode" "$OUT_DIR/E5_非法_scope_mode.json" "34007"

# ============================================================================
# 第六部分: 幂等 / 并发
# ============================================================================

echo; echo "======================================================"
echo " 第六部分: 幂等 / 并发"
echo "======================================================"

run_case "I 幂等(r1)" "/cognition-guidance" "{\"bundleFqns\":[\"$ERP\"],\"perspectives\":[\"schema_inventory\",\"instance_catalog\"],\"cognitionDepth\":\"L3\"}"
sleep 1
run_case "I 幂等(r2)" "/cognition-guidance" "{\"bundleFqns\":[\"$ERP\"],\"perspectives\":[\"schema_inventory\",\"instance_catalog\"],\"cognitionDepth\":\"L3\"}"
if assert_ready "$OUT_DIR/I_幂等(r1).json"; then
python3 -c "
import json
d1=json.load(open('$OUT_DIR/I_幂等(r1).json'))
d2=json.load(open('$OUT_DIR/I_幂等(r2).json'))
assert json.dumps(d1['data']['perspectives'],sort_keys=True)==json.dumps(d2['data']['perspectives'],sort_keys=True)
print('  [PASS] 两次输出一致')
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] 幂等性"; FAIL=$((FAIL+1)); FAILED_CASES+=("幂等"); }
fi

echo "  [INFO] 并发 5×task-brief..."
CONCURRENT_PASS=0
if should_run "并发"; then
for i in 1 2 3 4 5; do
  ( curl -s -m 60 -X POST "$API/task-brief" -H "$J" -d "{\"bundleFqns\":[\"$ERP\"],\"cognitionDepth\":\"L2\"}" | python3 -c "import json,sys;print(json.load(sys.stdin)['code'])" > "$OUT_DIR/conc_$i.code" ) &
done
wait
for i in 1 2 3 4 5; do
  if [ "$(cat "$OUT_DIR/conc_$i.code")" = "200" ]; then CONCURRENT_PASS=$((CONCURRENT_PASS+1)); fi
done
if [ "$CONCURRENT_PASS" -eq 5 ]; then
  PASS=$((PASS+1)); echo "  [PASS] 并发 5 次全部 200"
else
  echo "  [FAIL] 并发: $CONCURRENT_PASS/5 成功"
  FAIL=$((FAIL+1)); FAILED_CASES+=("并发")
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
    echo " 失败用例:"
    for c in "${FAILED_CASES[@]}"; do echo "   - $c"; done
    echo " 响应文件: $OUT_DIR"
    exit 1
fi
echo " 全部通过。输出文件: $OUT_DIR"
