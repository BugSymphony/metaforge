#!/usr/bin/env bash
# ============================================================================
# quickstart.md 端到端验证脚本 (metaforge-cli 元认知指导能力)
# 覆盖 quickstart.md 的 V1-V10 全部场景, 经 metaforge-pro.sh CLI 消费链路验证。
# 数据准备与 test/cognition-agent-test.sh 一致: cognition-agent-seed.sql (幂等)。
#
# 用法:
#   ./cli-test.sh             # seed + 全部场景
#   ./cli-test.sh <pattern>   # seed + 匹配场景
#   ./cli-test.sh --no-seed [<pattern>]
#   ./cli-test.sh --seed-only
#   ./cli-test.sh --help
#   BASE_URL=... META_FORGE_SERVER_URL=... MF_CLI=... ./cli-test.sh
#
# 依赖: curl, python3, psql(仅 seed), metaforge-pro.sh (REPO_ROOT/.metaforge 或 metaforge-cli/.metaforge)
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
OUT_DIR="${TEST_OUT_DIR:-/tmp/cognition-quickstart-tests}"
SEED_SQL="${SEED_SQL:-$(dirname "$0")/cognition-agent-seed.sql}"
mkdir -p "$OUT_DIR"

PASS=0
FAIL=0
FAILED_CASES=()

# ---------- 数据准备 (与 cognition-agent-test.sh 一致) ----------
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGDATABASE="${PGDATABASE:-metaforge}"
PGUSER="${PGUSER:-metaforge}"
PGPASSWORD="${PGPASSWORD:-metaforge}"

run_seed() {
    [ -f "$SEED_SQL" ] || { echo "  [FAIL] seed SQL 不存在: $SEED_SQL"; exit 1; }
    echo "  [INFO] 准备认知测试数据: $SEED_SQL"
    if ! PGPASSWORD="$PGPASSWORD" psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" \
            -v ON_ERROR_STOP=1 -f "$SEED_SQL" > /dev/null 2> /tmp/cognition-quickstart-seed.err; then
        echo "  [FAIL] 数据准备失败:"; tail -5 /tmp/cognition-quickstart-seed.err; exit 1
    fi
    echo "  [INFO] 数据准备完成"
}

if [ "$SEED" -eq 1 ]; then run_seed; fi
if [ "$SEED_ONLY" -eq 1 ]; then echo "  [INFO] --seed-only 完成"; exit 0; fi

# ---------- CLI 定位 ----------
REPO_ROOT=$(cd "$(dirname "$0")/.." && pwd)
if [ ! -x "$REPO_ROOT/.metaforge/scripts/metaforge-pro.sh" ] && [ -x "$REPO_ROOT/metaforge-cli/.metaforge/scripts/metaforge-pro.sh" ]; then
    MF_CLI="${MF_CLI:-$REPO_ROOT/metaforge-cli/.metaforge/scripts/metaforge-pro.sh}"
else
    MF_CLI="${MF_CLI:-$REPO_ROOT/.metaforge/scripts/metaforge-pro.sh}"
fi
if [ ! -x "$MF_CLI" ]; then
    echo "  [SKIP] 未找到 metaforge-pro.sh: $MF_CLI（跳过全部场景）"
    exit 1
fi

# ---------- 基础工具 ----------
START_TS=$(date +%s)
should_run() { [ -z "${FILTER:-}" ] || echo "$1" | grep -q "$FILTER"; }
assert_ready() { [ -f "$1" ] && [ "$(stat -c %Y "$1" 2>/dev/null)" -ge "$START_TS" ]; }

# 运行 CLI 场景: 捕获 stdout(文件) / stderr(文件) / 退出码(文件)
run_cli() {
    local name="$1"; shift
    if ! should_run "$name"; then return 1; fi
    printf '\n\033[1;36m=== %s ===\033[0m\n' "$name"
    local base="$OUT_DIR/$(echo "$name" | tr '/ ' '__')"
    META_FORGE_SERVER_URL="${META_FORGE_SERVER_URL:-$BASE_URL}" "$MF_CLI" "$@" > "$base.out" 2> "$base.err"
    echo "$?" > "$base.rc"
    echo "  rc=$(cat "$base.rc") (详见 $base.out / $base.err)"
    return 0
}

# 断言: CLI 成功且 stdout 为 JSON code=200
assert_json_ok() {
    local name="$1" file="$2"
    if ! assert_ready "$file"; then return; fi
    python3 -c "
import json
d=json.load(open('$file'))
assert d.get('code')==200, 'code=%s %s' % (d.get('code'),d.get('message'))
print('  [PASS] code=200')
" 2>/dev/null && { PASS=$((PASS+1)); return; }
    echo "  [FAIL] $name"
    cat "${file%.out}.err" 2>/dev/null | head -5 | sed 's/^/  stderr: /'
    FAIL=$((FAIL+1)); FAILED_CASES+=("$name")
}

# 断言: CLI 退出码
assert_rc() {
    local name="$1" rcf="$2" want="$3"
    if ! assert_ready "$rcf"; then return; fi
    local got
    got="$(cat "$rcf")"
    if [ "$got" = "$want" ]; then PASS=$((PASS+1)); echo "  [PASS] rc=$got"
    else
        echo "  [FAIL] $name 期望 rc=$want 实际 rc=$got"
        sed 's/^/  stderr: /' "${rcf%.rc}.err" 2>/dev/null | head -5
        FAIL=$((FAIL+1)); FAILED_CASES+=("$name")
    fi
}

# 断言: stderr 含指定中文片段 (无堆栈)
assert_err_contains() {
    local name="$1" errf="$2" want="$3"
    if ! assert_ready "$errf"; then return; fi
    if grep -qF "$want" "$errf"; then
        PASS=$((PASS+1)); echo "  [PASS] 提示含: $want"
    else
        echo "  [FAIL] $name stderr 缺少: $want"; sed 's/^/  stderr: /' "$errf" | head -8
        FAIL=$((FAIL+1)); FAILED_CASES+=("$name")
    fi
}

# 断言: stdout JSON 的 data 字段存在
assert_json_field() {
    local name="$1" file="$2" pyexpr="$3" label="$4"
    if ! assert_ready "$file"; then return; fi
    python3 -c "
import json
d=json.load(open('$file'))
assert d.get('code')==200
$pyexpr
print('  [PASS] $label')
" 2>/dev/null && { PASS=$((PASS+1)); return; }
    echo "  [FAIL] $name 断言失败: $label"
    FAIL=$((FAIL+1)); FAILED_CASES+=("$name")
}

# 断言: 输出包含指定视角 (JSON 或 prompt)
assert_perspectives() {
    local name="$1" file="$2" want="$3"
    if ! assert_ready "$file"; then return; fi
    python3 -c "
import json
d=json.load(open('$file'))
assert d.get('code')==200, 'code=%s %s' % (d.get('code'),d.get('message'))
got=set(d['data']['perspectives'].keys())
want=set('$want'.split(','))
assert want<=got, '缺少视角: %s (got %s)' % (want-got,got)
print('  [PASS] 视角齐备: %s' % sorted(got))
" 2>/dev/null && { PASS=$((PASS+1)); return; }
    echo "  [FAIL] $name 缺少视角: $want"
    FAIL=$((FAIL+1)); FAILED_CASES+=("$name")
}

# 断言: 输出含 dataVersionAnchors (FR-VER-001, map/array 双形态兼容)
assert_anchors() {
    local name="$1" file="$2"
    if ! assert_ready "$file"; then return; fi
    python3 -c "
import json
d=json.load(open('$file'))
assert d.get('code')==200
cm=d['data'].get('contextMeta') or d['data'].get('context_meta') or {}
a=cm.get('dataVersionAnchors') or cm.get('data_version_anchors') or {}
assert len(a)>0, '缺版本锚'
print('  [PASS] 版本锚存在')
" 2>/dev/null && { PASS=$((PASS+1)); return; }
    echo "  [FAIL] $name 缺版本锚"
    FAIL=$((FAIL+1)); FAILED_CASES+=("$name")
}

# ============================================================================
# 数据锚点 (cognition-agent-seed.sql 提供, 与 cognition-agent-test.sh 一致)
# ============================================================================
META="metaforge:1.0.0"
ORDER="order:1.0.0"
STEP_CHECK="order:1.0.0.Step_CheckInventory"
STEP_PAY="order:1.0.0.Step_CheckPayment"
TASK_ORDER="metaforge:1.0.0.agent.Task_OrderFulfillment"
DOMAIN_ORDER="metaforge:1.0.0.common.Domain_Order"

# ============================================================================
# V1 开发态环境 (.metaforge/)
# ============================================================================
echo; echo "======================================================"
echo " V1 开发态环境 (.metaforge/)"
echo "======================================================"

if should_run "V1"; then
    printf '\n\033[1;36m=== V1 env root (META_FORGE_ROOT) ===\033[0m\n'
    META_FORGE_ROOT="$REPO_ROOT" "$MF_CLI" env root > "$OUT_DIR/V1_env_root.out" 2> "$OUT_DIR/V1_env_root.err"
    echo "$?" > "$OUT_DIR/V1_env_root.rc"
    root="$(cat "$OUT_DIR/V1_env_root.out")"
    if [ -n "$root" ] && [ -d "$root" ]; then PASS=$((PASS+1)); echo "  [PASS] env root=$root"
    else echo "  [FAIL] V1 env root 输出无效"; sed 's/^/  stderr: /' "$OUT_DIR/V1_env_root.err"; FAIL=$((FAIL+1)); FAILED_CASES+=("V1 env root"); fi
fi

if run_cli "V1 env summary" env summary; then
    if assert_ready "$OUT_DIR/V1_env_summary.out"; then
        if grep -q "META_FORGE_SERVER_URL=" "$OUT_DIR/V1_env_summary.out" \
           && grep -q "META_FORGE_CONNECT_MS=" "$OUT_DIR/V1_env_summary.out" \
           && grep -q "META_FORGE_TIMEOUT_MS=" "$OUT_DIR/V1_env_summary.out"; then
            PASS=$((PASS+1)); echo "  [PASS] env summary key=value"
        else echo "  [FAIL] V1 env summary 缺 key"; FAIL=$((FAIL+1)); FAILED_CASES+=("V1 env summary"); fi
    fi
fi

if run_cli "V1 health" health; then
    if assert_ready "$OUT_DIR/V1_health.out"; then
        if grep -q "HEALTH OK" "$OUT_DIR/V1_health.out"; then
            PASS=$((PASS+1)); echo "  [PASS] HEALTH OK"
        else echo "  [FAIL] V1 health 非 OK"; FAIL=$((FAIL+1)); FAILED_CASES+=("V1 health"); fi
    fi
fi

# ============================================================================
# V2 模板注册表投影 (FR-CAP-002)
# ============================================================================
echo; echo "======================================================"
echo " V2 模板注册表投影"
echo "======================================================"

if run_cli "V2 templates" cognition templates; then
    if assert_ready "$OUT_DIR/V2_templates.out"; then
        TPL="$(cat "$OUT_DIR/V2_templates.out")"
        missing=0
        for t in bundle-catalog cognition-guidance task-brief step-guide navigate sub-task-brief; do
            echo "$TPL" | grep -qx "$t" || { missing=1; echo "  缺模板: $t"; }
        done
        if [ "$missing" -eq 0 ]; then
            PASS=$((PASS+1)); echo "  [PASS] 6 模板齐备"
        else echo "  [FAIL] V2 模板清单不齐"; FAIL=$((FAIL+1)); FAILED_CASES+=("V2 templates"); fi
    fi
fi

if run_cli "V2 templates --verify" cognition templates --verify; then
    if assert_ready "$OUT_DIR/V2_templates_--verify.out"; then
        if grep -q "（已注册）" "$OUT_DIR/V2_templates_--verify.out"; then
            PASS=$((PASS+1)); echo "  [PASS] 模板探测已注册"
        else echo "  [FAIL] V2 --verify 无注册标记"; FAIL=$((FAIL+1)); FAILED_CASES+=("V2 templates --verify"); fi
    fi
fi

# ============================================================================
# V3 平台发现 (metaforge.catalog)
# ============================================================================
echo; echo "======================================================"
echo " V3 平台发现 (bundle-catalog)"
echo "======================================================"

run_cli "V3 catalog" cognition execute bundle-catalog --bundles "$META" --format json
assert_json_ok "V3 catalog" "$OUT_DIR/V3_catalog.out"
assert_anchors "V3 catalog 版本锚" "$OUT_DIR/V3_catalog.out"
assert_json_field "V3 catalog bundle 列表" "$OUT_DIR/V3_catalog.out" \
    "pers=d['data']['perspectives']; bd=pers['bundle_directory']['bundles']; assert len(bd)>0, 'bundles 空'" \
    "Bundle 列表非空"

# 分页参数透传 (--page-size / --cursor)
run_cli "V3 catalog 分页" cognition execute bundle-catalog --bundles "$META" --page-size 10 --format json
assert_json_ok "V3 catalog 分页" "$OUT_DIR/V3_catalog_分页.out"

# ============================================================================
# V4 领域导航 (metaforge.navigate)
# ============================================================================
echo; echo "======================================================"
echo " V4 领域导航 (navigate)"
echo "======================================================"

run_cli "V4 navigate" cognition execute navigate --bundles "$ORDER" --subject-domain "$DOMAIN_ORDER" --format json
assert_json_ok "V4 navigate" "$OUT_DIR/V4_navigate.out"
assert_perspectives "V4 navigate 视角" "$OUT_DIR/V4_navigate.out" "domain_navigation"

run_cli "V4 navigate 缺 --bundles" cognition execute navigate
assert_rc "V4 navigate 缺 --bundles" "$OUT_DIR/V4_navigate_缺_--bundles.rc" "2"
assert_err_contains "V4 navigate 缺 --bundles 提示" "$OUT_DIR/V4_navigate_缺_--bundles.err" "必须指定 --bundles"

# ============================================================================
# V5 任务认知 (metaforge.task-brief)
# ============================================================================
echo; echo "======================================================"
echo " V5 任务认知 (task-brief)"
echo "======================================================"

run_cli "V5 task-brief json" cognition execute task-brief \
    --bundles "$ORDER" --depth L2 --archetype execution --max-tokens 8000 --format json
assert_json_ok "V5 task-brief json" "$OUT_DIR/V5_task-brief_json.out"
assert_anchors "V5 task-brief 版本锚" "$OUT_DIR/V5_task-brief_json.out"
assert_perspectives "V5 task-brief 视角" "$OUT_DIR/V5_task-brief_json.out" \
    "constraint_set,flow_blueprint,capability_catalog,decision_matrix,entity_profile"

run_cli "V5 task-brief prompt" cognition execute task-brief \
    --bundles "$ORDER" --depth L2 --archetype execution --max-tokens 8000 --format prompt
if assert_ready "$OUT_DIR/V5_task-brief_prompt.out"; then
    if grep -q "^#" "$OUT_DIR/V5_task-brief_prompt.out" && [ -s "$OUT_DIR/V5_task-brief_prompt.out" ]; then
        PASS=$((PASS+1)); echo "  [PASS] prompt 为 Markdown"
    else echo "  [FAIL] V5 prompt 非 Markdown/空"; FAIL=$((FAIL+1)); FAILED_CASES+=("V5 task-brief prompt"); fi
fi

run_cli "V5 task-brief 缺 --bundles" cognition execute task-brief
assert_rc "V5 task-brief 缺 --bundles" "$OUT_DIR/V5_task-brief_缺_--bundles.rc" "2"
assert_err_contains "V5 task-brief 缺 --bundles 提示" "$OUT_DIR/V5_task-brief_缺_--bundles.err" "必须指定 --bundles"

# ============================================================================
# V6 实体即时指导 (metaforge.step-guide)
# ============================================================================
echo; echo "======================================================"
echo " V6 实体即时指导 (step-guide)"
echo "======================================================"

run_cli "V6 step-guide" cognition execute step-guide --entity-fqn "$STEP_CHECK" --format json
assert_json_ok "V6 step-guide" "$OUT_DIR/V6_step-guide.out"
assert_perspectives "V6 step-guide 实体级 6 视角" "$OUT_DIR/V6_step-guide.out" \
    "entity_profile,constraint_set,capability_catalog,decision_matrix,impact_trace,relationship_graph"
assert_json_field "V6 step-guide 约束级别" "$OUT_DIR/V6_step-guide.out" \
    "cs=d['data']['perspectives']['constraint_set']['constraints']; assert isinstance(cs,list)" \
    "constraint_set 结构合法"

run_cli "V6 step-guide 缺 --entity-fqn" cognition execute step-guide
assert_rc "V6 step-guide 缺 --entity-fqn" "$OUT_DIR/V6_step-guide_缺_--entity-fqn.rc" "2"
assert_err_contains "V6 step-guide 缺 --entity-fqn 提示" "$OUT_DIR/V6_step-guide_缺_--entity-fqn.err" "必须指定 --entity-fqn"

run_cli "V6 step-guide 幽灵实体" cognition execute step-guide --entity-fqn "ghost:1.0.0.Task_X"
assert_rc "V6 step-guide 幽灵实体" "$OUT_DIR/V6_step-guide_幽灵实体.rc" "1"
assert_err_contains "V6 step-guide 34004" "$OUT_DIR/V6_step-guide_幽灵实体.err" "归属校验失败"

# ============================================================================
# V7 子任务认知 (metaforge.subtask)
# ============================================================================
echo; echo "======================================================"
echo " V7 子任务认知 (sub-task-brief)"
echo "======================================================"

run_cli "V7 subtask INHERITED" cognition execute sub-task-brief \
    --bundles "$META" --entry-entity "$TASK_ORDER" --scope-mode INHERITED --format json
assert_json_ok "V7 subtask INHERITED" "$OUT_DIR/V7_subtask_INHERITED.out"
assert_perspectives "V7 subtask INHERITED 视角" "$OUT_DIR/V7_subtask_INHERITED.out" \
    "constraint_set,capability_catalog,decision_matrix,entity_profile"

run_cli "V7 subtask PURE" cognition execute sub-task-brief \
    --bundles "$META" --entry-entity "$TASK_ORDER" --scope-mode PURE --format json
assert_json_ok "V7 subtask PURE" "$OUT_DIR/V7_subtask_PURE.out"
assert_json_field "V7 subtask PURE 仅 entity_profile" "$OUT_DIR/V7_subtask_PURE.out" \
    "pers=d['data']['perspectives']; assert 'entity_profile' in pers, '缺 entity_profile'; assert len(pers)==1, 'PURE 应仅 1 视角: %s' % list(pers)" \
    "PURE 仅返回 entity_profile"

run_cli "V7 subtask 缺参" cognition execute sub-task-brief
assert_rc "V7 subtask 缺参" "$OUT_DIR/V7_subtask_缺参.rc" "2"
assert_err_contains "V7 subtask 缺参 提示" "$OUT_DIR/V7_subtask_缺参.err" "必须指定 --bundles"

# ============================================================================
# V8 自由视角组合 (metaforge.guidance)
# ============================================================================
echo; echo "======================================================"
echo " V8 自由视角组合 (cognition-guidance)"
echo "======================================================"

run_cli "V8 guidance schema+instance" cognition execute cognition-guidance \
    --bundles "$ORDER" --perspectives schema_inventory,instance_catalog --format json
assert_json_ok "V8 guidance schema+instance" "$OUT_DIR/V8_guidance_schema+instance.out"
assert_json_field "V8 guidance 恰好返回所请求视角" "$OUT_DIR/V8_guidance_schema+instance.out" \
    "pers=set(d['data']['perspectives'].keys()); assert pers=={'schema_inventory','instance_catalog'}, '实际: %s' % pers" \
    "恰好返回 schema_inventory + instance_catalog"

run_cli "V8 guidance impact_trace" cognition execute cognition-guidance \
    --bundles "$ORDER" --entity-fqn "$STEP_CHECK" --perspectives impact_trace --format json
assert_json_ok "V8 guidance impact_trace" "$OUT_DIR/V8_guidance_impact_trace.out"
assert_perspectives "V8 guidance impact_trace 视角" "$OUT_DIR/V8_guidance_impact_trace.out" "impact_trace"

# ============================================================================
# V9 FQN 推测 (cognition resolve, FR-NL)
# ============================================================================
echo; echo "======================================================"
echo " V9 FQN 推测 (resolve)"
echo "======================================================"

# 说明: resolve 基于 bundle-catalog/navigate 候选做确定型匹配 (FR-NL-002)。
# 业务 Bundle 上料未就绪时 (R2), 服务端返回 34003 "请至少指定一个 Bundle",
# 此时本场景判定为 SKIP (需 mock 数据), 与 quickstart R2 说明一致。
run_cli "V9 resolve 唯一命中" cognition resolve "订单履约主任务"
if assert_ready "$OUT_DIR/V9_resolve_唯一命中.rc"; then
    rc="$(cat "$OUT_DIR/V9_resolve_唯一命中.rc")"
    if [ "$rc" = "0" ]; then
        out="$(cat "$OUT_DIR/V9_resolve_唯一命中.out")"
        if echo "$out" | grep -qE '^[a-zA-Z0-9:_-]+$'; then
            PASS=$((PASS+1)); echo "  [PASS] 唯一命中 FQN: $out"
        else echo "  [FAIL] V9 唯一命中输出非 FQN"; FAIL=$((FAIL+1)); FAILED_CASES+=("V9 resolve 唯一命中"); fi
    elif [ "$rc" = "1" ] && grep -qF "请至少指定一个 Bundle" "$OUT_DIR/V9_resolve_唯一命中.err"; then
        echo "  [SKIP] 唯一命中 (服务端未上料, 34003, R2 需 mock)"
    else
        echo "  [FAIL] V9 resolve 唯一命中 rc=$rc"; sed 's/^/  stderr: /' "$OUT_DIR/V9_resolve_唯一命中.err" | head -5
        FAIL=$((FAIL+1)); FAILED_CASES+=("V9 resolve 唯一命中")
    fi
fi

run_cli "V9 resolve 零命中" cognition resolve "不存在的任务xxx"
if assert_ready "$OUT_DIR/V9_resolve_零命中.rc"; then
    rc="$(cat "$OUT_DIR/V9_resolve_零命中.rc")"
    if [ "$rc" = "1" ]; then
        PASS=$((PASS+1)); echo "  [PASS] 零命中终止 rc=1"
    else
        echo "  [FAIL] V9 resolve 零命中 rc=$rc 期望 1"; sed 's/^/  stderr: /' "$OUT_DIR/V9_resolve_零命中.err" | head -5
        FAIL=$((FAIL+1)); FAILED_CASES+=("V9 resolve 零命中")
    fi
fi

# ============================================================================
# V10 错误处理与认知新鲜度 (FR-ERR/FR-VER)
# ============================================================================
echo; echo "======================================================"
echo " V10 错误处理与认知新鲜度"
echo "======================================================"

# 34001 无效模板
run_cli "V10 无效模板 34001" cognition execute not-a-template --bundles "$ORDER"
assert_rc "V10 无效模板 34001" "$OUT_DIR/V10_无效模板_34001.rc" "1"
assert_err_contains "V10 34001 中文提示" "$OUT_DIR/V10_无效模板_34001.err" "不存在，请检查模板 ID"

# 服务不可达 → 网络错误中文提示 (含地址)
META_FORGE_SERVER_URL="http://localhost:9999" bash "$MF_CLI" health > "$OUT_DIR/V10_网络错误.out" 2> "$OUT_DIR/V10_网络错误.err"
echo "$?" > "$OUT_DIR/V10_网络错误.rc"
echo "  [INFO] 网络错误: rc=$(cat "$OUT_DIR/V10_网络错误.rc")"
assert_rc "V10 服务不可达" "$OUT_DIR/V10_网络错误.rc" "1"
assert_err_contains "V10 服务不可达 中文提示" "$OUT_DIR/V10_网络错误.err" "无法连接 MetaForge 服务端"

# 版本锚 (FR-VER-001): 每次成功输出含版本锚, 已在前置场景 V3/V5 覆盖
# 新鲜度对比 (FR-VER-002): 两次查询版本锚一致 → 无过期提示 (对比由调用方完成)
if assert_ready "$OUT_DIR/V5_task-brief_json.out" && assert_ready "$OUT_DIR/V3_catalog.out"; then
    python3 -c "
import json
a=json.load(open('$OUT_DIR/V3_catalog.out'))
b=json.load(open('$OUT_DIR/V5_task-brief_json.out'))
def anchors(d):
    cm=d['data'].get('contextMeta') or d['data'].get('context_meta') or {}
    return cm.get('dataVersionAnchors') or cm.get('data_version_anchors') or {}
ka=set(x.get('bundleFqn') for x in anchors(a))
kb=set(x.get('bundleFqn') for x in anchors(b))
assert ka and kb, '锚点缺失'
print('  [PASS] 版本锚跨查询存在:', sorted(ka|kb))
" 2>/dev/null && { PASS=$((PASS+1)); } || { echo "  [FAIL] V10 版本锚对比"; FAIL=$((FAIL+1)); FAILED_CASES+=("V10 版本锚对比"); }
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
