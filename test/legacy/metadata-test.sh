#!/usr/bin/env bash
# =============================================================================
# MetaForge metadata BC 全生命周期端到端测试脚本
#
# 设计依据: version/specs/001-metadata-full-lifecycle/quickstart.md
#   （场景 1-7 + 一致性 + 性能），并针对当前实现的实际 REST 端点编写。
#
# 特性:
#   * 不硬编码 —— 端口/容器/jar/EntitySchema FQN/批量大小等全部来自环境变量或
#     运行时探测；测试内容由「已发布 EntitySchema 的 jsonSchema」动态生成
#     （schema-driven），自动满足 type / pattern / enum / minimum / maxLength 等约束。
#   * 数据复杂度 —— 多级父子 FQN 层级、多版本、混合属性类型与约束、批量导入、
#     版本差异对比。
#   * 韧性验证 —— jar 重启后数据持久性；PostgreSQL 重启后应用自动重连与恢复。
#
# 前置: metaforge-boot 已构建；PostgreSQL 容器运行中；有 bash/curl/python3/docker(或 podman)
#
# 端点映射（version/specs 文档 → 当前实现）:
#   POST /metadata/drafts                             -> POST /api/v1/metadata/drafts
#   GET  /metadata/entities?prefix=                   -> GET  /api/v1/metadata/entities/query/fqn-prefix?prefixes=
#   GET  /metadata/entities?schemaType=               -> GET  /api/v1/metadata/entities/query/entity-schema?entitySchemaFqn=
#   DELETE /metadata/entities/{fqn}                   -> POST /api/v1/metadata/entities/{fqn}/deactivate
#   POST /metadata/entities/{fqn}/reactivate          -> POST /api/v1/metadata/entities/{fqn}/reactivate
#   GET  /metadata/entities/{fqn}/versions            -> GET  /api/v1/metadata/history/{fqn}/versions
#   GET  .../versions/compare?v1&v2                   -> POST /api/v1/metadata/history/diff
#   POST /metadata/import (multipart)                 -> POST /api/v1/metadata/import (JSON body)
#   POST /metadata/export {PREFIX}                    -> POST /api/v1/metadata/export {type:FQN_PREFIXES}
#   GET  /metadata/admin/entities                     -> GET  /api/v1/metadata/admin/metadata
#   POST /metadata/validate/batch                     -> POST /api/v1/metadata/validate-batch
#
# 用法:
#   ./metadata-test.sh                       全量运行
#   MF_SKIP_JAR_RESTART=1 ./metadata-test.sh  跳过 jar 重启
#   MF_SKIP_PG_RESTART=1  ./metadata-test.sh  跳过 PostgreSQL 重启
#   MF_RUN_PERF=1 MF_PERF_DRAFT_COUNT=50 ./metadata-test.sh  含性能基准
#
# 可用环境变量（均可选）:
#   MF_PORT / MF_BASE_URL / MF_CONTAINER_RUNTIME / MF_PG_CONTAINER / MF_WAIT_TIMEOUT
#   MF_PARENT_DIR / MF_JAR_PATH / MF_JAVA_CMD / MF_JAVA_OPTS / MF_APP_LOG / MF_PID_FILE
#   MF_BUNDLE_CODE / MF_PKG_SEGMENT / MF_ENTITY_SEGMENT / MF_ENTITY_SCHEMA_FQN
#   MF_IMPORT_BATCH_SIZE / MF_RUN_PERF / MF_PERF_DRAFT_COUNT / MF_PERF_QUERY_COUNT / MF_PERF_BATCH_COUNT
#   MF_PERF_DRAFT_MS / MF_PERF_QUERY_MS / MF_PERF_IMPORT_MS（性能阈值）
#   MF_SKIP_JAR_RESTART / MF_SKIP_PG_RESTART
# =============================================================================

set -u

# ---------------------------------------------------------------- 基础配置
MF_PORT="${MF_PORT:-8080}"
MF_BASE_URL="${MF_BASE_URL:-http://localhost:${MF_PORT}}"
MF_CONTAINER_RUNTIME="${MF_CONTAINER_RUNTIME:-docker}"
MF_PG_CONTAINER="${MF_PG_CONTAINER:-metaforge-postgres}"
MF_WAIT_TIMEOUT="${MF_WAIT_TIMEOUT:-120}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MF_PARENT_DIR="${MF_PARENT_DIR:-$(cd "${SCRIPT_DIR}/../../.." && pwd)}"
MF_TMPDIR="$(mktemp -d)"
trap 'rm -rf "$MF_TMPDIR"' EXIT

# ---------------------------------------------------------------- 元模型前置参数
MF_BUNDLE_CODE="${MF_BUNDLE_CODE:-mf-e2e}"
MF_PKG_SEGMENT="${MF_PKG_SEGMENT:-pkg_e2e}"
MF_ENTITY_SEGMENT="${MF_ENTITY_SEGMENT:-Order}"
MF_ENTITY_SCHEMA_FQN="${MF_ENTITY_SCHEMA_FQN:-}"

# ---------------------------------------------------------------- 运行标识与测试数据
MF_RUN_ID="${MF_RUN_ID:-$(date +%s)}"
# 合法 segment: [A-Za-z][A-Za-z0-9_-]*
PREFIX="E2E${MF_RUN_ID}"
MASTER_FQN="${PREFIX}_ORD_MASTER"
LINE1_FQN="${PREFIX}_ORD_MASTER.LINE_001"
LINE2_FQN="${PREFIX}_ORD_MASTER.LINE_002"
SUB1_FQN="${PREFIX}_ORD_MASTER.LINE_001.SUB_001"
RPT_FQN="${PREFIX}_RPT_100"

MF_IMPORT_BATCH_SIZE="${MF_IMPORT_BATCH_SIZE:-5}"
MF_SKIP_JAR_RESTART="${MF_SKIP_JAR_RESTART:-0}"
MF_SKIP_PG_RESTART="${MF_SKIP_PG_RESTART:-0}"
MF_RUN_PERF="${MF_RUN_PERF:-0}"
MF_PERF_DRAFT_COUNT="${MF_PERF_DRAFT_COUNT:-30}"
MF_PERF_QUERY_COUNT="${MF_PERF_QUERY_COUNT:-100}"
MF_PERF_BATCH_COUNT="${MF_PERF_BATCH_COUNT:-200}"
# 性能阈值（ms，默认对应 quickstart 性能基准目标，可按环境调整）
MF_PERF_DRAFT_MS="${MF_PERF_DRAFT_MS:-50}"
MF_PERF_QUERY_MS="${MF_PERF_QUERY_MS:-20}"
MF_PERF_IMPORT_MS="${MF_PERF_IMPORT_MS:-5000}"

# ---------------------------------------------------------------- jar / 应用
MF_JAVA_CMD="${MF_JAVA_CMD:-java}"
MF_JAVA_OPTS="${MF_JAVA_OPTS:-}"
MF_APP_LOG="${MF_APP_LOG:-${MF_TMPDIR}/app.log}"
MF_PID_FILE="${MF_PID_FILE:-${MF_TMPDIR}/app.pid}"
if [ -z "${MF_JAR_PATH:-}" ]; then
  MF_JAR_PATH="$(ls -1 "${MF_PARENT_DIR}"/metaforge-boot/target/metaforge-boot-*.jar 2>/dev/null | grep -v -- '-sources' | head -1 || true)"
fi

# ---------------------------------------------------------------- 统计
PASS=0
FAIL=0
FAILURES=()

log()   { printf '\033[1;34m[%s]\033[0m %s\n' "$1" "$2"; }
ok()    { PASS=$((PASS+1)); printf '\033[1;32m  ✓\033[0m %s (%s)\n' "$1" "$2"; }
bad()   { FAIL=$((FAIL+1)); FAILURES+=("$1: got=$2 want=$3"); printf '\033[1;31m  ✗\033[0m %s (got=%s want=%s)\n' "$1" "$2" "$3"; }
check() { if [ "$2" = "$3" ]; then ok "$1" "$2"; else bad "$1" "$2" "$3"; fi; }
# check_contains <desc> <haystack> <needle>
check_contains() { case "$2" in *"$3"*) ok "$1" "$3";; *) bad "$1" "<$2>" "contains $3";; esac; }

# ---------------------------------------------------------------- JSON 提取（python3，不依赖 jq）
# peek <python-expr>   — JSON 从 stdin 读入，eval 结果输出，异常输出空串
peek() {
  python3 -c '
import sys, json
try:
    d = json.loads(sys.stdin.read())
except Exception:
    d = None
try:
    v = eval(sys.argv[1])
    print(v if isinstance(v, (int, float)) or v is None else (json.dumps(v, ensure_ascii=False) if not isinstance(v, str) else v))
except Exception:
    print("")
' "$1"
}
code_of() { peek "d.get('code')" <<< "$1"; }
msg_of()  { peek "d.get('message')" <<< "$1"; }
data_fld(){ peek "d.get('data',{}).get('$2')" <<< "$1"; }

# ---------------------------------------------------------------- HTTP
api() { # api <METHOD> <path> [json-body]
  local m="$1" p="$2" b="${3:-}"
  if [ -n "$b" ]; then
    curl -s -X "$m" "${MF_BASE_URL}${p}" -H "Content-Type: application/json" -d "$b"
  else
    curl -s -X "$m" "${MF_BASE_URL}${p}"
  fi
}

# ---------------------------------------------------------------- 健康/等待
http_ok() { curl -s -o /dev/null -w '%{http_code}' "${MF_BASE_URL}$1" 2>/dev/null | grep -q '200'; }
wait_app_up() {
  local i=0 t="$((MF_WAIT_TIMEOUT * 2))"
  while [ $i -lt "$t" ]; do http_ok /actuator/health && return 0; sleep 0.5; i=$((i+1)); done
  return 1
}
wait_app_down() {
  local i=0 t="$((MF_WAIT_TIMEOUT * 2))"
  while [ $i -lt "$t" ]; do http_ok /actuator/health || return 0; sleep 0.5; i=$((i+1)); done
  return 1
}
wait_pg_healthy() {
  local i=0 t="$((MF_WAIT_TIMEOUT * 2))"
  while [ $i -lt "$t" ]; do
    local st
    st="$("$MF_CONTAINER_RUNTIME" inspect -f '{{.State.Health.Status}}' "$MF_PG_CONTAINER" 2>/dev/null || echo starting)"
    [ "$st" = "healthy" ] && return 0
    sleep 1; i=$((i+1))
  done
  return 1
}

# ---------------------------------------------------------------- 应用生命周期
app_pid() { pgrep -f "${MF_JAR_PATH}" 2>/dev/null | head -1; }
stop_app() {
  local pid
  pid="$(app_pid)"
  if [ -n "$pid" ]; then
    log INFO "停止应用 pid=$pid"
    kill "$pid" 2>/dev/null || true
    local i=0; while [ $i -lt 60 ] && kill -0 "$pid" 2>/dev/null; do sleep 1; i=$((i+1)); done
    kill -9 "$pid" 2>/dev/null || true
    wait_app_down || true
  fi
}
start_app() {
  log INFO "启动应用: $MF_JAVA_CMD $MF_JAVA_OPTS -jar $MF_JAR_PATH"
  nohup "$MF_JAVA_CMD" $MF_JAVA_OPTS -jar "$MF_JAR_PATH" > "$MF_APP_LOG" 2>&1 &
  echo $! > "$MF_PID_FILE"
  wait_app_up
}

# =============================================================================
# 元模型前置: 未提供 MF_ENTITY_SCHEMA_FQN 时，动态创建并发布一个复杂 EntitySchema
# =============================================================================
setup_metamodel() {
  if [ -n "$MF_ENTITY_SCHEMA_FQN" ]; then
    log INFO "使用已提供的 EntitySchema: $MF_ENTITY_SCHEMA_FQN"
    return 0
  fi

  local bundle="${MF_BUNDLE_CODE}-${MF_RUN_ID}"
  local pkg="${MF_PKG_SEGMENT}${MF_RUN_ID}"
  local bver="${bundle}:0.0.1"
  MF_ENTITY_SCHEMA_FQN="${bver}.${pkg}.${MF_ENTITY_SEGMENT}"

  log INFO "动态创建元模型: bundle=$bundle schema=$MF_ENTITY_SCHEMA_FQN"

  local r
  r="$(api POST /api/v1/metamodel/bundles "{\"fqn\":\"${bundle}\",\"name\":\"E2E ${MF_RUN_ID}\",\"description\":\"metadata e2e fixture\",\"owner\":\"e2e\"}")"
  [ "$(code_of "$r")" = "200" ] || { log ERR "创建 bundle 失败: $r"; return 1; }

  r="$(api POST /api/v1/metamodel/packages "{\"bundleVersionFqn\":\"${bver}\",\"parentPackageFqn\":null,\"segment\":\"${pkg}\",\"description\":\"e2e namespace\"}")"
  [ "$(code_of "$r")" = "200" ] || { log ERR "创建 package 失败: $r"; return 1; }

  # 复杂属性集: 混合类型 + 约束（pattern/enum/minimum/maximum/maxLength/required）
  local attrs
  attrs="$(python3 <<'PY'
import json
attrs = [
 {"name":"orderId","type":"string","required":True,"description":"订单号","constraints":{"pattern":"^SO-\\d{6,}$"}},
 {"name":"customerName","type":"string","required":True,"description":"客户名称"},
 {"name":"status","type":"string","required":True,"description":"订单状态","constraints":{"enum":["created","paid","shipped","cancelled"]}},
 {"name":"amount","type":"number","description":"金额","constraints":{"minimum":0,"maximum":1000000}},
 {"name":"quantity","type":"integer","required":True,"description":"数量","constraints":{"minimum":1,"maximum":10000}},
 {"name":"priority","type":"string","description":"优先级","constraints":{"enum":["high","normal","low"]}},
 {"name":"remark","type":"string","description":"备注","constraints":{"maxLength":200}},
 {"name":"isVip","type":"boolean","description":"VIP 标记"},
]
print(json.dumps(attrs, ensure_ascii=False))
PY
)"
  [ -n "$attrs" ] || { log ERR "生成属性定义失败"; return 1; }

  r="$(api POST /api/v1/metamodel/entity-schemas "{\"packageFqn\":\"${bver}.${pkg}\",\"segment\":\"${MF_ENTITY_SEGMENT}\",\"name\":\"${MF_ENTITY_SEGMENT}\",\"description\":\"e2e entity\",\"nativeAttributes\":${attrs},\"mountedTemplateFqns\":[]}")"
  [ "$(code_of "$r")" = "200" ] || { log ERR "创建 EntitySchema 失败: $r"; return 1; }

  r="$(api PUT "/api/v1/metamodel/versions/${bver}/export-manifest" "{\"packageFqns\":[\"${bver}.${pkg}\"]}")"
  [ "$(code_of "$r")" = "200" ] || { log ERR "配置导出清单失败: $r"; return 1; }

  r="$(api POST "/api/v1/metamodel/versions/${bver}/publish")"
  [ "$(code_of "$r")" = "200" ] || { log ERR "发布版本失败: $r"; return 1; }

  log OK "元模型前置就绪: schema=$MF_ENTITY_SCHEMA_FQN (PUBLISHED)"
}

# 拉取已发布 jsonSchema 供内容生成
fetch_schema() {
  local r
  r="$(api GET "/api/v1/metamodel/entity-schemas/${MF_ENTITY_SCHEMA_FQN}")"
  peek "d.get('data',{}).get('jsonSchema')" <<< "$r" > "$MF_TMPDIR/schema.json"
  [ -s "$MF_TMPDIR/schema.json" ] || { log ERR "获取 jsonSchema 失败: $r"; return 1; }
  log INFO "已获取 jsonSchema -> $MF_TMPDIR/schema.json"
}

# schema-driven 内容生成器
gen() { python3 "$MF_TMPDIR/gen.py" "$1" "$MF_TMPDIR/schema.json" "$MF_RUN_ID" "${2:-}" "${3:-}"; }

setup_generator() {
  cat > "$MF_TMPDIR/gen.py" <<'PY'
#!/usr/bin/env python3
import json, os, random, re, string, sys

mode = sys.argv[1]
schema = json.load(open(sys.argv[2]))
seed = int(sys.argv[3])
rng = random.Random(seed)
count = int(sys.argv[4]) if len(sys.argv) > 4 and sys.argv[4].strip() else 1
prefix = sys.argv[5] if len(sys.argv) > 5 else ""
schema_fqn = os.environ.get("MF_ENTITY_SCHEMA_FQN", "")

def expand_pattern(pat):
    s = pat.strip()
    if s.startswith("^"): s = s[1:]
    if s.endswith("$"): s = s[:-1]
    out, i, n = [], 0, len(s)
    def quant(j):
        if j >= n: return 1, 1, 0
        if s[j] == "{":
            m = re.match(r"\{\s*(\d+)\s*(?:,\s*(\d*)\s*)?\}", s[j:])
            if not m: return 1, 1, 0
            lo, hi = int(m.group(1)), (int(m.group(2)) if m.group(2) else int(m.group(1)))
            return lo, max(hi, lo), len(m.group(0))
        if s[j] == "?": return 0, 1, 1
        if s[j] == "*": return 0, 3, 1
        if s[j] == "+": return 1, 3, 1
        return 1, 1, 0
    def repeat(fn, lo, hi):
        return "".join(fn() for _ in range(rng.randint(lo, hi)))
    def esc(e):
        if e == "d": return lambda: rng.choice(string.digits)
        if e == "D": return lambda: rng.choice(string.ascii_uppercase)
        if e == "w": return lambda: rng.choice(string.ascii_letters + string.digits + "_")
        if e == "s": return lambda: " "
        return lambda e=e: e
    while i < n:
        c = s[i]
        if c == "\\" and i + 1 < n:
            fn = esc(s[i + 1]); i += 2
            lo, hi, ql = quant(i); i += ql
            out.append(repeat(fn, lo, hi))
        elif c == "[":
            j = s.find("]", i)
            if j == -1:
                out.append(c); i += 1; continue
            cls = s[i + 1:j]
            if cls.startswith("^"): cls = cls[1:]
            chars, k = [], 0
            while k < len(cls):
                if cls[k] == "\\" and k + 1 < len(cls):
                    e = cls[k + 1]
                    chars.extend(string.digits if e == "d" else (string.ascii_letters + string.digits if e == "w" else [e]))
                    k += 2
                elif k + 1 < len(cls) and cls[k + 1] == "-" and k + 2 < len(cls):
                    chars.extend(chr(x) for x in range(ord(cls[k]), ord(cls[k + 2]) + 1))
                    k += 3
                else:
                    chars.append(cls[k]); k += 1
            i = j + 1
            lo, hi, ql = quant(i); i += ql
            if not chars: chars = list(string.ascii_letters + string.digits)
            pool = [ch for ch in chars if ch in string.ascii_letters + string.digits + "_"] or chars
            out.append("".join(rng.choice(pool) for _ in range(rng.randint(lo, hi))))
        elif c == "\\":
            out.append("\\"); i += 1
        elif c in "()|^$?*+":
            i += 1
        elif c == "{":
            m = re.match(r"\{[^}]*\}", s[i:])
            i += len(m.group(0)) if m else 1
        else:
            out.append(c); i += 1
    return "".join(out)

def gen_value(ps):
    t = ps.get("type", "string")
    if t == "boolean": return rng.choice([True, False])
    if t in ("number", "integer"):
        lo = ps.get("minimum", 1)
        hi = ps.get("maximum", 1000)
        if isinstance(lo, (int, float)) and isinstance(hi, (int, float)) and lo > hi: hi = lo
        if t == "integer": return rng.randint(int(lo), int(hi))
        return round(rng.uniform(float(lo), float(hi)), 2)
    if t == "array":
        items = ps.get("items")
        return [gen_value(items) if items else {"k": rng.randint(1, 9)} for _ in range(rng.randint(1, 2))]
    if t == "object":
        return {k: gen_value(v) for k, v in (ps.get("properties") or {}).items()}
    if ps.get("enum"): return rng.choice(ps["enum"])
    if ps.get("pattern"): return expand_pattern(ps["pattern"])
    lo = int(ps.get("minLength", 1)); hi = int(ps.get("maxLength", 32))
    if hi < lo: hi = lo
    return "".join(rng.choice(string.ascii_lowercase) for _ in range(rng.randint(lo, hi)))

def gen_content(omit=None, mutate=None):
    props = schema.get("properties", {})
    required = set(schema.get("required", []))
    content = {}
    for name, ps in props.items():
        if name == omit:
            continue
        if name in required or rng.random() < 0.7:
            content[name] = gen_value(ps)
    if mutate and mutate in props:
        content[mutate] = "INVALID__"
    return content

def draft_body(fqn, content, desc=None):
    return {"fqn": fqn, "name": fqn, "description": desc or ("e2e " + fqn),
            "entitySchemaFqn": schema_fqn, "content": content}

if mode == "content":
    print(json.dumps(gen_content(), ensure_ascii=False))
elif mode == "content2":
    rng.seed(seed + 1000)
    print(json.dumps(gen_content(), ensure_ascii=False))
elif mode == "invalid-missing":
    req = schema.get("required", [])
    print(json.dumps(gen_content(omit=req[0] if req else None), ensure_ascii=False))
elif mode == "invalid-pattern":
    target = next((n for n, p in schema.get("properties", {}).items() if p.get("pattern")), None)
    print(json.dumps(gen_content(mutate=target), ensure_ascii=False))
elif mode == "items":
    items = []
    for i in range(1, count + 1):
        fqn = "%s_IMP_%03d" % (prefix, i)
        items.append(draft_body(fqn, gen_content()))
    print(json.dumps(items, ensure_ascii=False))
else:
    print("{}")
PY
}

draft_body() { # draft_body <fqn> <content-json>
  python3 -c '
import json, sys
fqn = sys.argv[1]; content = json.loads(sys.argv[2]); schema = sys.argv[3]
print(json.dumps({"fqn": fqn, "name": fqn, "description": "e2e " + fqn,
                  "entitySchemaFqn": schema, "content": content}, ensure_ascii=False))
' "$1" "$2" "$MF_ENTITY_SCHEMA_FQN"
}

import_body() { # import_body <items-json-array>  — content 字段为 JSON 字符串
  python3 -c 'import json,sys;print(json.dumps({"content": sys.stdin.read(), "format": "JSON", "strategy": "SKIP"}))' <<< "$1"
}

# =============================================================================
# 场景 1: 草稿创建 → 结构校验 → 生效（端到端核心链路）
# =============================================================================
scenario1_core() {
  log INFO "场景 1: 草稿创建 → 结构校验 → 生效"
  local r content invalid_missing invalid_pattern dup

  content="$(gen content)"
  r="$(api POST /api/v1/metadata/drafts "$(draft_body "$MASTER_FQN" "$content")")"
  check "1.1 创建合法草稿 code" "$(code_of "$r")" "200"
  check "1.1 草稿 fqn" "$(data_fld "$r" fqn)" "$MASTER_FQN"
  check "1.1 baseVersion 为空" "$(data_fld "$r" baseVersion)" "None"

  invalid_missing="$(gen invalid-missing)"
  r="$(api POST /api/v1/metadata/drafts "$(draft_body "${PREFIX}_BAD_MISSING" "$invalid_missing")")"
  check "1.2 缺必填被拦截 code" "$(code_of "$r")" "31003"

  invalid_pattern="$(gen invalid-pattern)"
  r="$(api POST /api/v1/metadata/drafts "$(draft_body "${PREFIX}_BAD_PATTERN" "$invalid_pattern")")"
  check "1.2 pattern 违规 code" "$(code_of "$r")" "31003"
  check "1.2 违规类型 pattern" "$(peek "next((x.get('violationType') for x in (d.get('data') or []) if x.get('violationType')), '')" <<< "$r")" "pattern"

  dup="$(gen content2)"
  r="$(api POST /api/v1/metadata/drafts "$(draft_body "$MASTER_FQN" "$dup")")"
  check "1.3 FQN 重复 code" "$(code_of "$r")" "31001"
  check_contains "1.3 冲突消息" "$(msg_of "$r")" "$MASTER_FQN"

  r="$(api POST "/api/v1/metadata/entities/${MASTER_FQN}/activate")"
  check "1.4 生效 code" "$(code_of "$r")" "200"
  check "1.4 currentVersion=1" "$(data_fld "$r" currentVersion)" "1"

  r="$(api GET "/api/v1/metadata/entities/${MASTER_FQN}")"
  check "1.5 主表存在" "$(code_of "$r")" "200"

  r="$(api GET "/api/v1/metadata/history/${MASTER_FQN}/versions")"
  check "1.6 历史归档 1 条" "$(peek "d.get('data',{}).get('total')" <<< "$r")" "1"

  r="$(api GET "/api/v1/metadata/drafts/${MASTER_FQN}")"
  check "1.7 草稿已清除 code" "$(code_of "$r")" "31005"
}

# =============================================================================
# 场景 2: 多维度查询
# =============================================================================
scenario2_query() {
  log INFO "场景 2: 多维度查询"
  local r fqns total

  r="$(api GET "/api/v1/metadata/entities/${MASTER_FQN}")"
  check "2.1 FQN 精准查询" "$(code_of "$r")" "200"
  check "2.1 返回 entitySchemaFqn" "$(data_fld "$r" entitySchemaFqn)" "$MF_ENTITY_SCHEMA_FQN"

  r="$(api GET "/api/v1/metadata/entities/query/fqn-prefix?prefixes=${PREFIX}_ORD,${PREFIX}_RPT&page=1&size=20")"
  fqns="$(peek "[x['fqn'] for x in (d.get('data',{}).get('content') or [])]" <<< "$r")"
  check_contains "2.2 前缀查询含 master" "$fqns" "$MASTER_FQN"
  check_contains "2.2 前缀查询含 line1" "$fqns" "$LINE1_FQN"
  check_contains "2.2 前缀查询含 line2" "$fqns" "$LINE2_FQN"
  check_contains "2.2 前缀查询含 sub1" "$fqns" "$SUB1_FQN"
  check_contains "2.2 前缀查询含 rpt" "$fqns" "$RPT_FQN"

  r="$(api GET "/api/v1/metadata/entities/query/entity-schema?entitySchemaFqn=${MF_ENTITY_SCHEMA_FQN}&page=1&size=50")"
  total="$(peek "d.get('data',{}).get('total')" <<< "$r")"
  [ "${total:-0}" -ge 5 ] 2>/dev/null && ok "2.3 类型查询 ≥5" "$total" ">=5" || bad "2.3 类型查询 ≥5" "$total" ">=5"
}

# =============================================================================
# 场景 3: 下线 → 重新生效
# =============================================================================
scenario3_deactivate() {
  log INFO "场景 3: 下线 → 重新生效"
  local r can

  r="$(api GET "/api/v1/metadata/entities/${MASTER_FQN}/deactivation-check")"
  can="$(peek "d.get('data',{}).get('canDeactivate')" <<< "$r")"
  check "3.1 父实体引用校验 canDeactivate=false" "$can" "False"

  r="$(api POST "/api/v1/metadata/entities/${MASTER_FQN}/deactivate")"
  check "3.2 父实体下线被拦截 code" "$(code_of "$r")" "31008"

  r="$(api POST "/api/v1/metadata/entities/${LINE2_FQN}/deactivate")"
  check "3.3 叶节点下线 code" "$(code_of "$r")" "200"

  r="$(api GET "/api/v1/metadata/entities/${LINE2_FQN}")"
  check "3.4 下线后查询 code" "$(code_of "$r")" "31004"

  r="$(api POST "/api/v1/metadata/entities/${LINE2_FQN}/reactivate")"
  check "3.5 重新生效 code" "$(code_of "$r")" "200"
  check "3.5 currentVersion 保持" "$(data_fld "$r" currentVersion)" "1"

  r="$(api GET "/api/v1/metadata/entities/${LINE2_FQN}")"
  check "3.6 主表恢复" "$(code_of "$r")" "200"
}

# =============================================================================
# 场景 4: 历史版本追溯与差异对比
# =============================================================================
scenario4_history() {
  log INFO "场景 4: 历史版本追溯与差异对比"
  local r v2content

  # 从生效版本派生修改草稿 → 生效 → version 2
  r="$(api POST "/api/v1/metadata/drafts/from-active/${MASTER_FQN}")"
  check "4.1 从生效版本创建草稿" "$(code_of "$r")" "200"
  v2content="$(gen content2)"
  r="$(api PUT "/api/v1/metadata/drafts/${MASTER_FQN}/content" "{\"content\":${v2content}}")"
  check "4.1 更新草稿内容" "$(code_of "$r")" "200"
  r="$(api POST "/api/v1/metadata/entities/${MASTER_FQN}/activate")"
  check "4.1 第二次生效" "$(code_of "$r")" "200"
  check "4.1 currentVersion=2" "$(data_fld "$r" currentVersion)" "2"

  r="$(api GET "/api/v1/metadata/history/${MASTER_FQN}/versions")"
  check "4.2 版本列表 total=2" "$(peek "d.get('data',{}).get('total')" <<< "$r")" "2"
  check "4.2 按版本倒序" "$(peek "[x['version'] for x in (d.get('data',{}).get('content') or [])]" <<< "$r")" "[2, 1]"

  r="$(api GET "/api/v1/metadata/history/${MASTER_FQN}/versions/1")"
  check "4.3 单版本详情 code" "$(code_of "$r")" "200"
  check "4.3 版本号" "$(peek "d.get('data',{}).get('version')" <<< "$r")" "1"

  r="$(api POST /api/v1/metadata/history/diff "{\"fqn\":\"${MASTER_FQN}\",\"versionA\":1,\"versionB\":2}")"
  local diffs dtypes
  diffs="$(peek "len(d.get('data',{}).get('diffs') or [])" <<< "$r")"
  [ "${diffs:-0}" -ge 1 ] 2>/dev/null && ok "4.4 diff 有变更" "$diffs" ">=1" || bad "4.4 diff 有变更" "$diffs" ">=1"
  dtypes="$(peek "[x['diffType'] for x in (d.get('data',{}).get('diffs') or [])]" <<< "$r")"
  check_contains "4.4 diff 类型" "$dtypes" "MODIFIED"
}

# =============================================================================
# 场景 5: 批量导入导出
# =============================================================================
scenario5_import_export() {
  log INFO "场景 5: 批量导入导出"
  local r items imported_fqn

  items="$(gen items "$MF_IMPORT_BATCH_SIZE" "$PREFIX")"
  r="$(api POST /api/v1/metadata/import "$(import_body "$items")")"
  check "5.1 导入 total" "$(peek "d.get('data',{}).get('totalCount')" <<< "$r")" "$MF_IMPORT_BATCH_SIZE"
  check "5.1 导入 success" "$(peek "d.get('data',{}).get('successCount')" <<< "$r")" "$MF_IMPORT_BATCH_SIZE"
  check "5.1 导入 error=0" "$(peek "d.get('data',{}).get('errorCount')" <<< "$r")" "0"

  imported_fqn="${PREFIX}_IMP_001"
  r="$(api GET "/api/v1/metadata/entities/${imported_fqn}")"
  check "5.2 导入仅写草稿表 code" "$(code_of "$r")" "31004"

  r="$(api POST "/api/v1/metadata/entities/${imported_fqn}/activate")"
  check "5.3 激活导入项" "$(code_of "$r")" "200"
  check "5.3 currentVersion=1" "$(data_fld "$r" currentVersion)" "1"

  r="$(api POST /api/v1/metadata/export "{\"type\":\"FQN_PREFIXES\",\"fqnPrefixes\":[\"${PREFIX}_IMP\"],\"format\":\"JSON\"}")"
  check "5.4 导出 entityCount=1" "$(peek "d.get('data',{}).get('entityCount')" <<< "$r")" "1"
  check_contains "5.4 导出内容含激活项" "$(peek "d.get('data',{}).get('content')" <<< "$r")" "$imported_fqn"
}

# =============================================================================
# 场景 6: 管理端全状态查询
# =============================================================================
scenario6_admin() {
  log INFO "场景 6: 管理端全状态查询"
  local r statuses
  r="$(api GET "/api/v1/metadata/admin/metadata?statuses=DRAFT,ACTIVE,HISTORY&page=1&size=100")"
  check "6.1 管理查询 code" "$(code_of "$r")" "200"
  statuses="$(peek "sorted({x.get('status') for x in (d.get('data',{}).get('content') or [])})" <<< "$r")"
  check_contains "6.1 含 DRAFT" "$statuses" "DRAFT"
  check_contains "6.1 含 ACTIVE" "$statuses" "ACTIVE"
  check_contains "6.1 含 HISTORY" "$statuses" "HISTORY"
}

# =============================================================================
# 场景 7: 批量合规校验
# =============================================================================
scenario7_validate() {
  log INFO "场景 7: 批量合规校验"
  local r total succ
  r="$(api POST "/api/v1/metadata/validate-batch?entitySchemaFqn=${MF_ENTITY_SCHEMA_FQN}")"
  total="$(peek "d.get('data',{}).get('totalCount')" <<< "$r")"
  succ="$(peek "d.get('data',{}).get('successCount')" <<< "$r")"
  [ "${total:-0}" -ge 1 ] 2>/dev/null && ok "7.1 批量校验 total≥1" "$total" ">=1" || bad "7.1 批量校验 total≥1" "$total" ">=1"
  [ "${total:-0}" = "${succ:-0}" ] 2>/dev/null && ok "7.1 全部通过" "$succ" "$total" || bad "7.1 全部通过" "$succ" "$total"
}

# =============================================================================
# 一致性验证
# =============================================================================
consistency_checks() {
  log INFO "一致性验证: FQN 唯一性 / 主表-历史一致性"
  local r fqn i main_content hist_content
  local -a codes=()

  fqn="${PREFIX}_UNIQ"
  for i in 1 2 3; do
    r="$(api POST /api/v1/metadata/drafts "$(draft_body "$fqn" "$(gen content)")")"
    codes+=("$(code_of "$r")")
  done
  check "一致性 FQN 唯一 [200,31001,31001]" "${codes[*]}" "200 31001 31001"

  main_content="$(api GET "/api/v1/metadata/entities/${MASTER_FQN}" | peek "json.dumps(d.get('data',{}).get('content'),sort_keys=True)")"
  hist_content="$(api GET "/api/v1/metadata/history/${MASTER_FQN}/versions/2" | peek "json.dumps(d.get('data',{}).get('content'),sort_keys=True)")"
  check "一致性 主表=历史 v2" "$main_content" "$hist_content"
}

# =============================================================================
# 韧性: jar 重启后数据持久性
# =============================================================================
resilience_jar_restart() {
  [ "$MF_SKIP_JAR_RESTART" = "1" ] && { log INFO "跳过 jar 重启测试 (MF_SKIP_JAR_RESTART=1)"; return; }
  if [ -z "$MF_JAR_PATH" ] || [ ! -f "$MF_JAR_PATH" ]; then
    log WARN "未找到 boot jar，跳过 jar 重启测试"
    return
  fi
  log INFO "韧性: jar 重启 → 数据持久性"

  local before_ver before_content after_ver after_content
  before_ver="$(api GET "/api/v1/metadata/entities/${MASTER_FQN}" | peek "d.get('data',{}).get('currentVersion')")"
  before_content="$(api GET "/api/v1/metadata/entities/${MASTER_FQN}" | peek "json.dumps(d.get('data',{}).get('content'),sort_keys=True)")"

  stop_app
  start_app || { bad "jar 重启后应用可启动" "no" "yes"; return; }
  ok "jar 重启后应用健康" "UP"

  after_ver="$(api GET "/api/v1/metadata/entities/${MASTER_FQN}" | peek "d.get('data',{}).get('currentVersion')")"
  after_content="$(api GET "/api/v1/metadata/entities/${MASTER_FQN}" | peek "json.dumps(d.get('data',{}).get('content'),sort_keys=True)")"
  check "jar 重启后 currentVersion 保持" "$after_ver" "$before_ver"
  check "jar 重启后 content 保持一致" "$after_content" "$before_content"
}

# =============================================================================
# 韧性: PostgreSQL 重启 → 应用重连与恢复
# =============================================================================
resilience_pg_restart() {
  [ "$MF_SKIP_PG_RESTART" = "1" ] && { log INFO "跳过 PostgreSQL 重启测试 (MF_SKIP_PG_RESTART=1)"; return; }
  log INFO "韧性: PostgreSQL 重启 → 应用重连（tmpfs 数据卷，重启即清空）"

  log INFO "重启容器: $MF_CONTAINER_RUNTIME restart $MF_PG_CONTAINER"
  "$MF_CONTAINER_RUNTIME" restart "$MF_PG_CONTAINER" >/dev/null || { bad "PG 容器重启命令" "fail" "ok"; return; }
  wait_pg_healthy || { bad "PG 容器恢复 healthy" "no" "yes"; return; }
  ok "PG 容器恢复 healthy" "healthy"

  # 应用自动重连（HikariCP），健康状态恢复
  wait_app_up || { bad "PG 重启后应用恢复 UP" "no" "yes"; return; }
  ok "PG 重启后应用自动恢复 UP" "UP"

  # tmpfs 清空后重启 jar，Flyway 重建 schema，验证全新环境可正常服务
  if [ -n "$MF_JAR_PATH" ] && [ -f "$MF_JAR_PATH" ]; then
    log INFO "PG 数据已清空，重启 jar 重建 Flyway schema"
    stop_app
    start_app || { bad "PG 清空后应用可启动" "no" "yes"; return; }
    ok "PG 清空后应用重建成功" "UP"

    MF_ENTITY_SCHEMA_FQN=""
    setup_metamodel || { bad "恢复后重建元模型" "fail" "ok"; return; }
    fetch_schema || { bad "恢复后重建 schema" "fail" "ok"; return; }
    local r smoke
    r="$(api POST /api/v1/metadata/drafts "$(draft_body "${PREFIX}_SMOKE" "$(gen content)")")"
    check "PG 恢复后冒烟: 创建草稿" "$(code_of "$r")" "200"
    smoke="$(data_fld "$r" fqn)"
    check "PG 恢复后冒烟: 草稿 FQN" "$smoke" "${PREFIX}_SMOKE"
  fi
}

# =============================================================================
# 性能基准（可选，MF_RUN_PERF=1）
# =============================================================================
performance() {
  [ "$MF_RUN_PERF" = "1" ] || { log INFO "跳过性能基准 (MF_RUN_PERF=1 开启)"; return; }
  log INFO "性能基准"
  local i t0 t1 sum=0 avg p95 dur
  local -a times=()

  for i in $(seq 1 "$MF_PERF_DRAFT_COUNT"); do
    local fqn="${PREFIX}_PERF_$(printf %03d "$i")"
    t0=$(date +%s%N)
    api POST /api/v1/metadata/drafts "$(draft_body "$fqn" "$(gen content)")" > /dev/null
    t1=$(date +%s%N)
    times+=($(((t1 - t0) / 1000000)))
  done
  sum=0; for t in "${times[@]}"; do sum=$((sum + t)); done
  avg=$((sum / ${#times[@]}))
  printf '  %-40s avg=%-5dms\n' "草稿创建 x${MF_PERF_DRAFT_COUNT}" "$avg"
  [ "$avg" -le "$MF_PERF_DRAFT_MS" ] && ok "草稿创建 avg ≤${MF_PERF_DRAFT_MS}ms" "$avg" "<=${MF_PERF_DRAFT_MS}" || bad "草稿创建 avg ≤${MF_PERF_DRAFT_MS}ms" "$avg" "<=${MF_PERF_DRAFT_MS}"

  times=()
  for i in $(seq 1 "$MF_PERF_QUERY_COUNT"); do
    t0=$(date +%s%N)
    api GET "/api/v1/metadata/entities/${MASTER_FQN}" > /dev/null
    t1=$(date +%s%N)
    times+=($(((t1 - t0) / 1000000)))
  done
  p95=$(python3 -c "import sys; a=sorted(int(x) for x in sys.argv[1:]); print(a[int(len(a)*0.95)-1])" "${times[@]}")
  printf '  %-40s p95=%-5dms\n' "FQN 精准查询 x${MF_PERF_QUERY_COUNT}" "$p95"
  [ "$p95" -le "$MF_PERF_QUERY_MS" ] && ok "FQN 精准查询 p95 ≤${MF_PERF_QUERY_MS}ms" "$p95" "<=${MF_PERF_QUERY_MS}" || bad "FQN 精准查询 p95 ≤${MF_PERF_QUERY_MS}ms" "$p95" "<=${MF_PERF_QUERY_MS}"

  local items
  items="$(gen items "$MF_PERF_BATCH_COUNT" "${PREFIX}PB")"
  t0=$(date +%s%N)
  api POST /api/v1/metadata/import "$(import_body "$items")" > /dev/null
  t1=$(date +%s%N)
  dur=$(((t1 - t0) / 1000000))
  printf '  %-40s %dms\n' "批量导入 x${MF_PERF_BATCH_COUNT}" "$dur"
  [ "$dur" -le "$MF_PERF_IMPORT_MS" ] && ok "批量导入 ≤${MF_PERF_IMPORT_MS}ms" "${dur}ms" "<=${MF_PERF_IMPORT_MS}ms" || bad "批量导入 ≤${MF_PERF_IMPORT_MS}ms" "${dur}ms" "<=${MF_PERF_IMPORT_MS}ms"
}

# =============================================================================
# 主流程
# =============================================================================
main() {
  log INFO "MetaForge metadata 全生命周期 e2e"
  log INFO "BASE_URL=$MF_BASE_URL  PG=$MF_CONTAINER_RUNTIME/$MF_PG_CONTAINER  RUN_ID=$MF_RUN_ID"
  log INFO "ENTITY_SCHEMA_FQN=${MF_ENTITY_SCHEMA_FQN:-<动态创建>}"
  log INFO "JAR=${MF_JAR_PATH:-<未配置>}"

  # 1) 应用可用性
  if ! wait_app_up; then
    if [ -n "$MF_JAR_PATH" ] && [ -f "$MF_JAR_PATH" ]; then
      log INFO "应用未启动，尝试自行启动"
      start_app || { log ERR "应用启动失败，日志见 $MF_APP_LOG"; exit 1; }
    else
      log ERR "应用不可达且未配置 jar 路径，无法继续"
      exit 1
    fi
  fi
  ok "应用健康" "UP"

  # 2) 元模型前置 + 生成器
  setup_metamodel || { log ERR "元模型前置失败"; exit 1; }
  setup_generator
  fetch_schema || { log ERR "schema 获取失败"; exit 1; }

  # 3) 构造测试数据（多级父子层级 + 独立实体）
  log INFO "测试数据层级: $MASTER_FQN → ($LINE1_FQN, $LINE2_FQN), $LINE1_FQN → $SUB1_FQN; 独立: $RPT_FQN"
  local r
  for fqn in "$LINE1_FQN" "$LINE2_FQN" "$SUB1_FQN" "$RPT_FQN"; do
    r="$(api POST /api/v1/metadata/drafts "$(draft_body "$fqn" "$(gen content)")")"
    if [ "$(code_of "$r")" = "200" ]; then
      api POST "/api/v1/metadata/entities/${fqn}/activate" > /dev/null
    else
      log ERR "构造数据失败 $fqn: $r"
    fi
  done

  # 4) 功能场景
  scenario1_core
  scenario2_query
  scenario3_deactivate
  scenario4_history
  scenario5_import_export
  scenario6_admin
  scenario7_validate
  consistency_checks

  # 5) 韧性
  resilience_jar_restart
  resilience_pg_restart

  # 6) 性能
  performance

  # 7) 汇总
  echo
  log INFO "=============================="
  log INFO "通过 $PASS  失败 $FAIL"
  if [ "$FAIL" -gt 0 ]; then
    for f in "${FAILURES[@]}"; do log ERR "  ✗ $f"; done
  fi
  log INFO "=============================="
  [ "$FAIL" -eq 0 ]
}

main
