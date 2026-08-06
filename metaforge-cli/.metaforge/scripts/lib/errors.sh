#!/usr/bin/env bash
# lib/errors.sh — 服务端错误码与网络错误 → 简体中文提示映射（FR-ERR-001/018/021，NFR-006/007）。
#
# 错误码（上游契约 rest-api.md v1.1.0）：
#   34001 模板未注册 / 34002 Bundle FQN 非法 / 34003 Bundle FQN 列表为空
#   34004 实体 FQN 不属于任何已发布 Bundle / 34005 单视角超时(200ms) / 34006 上游 BC 不可用
# 网络错误：服务端不可达。
#
# 用法：
#   mf_error_message <code> [detail]    # 输出映射后的中文提示（无 detail 时给出通用示例）
#   mf_error_dump <code> <message> [data_json]   # 输出完整中文错误（含 34004 候选列表）
#   mf_error_prompt <code> <message> [data_json] # 同上，前缀 "错误:"

set -uo pipefail

# 错误码 → 中文提示（FR-ERR-001 映射表）。
# detail 可用 {detail} 占位替换。
mf_error_message() {
    local code="$1"
    local detail="${2:-}"
    local msg=""
    case "$code" in
        34001) msg="模板 {detail} 不存在，请检查模板 ID" ;;
        34002) msg="Bundle FQN 格式非法：{detail}" ;;
        34003) msg="请至少指定一个 Bundle" ;;
        34004) msg="实体 FQN 归属校验失败，请检查（可参考候选列表）" ;;
        34005) msg="部分视角查询超时，已截断" ;;
        34006) msg="上游服务暂不可用，请稍后重试" ;;
        0)     msg="服务端返回未知响应" ;;
        *)     msg="服务端返回错误码 {detail}" ;;
    esac
    if [[ -n "$detail" ]]; then
        msg="${msg//\{detail\}/$detail}"
    else
        msg="${msg//\{detail\}/}"
    fi
    printf '%s' "$msg"
}

# 网络错误提示（FR-ERR-001 网络错误行）。
mf_network_error_message() {
    local url="${1:-}"
    if [[ -n "$url" ]]; then
        printf '无法连接 MetaForge 服务端：%s' "$url"
    else
        printf '无法连接 MetaForge 服务端'
    fi
}

# 输出完整中文错误信息（到 stderr）。data_json 用于 34004 候选列表展示（FR-ERR-001）。
mf_error_dump() {
    local code="$1"
    local message="${2:-}"
    local data="${3:-}"
    local detail="$message"
    printf '错误：' >&2
    mf_error_message "$code" "$detail" >&2
    printf '\n' >&2
    if [[ "$code" == "34004" ]] && [[ -n "$data" ]]; then
        mf_error_candidates "$data"
    fi
    return 0
}

# 从 34004 响应 data 中提取并展示候选列表（data.candidates 或 data 为数组时兼容）。
mf_error_candidates() {
    local data="$1"
    local candidates=""
    if command -v python3 >/dev/null 2>&1; then
        candidates="$(printf '%s' "$data" | python3 -c '
import json, sys
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit(0)
c = d.get("candidates") if isinstance(d, dict) else d
if isinstance(c, list):
    for x in c:
        print(x)
')" 2>/dev/null || candidates=""
    fi
    if [[ -n "$candidates" ]]; then
        printf '候选列表：\n' >&2
        while IFS= read -r c; do
            [[ -n "$c" ]] && printf '  - %s\n' "$c" >&2
        done <<< "$candidates"
    fi
    return 0
}

# 输出完整中文错误信息（前缀"错误:"，到 stderr）。
mf_error_prompt() {
    mf_error_dump "$@"
}
