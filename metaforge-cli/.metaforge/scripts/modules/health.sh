#!/usr/bin/env bash
# modules/health.sh — 服务端健康检查（FR-DEV-002 health）。
#
#   health  GET {base-url}/actuator/health
#   输出 HEALTH OK / HEALTH FAIL + 中文原因（不暴露堆栈，FR-018）

set -uo pipefail

mf_health() {
    local verbose=0
    for a in "$@"; do [[ "$a" == "--verbose" ]] && verbose=1; done

    local resp
    local vb=()
    [[ "$verbose" -eq 1 ]] && vb=(--verbose)
    if ! resp="$(mf_http_health "${vb[@]}")"; then
        return 1
    fi

    # 健康端点成功即认为 OK（/actuator/health 返回 200 时 curl 已成功）
    printf 'HEALTH OK\n'
    return 0
}
