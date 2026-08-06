#!/usr/bin/env bash
# modules/cognition.sh — 认知查询模块（FR-DEV-002/003、FR-DLV-003、FR-CAP-002、D1/D2/R3）。
#
#   cognition templates                列出服务端模板注册表投影（内置 6 模板，可选 --verify 探测）
#   cognition execute <templateId>     认知查询：flag → CognitionRequest 结构化字段（camelCase）
#
# 设计：
#   - 模板清单为"注册表投影"，单一来源维护；不把"模板→命令"映射硬编码到各命令文件（FR-CAP-002/R3）
#   - flag→字段映射表（contracts/script-cli.md）：全部 CognitionRequest 字段（D1 camelCase）
#   - 请求体构造一律经 python3 JSON 序列化，杜绝手工拼 JSON 注入风险
#   - 模板特有必填校验（FR-014）在对应用户故事阶段补充（task-brief/--bundles 等）

set -uo pipefail

# 内置模板清单（PRD §4.0 / FR-DEV-002；服务端注册表投影）。
MF_TEMPLATES=(bundle-catalog cognition-guidance task-brief step-guide navigate sub-task-brief)

# 输出服务端模板注册表投影。
# --verify: 探测服务端实际注册状态（注册模板调用返回非 34001；未注册返回 34001）
mf_cognition_templates() {
    local verify=0
    for a in "$@"; do [[ "$a" == "--verify" ]] && verify=1; done

    if [[ "$verify" -eq 0 ]]; then
        for t in "${MF_TEMPLATES[@]}"; do
            printf '%s\n' "$t"
        done
        return 0
    fi

    # --verify：对每个模板做轻量探测（空 bundle 列表），注册模板返回非 34001
    for t in "${MF_TEMPLATES[@]}"; do
        local resp code
        resp="$(mf_http_post_cognition "$t" '{"bundleFqns":[]}' --raw 2>/dev/null)"
        code="$(mf_http_extract_code "$resp")"
        if [[ -z "$resp" ]]; then
            printf '%s\t（无法验证：服务端不可达）\n' "$t"
        elif [[ "$code" == "34001" ]]; then
            printf '%s\t（未注册）\n' "$t"
        else
            printf '%s\t（已注册）\n' "$t"
        fi
    done
    return 0
}

# flag → CognitionRequest 字段映射（D1 camelCase）。构造请求体 JSON。
# 全部 flag 与 contracts/script-cli.md 对齐；--json 原样覆盖所有 flag。
mf_cognition_build_body() {
    local template_id="${1:-}"; shift
    local bundles="" entity_fqn="" entry_entity="" entity_types="" subject_domain="" scope_mode=""
    local depth="" archetype="" max_tokens="" expand="" format="" page_size="" cursor=""
    local raw_json="" perspectives=""
    local verbose=0

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --bundles)        bundles="${2:-}"; shift 2 ;;
            --entity-fqn)     entity_fqn="${2:-}"; shift 2 ;;
            --entry-entity)   entry_entity="${2:-}"; shift 2 ;;
            --entity-types)   entity_types="${2:-}"; shift 2 ;;
            --subject-domain) subject_domain="${2:-}"; shift 2 ;;
            --scope-mode)     scope_mode="${2:-}"; shift 2 ;;
            --depth)          depth="${2:-}"; shift 2 ;;
            --archetype)      archetype="${2:-}"; shift 2 ;;
            --max-tokens)     max_tokens="${2:-}"; shift 2 ;;
            --expand)         expand="${2:-}"; shift 2 ;;
            --format)         format="${2:-}"; shift 2 ;;
            --page-size)      page_size="${2:-}"; shift 2 ;;
            --cursor)         cursor="${2:-}"; shift 2 ;;
            --perspectives)   perspectives="${2:-}"; shift 2 ;;
            --json)           raw_json="${2:-}"; shift 2 ;;
            --verbose)        verbose=1; shift ;;
            *) echo "错误：未知 flag '$1'" >&2; return 2 ;;
        esac
    done

    # --json 原样覆盖（FR-DEV-003）
    if [[ -n "$raw_json" ]]; then
        printf '%s' "$raw_json"
        return 0
    fi

    # 默认值从配置读取（FR-CFG-002）
    [[ -z "$depth" ]] && depth="$(mf_config_get default.depth)"
    [[ -z "$archetype" ]] && archetype="$(mf_config_get default.archetype)"
    [[ -z "$max_tokens" ]] && max_tokens="$(mf_config_get default.max_tokens)"
    [[ -z "$expand" ]] && expand="$(mf_config_get default.expand)"
    [[ -z "$format" ]] && format="$(mf_config_get default.format)"

    # 用 python3 构造 JSON（bundleFqns/entityTypes/perspectives 数组化，其余字符串/数字）
    FORMAT="$format" MAX_TOKENS="$max_tokens" PAGE_SIZE="$page_size" CURSOR="$cursor" \
    BUNDLES="$bundles" ENTITY_TYPES="$entity_types" PERSPECTIVES="$perspectives" \
    ENTITY_FQN="$entity_fqn" ENTRY_ENTITY="$entry_entity" \
    SUBJECT_DOMAIN="$subject_domain" SCOPE_MODE="$scope_mode" \
    DEPTH="$depth" ARCHETYPE="$archetype" EXPAND="$expand" \
    python3 -c '
import json, os
def split_csv(s):
    if not s:
        return []
    return [x.strip() for x in s.split(",") if x.strip()]
def num_or(s, default=None):
    if s is None or s == "":
        return default
    try:
        return int(s)
    except ValueError:
        return default
body = {}
b = split_csv(os.environ["BUNDLES"])
if b:
    body["bundleFqns"] = b
et = split_csv(os.environ["ENTITY_TYPES"])
if et:
    body["entityTypes"] = et
pe = split_csv(os.environ["PERSPECTIVES"])
if pe:
    body["perspectives"] = pe
for key, env, is_num in [
    ("entityFqn", "ENTITY_FQN", False),
    ("entryEntityFqn", "ENTRY_ENTITY", False),
    ("subjectDomainFqn", "SUBJECT_DOMAIN", False),
    ("scopeMode", "SCOPE_MODE", False),
    ("cognitionDepth", "DEPTH", False),
    ("agentArchetype", "ARCHETYPE", False),
    ("expand", "EXPAND", False),
    ("format", "FORMAT", False),
]:
    v = os.environ.get(env, "")
    if v:
        body[key] = v
mt = num_or(os.environ.get("MAX_TOKENS"))
if mt is not None:
    body["maxTokens"] = mt
ps = num_or(os.environ.get("PAGE_SIZE"))
if ps is not None:
    body["pageSize"] = ps
cu = num_or(os.environ.get("CURSOR"))
if cu is not None:
    body["cursor"] = cu
print(json.dumps(body, ensure_ascii=False))
'
}

# 模板必填 flag 校验（FR-014 / FR-DLV-004）。
# task-brief → --bundles；缺失时输出用法提示并中止（退出码 2）。
mf_cognition_validate_required() {
    local template_id="$1"; shift
    local bundles=""
    local entry_entity=""
    local entity_fqn=""
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --bundles)     bundles="${2:-}"; shift 2 ;;
            --entity-fqn)  entity_fqn="${2:-}"; shift 2 ;;
            --entry-entity) entry_entity="${2:-}"; shift 2 ;;
            *) shift ;;
        esac
    done

    case "$template_id" in
        task-brief)
            if [[ -z "$bundles" ]]; then
                echo "错误：metaforge.task-brief 必须指定 --bundles（如 --bundles order:1.0.0）" >&2
                return 2
            fi
            ;;
        step-guide)
            if [[ -z "$entity_fqn" ]]; then
                echo "错误：metaforge.step-guide 必须指定 --entity-fqn（如 --entity-fqn order:1.0.0.Step_CheckInventory）" >&2
                return 2
            fi
            ;;
        navigate)
            if [[ -z "$bundles" ]]; then
                echo "错误：metaforge.navigate 必须指定 --bundles（如 --bundles order:1.0.0）" >&2
                return 2
            fi
            ;;
        sub-task-brief)
            if [[ -z "$bundles" ]]; then
                echo "错误：metaforge.subtask 必须指定 --bundles（如 --bundles order:1.0.0）" >&2
                return 2
            fi
            if [[ -z "$entry_entity" ]]; then
                echo "错误：metaforge.subtask 必须指定 --entry-entity（如 --entry-entity order:1.0.0.Task_OrderProcessing）" >&2
                return 2
            fi
            ;;
    esac
    return 0
}

# cognition execute <templateId>：认知查询（flag → CognitionRequest → POST → 输出）。
# 输出：format=json 原样输出 ApiResponse；format=prompt 输出 data.content（Markdown，零解析注入）。
mf_cognition_execute() {
    local template_id="${1:-}"
    if [[ -z "$template_id" ]]; then
        echo "错误：缺少 templateId。用法：metaforge-pro.sh cognition execute <templateId> [flags]" >&2
        return 2
    fi
    shift

    local verbose=0
    local format="$(mf_config_get default.format)"
    local args=()
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --verbose) verbose=1; shift ;;
            --format)  format="${2:-}"; args+=("$1" "$2"); shift 2 ;;
            *)         args+=("$1"); shift ;;
        esac
    done

    # 模板必填校验（FR-014）：缺失 → 用法提示并中止（退出码 2）
    if ! mf_cognition_validate_required "$template_id" "${args[@]}"; then
        return 2
    fi

    local body
    if ! body="$(mf_cognition_build_body "$template_id" "${args[@]}")"; then
        return 2
    fi

    local resp rc
    local vb=()
    [[ "$verbose" -eq 1 ]] && vb=(--verbose)
    resp="$(mf_http_post_cognition "$template_id" "$body" "${vb[@]}")"
    rc=$?
    if [[ "$rc" -ne 0 ]]; then
        # 业务错误已在 http.sh 内输出中文提示
        return "$rc"
    fi

    # 输出（FR-OUT-001/002）：json 原样透传；prompt 输出 Markdown
    if [[ "$format" == "prompt" ]]; then
        printf '%s' "$resp" | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
    data = d.get("data") or {}
    print(data.get("content") or data.get("prompt") or "", end="")
except Exception:
    print("", end="")
'
        printf '\n'
    else
        printf '%s\n' "$resp"
    fi

    # FR-019：展示截断/跳过的视角提示（避免 Agent 误认为数据缺失）。
    # 提示输出到 stderr，保持 stdout 为原样透传的 json/prompt（FR-OUT-002 零信息损耗）。
    printf '%s' "$resp" | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
    cm = (d.get("data") or {}).get("contextMeta") or (d.get("data") or {}).get("context_meta") or {}
    skipped = cm.get("skippedPerspectives") or cm.get("skipped_perspectives") or []
    truncated = cm.get("truncatedPerspectives") or cm.get("truncated_perspectives") or []
    for s in (skipped or []):
        reason = s.get("reason") if isinstance(s, dict) else ""
        print("提示：视角 %s 已跳过（%s）" % (s.get("perspective_id") if isinstance(s, dict) else s, reason), file=sys.stderr)
    for t in (truncated or []):
        reason = t.get("reason") if isinstance(t, dict) else ""
        print("提示：视角 %s 查询超时已截断（%s）" % (t.get("perspective_id") if isinstance(t, dict) else t, reason), file=sys.stderr)
except Exception:
    pass
'

    # FR-006/T031：impact_trace 分层摘要（forward/backward/impact_paths），输出到 stderr 便于阅读
    mf_cognition_impact_summary "$resp"
    return 0
}

# impact_trace 分层呈现（FR-CAP-006/FR-006、T031）：从响应中提取
# forward_impact / backward_dependency / impact_paths 并按影响程度分层，输出到 stderr。
# 默认 stdout 仍为原样透传（FR-OUT-002），本函数仅作可读性增强。
mf_cognition_impact_summary() {
    local resp="${1:-}"
    printf '%s' "$resp" | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
    pers = (d.get("data") or {}).get("perspectives") or {}
    it = pers.get("impact_trace") or {}
    if isinstance(it, dict) and "data" in it and isinstance(it["data"], dict):
        it = it["data"]
    fwd = it.get("forward_impact")
    if fwd is None:
        fwd = it.get("forwardImpact")
    bwd = it.get("backward_dependency")
    if bwd is None:
        bwd = it.get("backwardDependency")
    paths = it.get("impact_paths")
    if paths is None:
        paths = it.get("impactPaths")
    if fwd is None and bwd is None and paths is None:
        sys.exit(0)
    print("影响感知（impact_trace）：", file=sys.stderr)
    if fwd is not None:
        n = len(fwd) if isinstance(fwd, list) else 1
        print("  - 正向影响（forward）: %d 个直接/间接波及项" % n, file=sys.stderr)
    if bwd is not None:
        n = len(bwd) if isinstance(bwd, list) else 1
        print("  - 反向依赖（backward）: %d 个依赖项" % n, file=sys.stderr)
    if paths is not None:
        n = len(paths) if isinstance(paths, list) else 1
        print("  - 影响路径（paths）: %d 条" % n, file=sys.stderr)
except Exception:
    pass
'
    return 0
}

# cognition resolve <描述>：基于服务端数据推测 FQN（FR-NL-001~005）。
# 行为（FR-NL-004）：
#   唯一命中 → 输出 FQN，退出码 0
#   多候选   → 列出候选（fqn/name/description）请用户选择，退出码 3
#   零命中   → 终止并给原因 + 平台已发布清单，退出码 1
# 参数：--type bundle|domain|entity|auto（默认 auto）
mf_cognition_resolve() {
    local type="auto"
    local desc_parts=()
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --type) type="${2:-auto}"; shift 2 ;;
            -*) echo "错误：未知 flag '$1'" >&2; return 2 ;;
            *) desc_parts+=("$1"); shift ;;
        esac
    done
    if [[ ${#desc_parts[@]} -eq 0 ]]; then
        echo "错误：缺少自然语言描述。用法：metaforge-pro.sh cognition resolve <描述> [--type bundle|domain|entity|auto]" >&2
        return 2
    fi
    local desc="${desc_parts[*]}"

    # 推测（仅基于服务端数据，严禁臆测）。返回码非零分两种：
    #   服务端不可达/业务错误 → 已在 http.sh 内提示，此处直接退出；
    #   零命中（MF_FQN_NONE=1）→ 属正常终止路径，走下方 FR-NL-004 分支。
    mf_fqn_resolve "$desc" "$type" || {
        if [[ "${MF_FQN_NONE:-}" == "1" ]]; then
            :   # 零命中，进入下方处理
        else
            return 1
        fi
    }

    if [[ -n "$MF_FQN_UNIQUE" ]]; then
        printf '%s\n' "$MF_FQN_UNIQUE"
        return 0
    fi
    if [[ -n "$MF_FQN_MULTI" ]]; then
        printf '「%s」存在多个候选，请选择（不擅自猜测）：\n' "$desc"
        mf_fqn_print_multi
        return 3
    fi
    # 零命中：终止并给出原因 + 平台现有清单（FR-NL-004）
    printf '未找到匹配「%s」的项，已终止执行。\n' "$desc"
    printf '平台当前已发布：%s\n' "$(mf_fqn_published_bundles)"
    return 1
}
