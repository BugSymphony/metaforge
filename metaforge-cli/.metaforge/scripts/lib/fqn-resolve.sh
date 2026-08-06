#!/usr/bin/env bash
# lib/fqn-resolve.sh — FQN 推测流水线（FR-NL-001~005、FR-011/012、D3）。
#
# 铁律：推测必须基于服务端认知接口返回的目录/实例数据，严禁凭空臆测（FR-NL-002）。
# 匹配优先级：精确 FQN > 名称精确匹配 > keywords/aliases 匹配 > 子串包含（FR-NL-003）。
# 不引入模糊检索/相似度（保证可解释、可预测、可审计）。
#
# 结果判定（FR-NL-004）：
#   唯一命中 → 自动确认，输出 FQN
#   多候选   → 输出候选列表，请用户选择（不擅自猜测）
#   零命中   → 终止，输出原因与平台现有清单
#
# 目标类型识别（FR-NL-005）：Bundle / 主题域(含任务) / 实体。
# 数据源：
#   Bundle     → bundle-catalog（bundle_directory + domain_navigation）
#   主题域/任务 → navigate（domain_navigation）
#   实体       → cognition-guidance 组合 schema_inventory + instance_catalog
#
# 用法：
#   mf_fqn_resolve_bundle <描述>
#   mf_fqn_resolve_domain <描述>
#   mf_fqn_resolve_entity <描述> [bundle_fqns...]
#   mf_fqn_match <描述> <candidates_json> [type]

set -uo pipefail

# 从 candidates JSON（数组）中按描述做确定型匹配，输出：
#   MF_FQN_UNIQUE=<fqn>         唯一命中
#   MF_FQN_MULTI=<JSON 数组>     多候选（结构化）
#   MF_FQN_NONE=1                零命中
# 匹配顺序：精确 FQN > name 精确 > keywords/aliases > 子串。
mf_fqn_match() {
    local desc="$1" candidates_json="${2:-[]}"
    local lc_desc="$(printf '%s' "$desc" | tr 'A-Z' 'a-z')"
    local result
    result="$(printf '%s' "$candidates_json" | python3 -c "
import json, sys
desc = '''$lc_desc'''
try:
    cands = json.load(sys.stdin)
except Exception:
    sys.exit(1)
if not isinstance(cands, list) or not cands:
    print('MF_FQN_NONE=1')
    sys.exit(0)

def norm(s):
    return str(s).lower() if s is not None else ''

def score(c):
    fqn = norm(c.get('fqn'))
    name = norm(c.get('name'))
    desc_s = norm(c.get('description'))
    keywords = [norm(k) for k in (c.get('keywords') or [])]
    aliases = [norm(a) for a in (c.get('aliases') or [])]
    if fqn == desc or name == desc:
        return 3
    if desc in keywords or desc in aliases:
        return 2
    if desc in fqn or desc in name or desc in desc_s:
        return 1
    return 0

scored = [(score(c), c) for c in cands]
scored = [(s, c) for s, c in scored if s > 0]
scored.sort(key=lambda x: -x[0])
if not scored:
    print('MF_FQN_NONE=1')
    sys.exit(0)
top = scored[0][0]
top_cands = [c for s, c in scored if s == top]
if len(top_cands) == 1:
    print('MF_FQN_UNIQUE=' + str(top_cands[0].get('fqn')))
else:
    print('MF_FQN_MULTI=' + json.dumps(top_cands, ensure_ascii=False))
")"
    if [[ -z "$result" ]]; then
        MF_FQN_NONE=1
        return 1
    fi
    if [[ "$result" == MF_FQN_UNIQUE=* ]]; then
        MF_FQN_UNIQUE="${result#MF_FQN_UNIQUE=}"
        MF_FQN_MULTI=""
        MF_FQN_NONE=""
        return 0
    elif [[ "$result" == MF_FQN_MULTI=* ]]; then
        MF_FQN_UNIQUE=""
        MF_FQN_MULTI="${result#MF_FQN_MULTI=}"
        MF_FQN_NONE=""
        return 0
    else
        MF_FQN_UNIQUE=""
        MF_FQN_MULTI=""
        MF_FQN_NONE=1
        return 1
    fi
}

# 从 bundle-catalog 响应中提取 Bundle 候选（fqn/name/description）。
# 兼容视角层级：perspectives.<id>.data.bundles（契约）与扁平 bundles/items 变体。
mf_fqn_bundle_candidates() {
    local resp="${1:-}"
    if command -v python3 >/dev/null 2>&1; then
        printf '%s' "$resp" | python3 -c '
import json, sys
d = json.load(sys.stdin)
data = d.get("data") or {}
pers = data.get("perspectives", {}) if isinstance(data.get("perspectives"), dict) else {}
out = []

def collect(p):
    if not isinstance(p, dict):
        return
    if "data" in p and isinstance(p["data"], dict):
        p = p["data"]
    items = p.get("bundles") or p.get("items") or p.get("list") or []
    if isinstance(p, list):
        items = p
    for b in items:
        if isinstance(b, dict):
            out.append({"fqn": b.get("fqn") or b.get("bundleFqn") or b.get("id") or "",
                        "name": b.get("name") or "",
                        "description": b.get("description") or ""})

for key in ("bundle_directory", "bundleDirectory", "bundles"):
    if key in pers:
        collect(pers[key])
        if out:
            break
if not out and "bundles" in data:
    collect(data)
print(json.dumps(out, ensure_ascii=False))
' 2>/dev/null || printf '[]'
    fi
}

# 从 navigate 响应中提取主题域/任务候选（fqn/name/description/keywords）。
mf_fqn_domain_candidates() {
    local resp="${1:-}"
    if command -v python3 >/dev/null 2>&1; then
        printf '%s' "$resp" | python3 -c '
import json, sys
d = json.load(sys.stdin)
data = d.get("data") or {}
pers = data.get("perspectives", {}) if isinstance(data.get("perspectives"), dict) else {}
out = []

def walk(node):
    if not isinstance(node, dict):
        return
    if "data" in node and isinstance(node["data"], dict):
        node = node["data"]
    fqn = node.get("fqn") or node.get("subjectDomainFqn") or node.get("taskFqn") or ""
    name = node.get("name") or ""
    desc = node.get("description") or ""
    if fqn or name:
        out.append({"fqn": fqn, "name": name, "description": desc,
                    "keywords": node.get("keywords") or []})
    for k, v in node.items():
        if isinstance(v, dict):
            walk(v)
        elif isinstance(v, list):
            for x in v:
                walk(x)

for key in ("domain_navigation", "domainNavigation"):
    if key in pers:
        walk(pers[key])
        if out:
            break
if not out and "domain_navigation" in data:
    walk(data)
print(json.dumps(out, ensure_ascii=False))
' 2>/dev/null || printf '[]'
    fi
}

# 从 cognition-guidance（schema_inventory + instance_catalog）响应中提取实体候选。
mf_fqn_entity_candidates() {
    local resp="${1:-}"
    if command -v python3 >/dev/null 2>&1; then
        printf '%s' "$resp" | python3 -c '
import json, sys
d = json.load(sys.stdin)
data = d.get("data") or {}
pers = data.get("perspectives", {}) if isinstance(data.get("perspectives"), dict) else {}
out = []
ic = pers.get("instance_catalog") or {}
if isinstance(ic, dict) and "data" in ic and isinstance(ic["data"], dict):
    ic = ic["data"]
items = ic.get("entities") or ic.get("items") or []
for e in items:
    if isinstance(e, dict):
        out.append({"fqn": e.get("fqn") or e.get("id") or "",
                    "name": e.get("name") or "",
                    "description": e.get("description") or "",
                    "entitySchemaFqn": e.get("entitySchemaFqn") or ""})
print(json.dumps(out, ensure_ascii=False))
' 2>/dev/null || printf '[]'
    fi
}

# 平台当前已发布 Bundle 清单（零命中时用于给用户提供可选项，FR-NL-004）。
mf_fqn_published_bundles() {
    local resp
    resp="$(mf_http_post_cognition "bundle-catalog" "{\"format\":\"json\"}")" || {
        printf '（无法获取平台 Bundle 清单：服务端暂不可用）'
        return 0
    }
    mf_fqn_bundle_candidates "$resp" | python3 -c '
import json, sys
try:
    cands = json.load(sys.stdin)
    print("，".join(c.get("fqn") or c.get("name") or "" for c in cands if c))
except Exception:
    print("")
'
}

# Bundle FQN 推测：调 bundle-catalog 获取候选 → 匹配。
mf_fqn_resolve_bundle() {
    local desc="$1"
    local resp
    resp="$(mf_http_post_cognition "bundle-catalog" "{\"format\":\"json\"}")" || return 1
    local cands="$(mf_fqn_bundle_candidates "$resp")"
    mf_fqn_match "$desc" "$cands"
    return $?
}

# 主题域/任务 FQN 推测：调 navigate 获取候选 → 匹配。
mf_fqn_resolve_domain() {
    local desc="$1"
    local resp
    resp="$(mf_http_post_cognition "navigate" "{\"format\":\"json\"}")" || return 1
    local cands="$(mf_fqn_domain_candidates "$resp")"
    mf_fqn_match "$desc" "$cands"
    return $?
}

# 实体 FQN 推测：调 cognition-guidance（schema_inventory + instance_catalog）获取候选 → 匹配。
mf_fqn_resolve_entity() {
    local desc="$1"
    local resp
    resp="$(mf_http_post_cognition "cognition-guidance" \
        "{\"bundleFqns\":[],\"perspectives\":[\"schema_inventory\",\"instance_catalog\"],\"format\":\"json\"}")" || return 1
    local cands="$(mf_fqn_entity_candidates "$resp")"
    mf_fqn_match "$desc" "$cands"
    return $?
}

# 顶层入口：自动识别目标类型并推测（FR-NL-005）。
# 输出：唯一命中 → FQN；多候选 → 候选 JSON；零命中 → 空并置 MF_FQN_NONE=1。
# 目标类型由调用方根据上下文指定（bundle/domain/entity），未指定时依次尝试。
mf_fqn_resolve() {
    local desc="$1" type="${2:-auto}"
    MF_FQN_UNIQUE=""; MF_FQN_MULTI=""; MF_FQN_NONE=""
    case "$type" in
        bundle)  mf_fqn_resolve_bundle "$desc"; return $? ;;
        domain)  mf_fqn_resolve_domain "$desc"; return $? ;;
        entity)  mf_fqn_resolve_entity "$desc"; return $? ;;
    esac
    # auto：先 bundle，再 domain，再 entity（FR-NL-005 顺序）
    mf_fqn_resolve_bundle "$desc" && { [[ -n "$MF_FQN_UNIQUE" || -n "$MF_FQN_MULTI" ]] && return 0; }
    mf_fqn_resolve_domain "$desc" && { [[ -n "$MF_FQN_UNIQUE" || -n "$MF_FQN_MULTI" ]] && return 0; }
    mf_fqn_resolve_entity "$desc"
    return $?
}

# 输出候选列表（人类可读，供用户选择）。
mf_fqn_print_multi() {
    printf '%s' "$MF_FQN_MULTI" | python3 -c '
import json, sys
try:
    cands = json.load(sys.stdin)
except Exception:
    sys.exit(0)
for i, c in enumerate(cands, 1):
    print("  %d) %s  %s  %s" % (i, c.get("fqn") or "?", c.get("name") or "", c.get("description") or ""))
'
}
