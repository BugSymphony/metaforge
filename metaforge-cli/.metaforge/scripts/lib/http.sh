#!/usr/bin/env bash
# lib/http.sh — 全部 REST 通信的唯一承载（FR-REST-001~005、FR-025、FR-DLV-009、D1/D8/R4）。
#
# 端点（上游契约 rest-api.md v1.1.0）：
#   POST {base-url}/api/v1/cognition/{templateId}   — 认知查询（camelCase 字段，D1）
#   GET  {base-url}/actuator/health                 — 健康检查
#
# 行为：
#   - curl 封装，连接/读取超时可配置（默认 3000ms / 10000ms）
#   - X-Trace-Id 透传/生成（FR-REST-004）
#   - 瞬时故障自动重试 1 次：网络错误 / 34005 / 34006（FR-025）
#   - ApiResponse<T> 解析：code=200 视为成功（FR-REST-003）
#   - data_version_anchors map/array 双形态兼容（R4/D8）
#   - 无认证直连，不硬编码凭据（FR-024）
#
# 用法（被 modules/*.sh 调用）：
#   mf_http_post_json <templateId> <json_body> [--verbose]
#   mf_http_get <path> [--verbose]
#   mf_http_extract_anchors <response_json>
#   mf_http_gen_trace_id

set -uo pipefail

MF_HTTP_BASE_URL=""
MF_HTTP_LAST_TRACE_ID=""
MF_HTTP_LAST_RESPONSE=""

# 生成链路追踪 ID（FR-REST-004）：时间戳 + 进程号 + 随机数。
mf_http_gen_trace_id() {
    printf '%s-%s-%s' "$(date +%s)" "$$" "$((RANDOM % 100000))"
}

# 基础 curl 参数（超时 / 追踪头）。全局 MF_HTTP_BASE_URL 由调用方经 mf_http_init 设置。
mf_http_init() {
    MF_HTTP_BASE_URL="$(mf_config_get server.base_url)"
    MF_HTTP_TRACE_ID="${META_FORGE_TRACE_ID:-$(mf_http_gen_trace_id)}"
    return 0
}

# 执行 curl 并解析响应。成功（curl 层）返回 0；网络失败返回非 0 且 MF_HTTP_LAST_RESPONSE 为空。
# 透传 --verbose 时输出原始请求/响应（NFR-006）。
mf_http_curl() {
    local method="$1" url="$2" payload="${3:-}"
    shift 3
    local verbose=0
    local connect_ms="$(mf_config_get server.connect_ms)"
    local read_ms="$(mf_config_get server.timeout_ms)"
    local args=()
    for a in "$@"; do
        [[ "$a" == "--verbose" ]] && verbose=1
    done

    local headers=(-H "Content-Type: application/json" -H "Accept: application/json" -H "X-Trace-Id: $MF_HTTP_TRACE_ID")

    if [[ "$verbose" -eq 1 ]]; then
        printf '\n[verbose] %s %s\n' "$method" "$url" >&2
        [[ -n "$payload" ]] && printf '[verbose] request-body: %s\n' "$payload" >&2
    fi

    if [[ "$method" == "POST" ]]; then
        MF_HTTP_LAST_RESPONSE="$(curl -s -m "$read_ms" --connect-timeout "$connect_ms" \
            -X POST "$url" "${headers[@]}" -d "$payload")"
    else
        MF_HTTP_LAST_RESPONSE="$(curl -s -m "$read_ms" --connect-timeout "$connect_ms" \
            "$url" "${headers[@]}")"
    fi
    local curl_rc=$?

    if [[ "$curl_rc" -ne 0 ]]; then
        MF_HTTP_LAST_RESPONSE=""
        return 1
    fi
    if [[ "$verbose" -eq 1 ]]; then
        printf '[verbose] response: %s\n' "$MF_HTTP_LAST_RESPONSE" >&2
    fi
    return 0
}

# 解析 ApiResponse<T> 的 code（FR-REST-003）。解析失败输出 0（未知）。
mf_http_extract_code() {
    local resp="${1:-}"
    if command -v python3 >/dev/null 2>&1; then
        printf '%s' "$resp" | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get("code", 0))
except Exception:
    print("0")
' 2>/dev/null || printf '0'
    else
        printf '%s' "$resp" | sed -n 's/.*"code"[[:space:]]*:[[:space:]]*\([0-9]*\).*/\1/p'
    fi
}

# 解析 ApiResponse<T> 的 message 与 data（用于错误展示）。
mf_http_extract_message() {
    local resp="${1:-}"
    if command -v python3 >/dev/null 2>&1; then
        printf '%s' "$resp" | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get("message", ""))
except Exception:
    print("")
' 2>/dev/null || printf ''
    fi
}

# 执行认知查询 POST /api/v1/cognition/{templateId}，含瞬时故障重试 1 次（FR-025）。
# 成功（code=200）返回 0 并将响应存于 MF_HTTP_LAST_RESPONSE；业务错误返回 1。
# --raw：无论 code 均将响应体 echo 到 stdout（供模板探测等场景使用）。
mf_http_post_cognition() {
    local template_id="$1" payload="$2"
    shift 2
    local verbose=0 raw=0
    for a in "$@"; do
        case "$a" in
            --verbose) verbose=1 ;;
            --raw) raw=1 ;;
        esac
    done

    mf_http_init
    local url="$MF_HTTP_BASE_URL/api/v1/cognition/$template_id"
    local attempt=0 code=0
    local vb=()
    [[ "$verbose" -eq 1 ]] && vb=(--verbose)

    for attempt in 1 2; do
        if ! mf_http_curl POST "$url" "$payload" "${vb[@]}"; then
            if [[ "$attempt" -eq 1 ]]; then
                continue   # 网络瞬时错误，重试 1 次
            fi
            mf_http_network_error_dump "$MF_HTTP_BASE_URL"
            return 1
        fi
        code="$(mf_http_extract_code "$MF_HTTP_LAST_RESPONSE")"
        if [[ "$code" == "34005" || "$code" == "34006" ]]; then
            if [[ "$attempt" -eq 1 ]]; then
                continue   # 视角超时 / 上游不可用，重试 1 次
            fi
        fi
        break
    done

    if [[ "$code" == "200" ]]; then
        printf '%s' "$MF_HTTP_LAST_RESPONSE"
        return 0
    fi
    if [[ "$raw" -eq 1 ]]; then
        printf '%s' "$MF_HTTP_LAST_RESPONSE"
        return 0
    fi
    # 34001 的 detail 用 templateId（FR-ERR-001 映射示例）；其余用服务端 message
    local detail
    if [[ "$code" == "34001" ]]; then
        detail="$template_id"
    else
        detail="$(mf_http_extract_message "$MF_HTTP_LAST_RESPONSE")"
    fi
    mf_error_dump "$code" "$detail" "$(mf_http_extract_data "$MF_HTTP_LAST_RESPONSE")"
    return 1
}

# 网络错误提示（服务不可达，FR-ERR-001）。
mf_http_network_error_dump() {
    local url="${1:-}"
    printf '错误：' >&2
    mf_network_error_message "$url" >&2
    printf '\n' >&2
    return 0
}

# 健康检查 GET /actuator/health（FR-DEV-002 health）。
mf_http_health() {
    mf_http_init
    local url="$MF_HTTP_BASE_URL/actuator/health"
    local vb=()
    [[ "${1:-}" == "--verbose" ]] && vb=(--verbose)
    if ! mf_http_curl GET "$url" "" "${vb[@]}"; then
        mf_http_network_error_dump "$MF_HTTP_BASE_URL"
        return 1
    fi
    printf '%s' "$MF_HTTP_LAST_RESPONSE"
    return 0
}

# 提取 ApiResponse<T> 的 data 字段（JSON 对象，供 34004 候选等展示）。
mf_http_extract_data() {
    local resp="${1:-}"
    if command -v python3 >/dev/null 2>&1; then
        printf '%s' "$resp" | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
    print(json.dumps(d.get("data", {}), ensure_ascii=False))
except Exception:
    print("{}")
' 2>/dev/null || printf '{}'
    fi
}

# 提取并规范 data_version_anchors（R4/D8 双形态兼容）。
# 契约示例（map）：{"order": {"version": "1.0.0", "queriedAt": "..."}}
# mock（array）：  [{"bundleFqn": "order:1.0.0", "bundle": "order", "publishedVersion": "...", "queriedAt": "..."}]
# 输出统一为每行："<bundle> <publishedVersion> <queriedAt>"。
mf_http_extract_anchors() {
    local resp="${1:-}"
    if command -v python3 >/dev/null 2>&1; then
        printf '%s' "$resp" | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
    cm = (d.get("data") or {}).get("context_meta") or (d.get("data") or {}).get("contextMeta") or {}
    anchors = cm.get("data_version_anchors") or cm.get("dataVersionAnchors") or {}
except Exception:
    sys.exit(0)
if isinstance(anchors, list):
    for a in anchors:
        if not isinstance(a, dict):
            continue
        b = a.get("bundle") or (a.get("bundleFqn") or "").split(":")[0]
        v = a.get("publishedVersion") or a.get("version") or ""
        t = a.get("queriedAt") or ""
        print("%s %s %s" % (b, v, t))
elif isinstance(anchors, dict):
    for b, info in anchors.items():
        if isinstance(info, dict):
            print("%s %s %s" % (b, info.get("version", ""), info.get("queriedAt", "")))
        else:
            print("%s %s " % (b, info))
' 2>/dev/null
    fi
}

# 版本锚过期对比（FR-VER-002/FR-007、T033/R4）。
# 输入：两份版本锚文本（mf_http_extract_anchors 输出，每行 "bundle version queriedAt"）。
# 输出：发生版本变化的 Bundle 清单（每行一行）；一致则无输出。
mf_http_anchors_diff() {
    local a="${1:-}" b="${2:-}"
    printf '%s\n--SEP--\n%s\n' "$a" "$b" | python3 -c '
import sys
def parse(text):
    m = {}
    for line in text.splitlines():
        parts = line.split()
        if len(parts) >= 2:
            m[parts[0]] = parts[1]
    return m
text = sys.stdin.read()
if "--SEP--" not in text:
    sys.exit(0)
a_text, b_text = text.split("--SEP--", 1)
a = parse(a_text)
b = parse(b_text)
changed = [k for k in b if k in a and a[k] != b[k]]
for k in changed:
    print(k)
'
    return 0
}
