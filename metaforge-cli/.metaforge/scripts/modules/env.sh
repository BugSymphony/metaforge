#!/usr/bin/env bash
# modules/env.sh — 开发态环境：env root / env summary（FR-DEV-002、FR-DEV-004）。
#
#   env root      向上搜索 .metaforge/ 标记定位项目根；META_FORGE_ROOT 环境变量可覆盖
#   env summary   输出 key=value 环境摘要（META_FORGE_ROOT / META_FORGE_SERVER_URL 等）

set -uo pipefail

# 定位项目根：优先 META_FORGE_ROOT，否则从当前目录向上搜索 .metaforge/ 标记。
# 搜索终止于根目录。未找到返回 1。
mf_env_find_root() {
    if [[ -n "${META_FORGE_ROOT:-}" ]]; then
        printf '%s' "$META_FORGE_ROOT"
        return 0
    fi
    local dir="$(pwd -P)"
    while true; do
        if [[ -d "$dir/.metaforge" ]]; then
            printf '%s' "$dir"
            return 0
        fi
        [[ "$dir" == "/" ]] && break
        dir="$(dirname "$dir")"
    done
    return 1
}

mf_env_root() {
    local root
    if root="$(mf_env_find_root)"; then
        printf '%s\n' "$root"
        return 0
    fi
    printf '错误：未定位到项目根（未找到 .metaforge/ 标记，且未设置 META_FORGE_ROOT）\n' >&2
    return 1
}

mf_env_summary() {
    local root server
    if root="$(mf_env_find_root)"; then
        printf 'META_FORGE_ROOT=%s\n' "$root"
    else
        printf 'META_FORGE_ROOT=（未定位）\n'
    fi
    server="$(mf_config_get server.base_url)"
    printf 'META_FORGE_SERVER_URL=%s\n' "$server"
    printf 'META_FORGE_CONNECT_MS=%s\n' "$(mf_config_get server.connect_ms)"
    printf 'META_FORGE_TIMEOUT_MS=%s\n' "$(mf_config_get server.timeout_ms)"
    printf 'META_FORGE_CONFIG=%s\n' "$MF_CONFIG_FILE"
    return 0
}
