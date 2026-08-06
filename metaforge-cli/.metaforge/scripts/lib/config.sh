#!/usr/bin/env bash
# lib/config.sh — metaforge-cli 配置加载与合并（FR-CFG-001/002/003，Q3/D5）。
#
# 覆盖优先级（固定）：命令 flag > 环境变量 > 配置文件 > 默认值（FR-CFG-003）。
# 配置源：
#   - 用户级配置文件  ~/.config/metaforge/config.yml（Q3）
#   - 环境变量        META_FORGE_SERVER_URL / META_FORGE_CONNECT_MS / META_FORGE_TIMEOUT_MS
#   - 默认值          base-url http://localhost:8080、depth L2、archetype execution、
#                     max-tokens 8000、connect 3000、read 10000、page-size 20、
#                     format json、expand lazy
#
# 用法（由 metaforge-pro.sh 在模块之前 source）：
#   mf_config_load                 # 加载 默认值<-配置文件<-环境变量 到全局关联数组 MF_CFG
#   mf_config_get <key>            # 输出有效配置值（已含 env/config/默认）
#   mf_config_set <key> <value>    # 命令 flag 覆盖（优先级最高，键值校验通过才写入）

set -uo pipefail

MF_CONFIG_FILE="${META_FORGE_CONFIG:-$HOME/.config/metaforge/config.yml}"

# 默认配置（FR-CFG-002）
declare -gA MF_CFG=(
    [server.base_url]="http://localhost:8080"
    [server.connect_ms]="3000"
    [server.timeout_ms]="10000"
    [default.depth]="L2"
    [default.archetype]="execution"
    [default.max_tokens]="8000"
    [default.page_size]="20"
    [default.format]="json"
    [default.expand]="lazy"
)

# 规范化配置键：将 '-' 与 '_' 统一为 '_'，去除首尾空白。
# 示例：'server.base-url' -> 'server.base_url'；'server.base_url' 保持原样。
mf_config_normalize_key() {
    printf '%s' "$1" | tr '-' '_'
}

# 已知合法配置键集合（用于 flag/文件/env 写入校验，防止拼写漂移）。
mf_config_is_valid_key() {
    case "$1" in
        server.base_url|server.connect_ms|server.timeout_ms| \
        default.depth|default.archetype|default.max_tokens| \
        default.page_size|default.format|default.expand) return 0 ;;
        *) return 1 ;;
    esac
}

# 从配置文件加载（~/.config/metaforge/config.yml）。
# 兼容两种写法：
#   flat:  server.base-url: http://localhost:8080
#   yaml:  server:
#            base-url: http://localhost:8080
# 忽略空行与注释（# 开头）。
mf_config_load_file() {
    [[ -f "$MF_CONFIG_FILE" ]] || return 0
    local line key val cur_sub
    local section=""
    while IFS= read -r line || [[ -n "$line" ]]; do
        line="$(printf '%s' "$line" | sed 's/[[:space:]]*$//')"
        [[ -n "${line//[[:space:]]/}" ]] || continue
        [[ "$(printf '%s' "$line" | sed 's/^[[:space:]]*//')" == \#* ]] && continue
        # 嵌套行：`  key: value`（当前处于某 section 下）
        if [[ "$line" =~ ^[[:space:]]+([A-Za-z0-9_.-]+):[[:space:]]*(.*)$ ]] && [[ -n "$section" ]]; then
            cur_sub="$(mf_config_normalize_key "${BASH_REMATCH[1]}")"
            val="${BASH_REMATCH[2]//\"/}"
            val="${val//\'/}"
            val="$(printf '%s' "$val" | sed 's/[[:space:]]*$//')"
            local full_key="${section}.${cur_sub}"
            if mf_config_is_valid_key "$full_key"; then
                MF_CFG["$full_key"]="$val"
            fi
            continue
        fi
        # 顶层行：`key: value`（flat 全键或 yaml section）
        if [[ "$line" =~ ^([A-Za-z0-9_.-]+):[[:space:]]*(.*)$ ]]; then
            key="$(mf_config_normalize_key "${BASH_REMATCH[1]}")"
            val="${BASH_REMATCH[2]//\"/}"
            val="${val//\'/}"
            val="$(printf '%s' "$val" | sed 's/[[:space:]]*$//')"
            if mf_config_is_valid_key "$key"; then
                MF_CFG["$key"]="$val"
                section=""          # flat 全键，不进入嵌套
            else
                section="$key"      # 作为 yaml section，后续嵌套行挂接
            fi
        fi
    done < "$MF_CONFIG_FILE"
    return 0
}

# 从环境变量加载（覆盖配置文件）。
mf_config_load_env() {
    [[ -n "${META_FORGE_SERVER_URL:-}" ]] && MF_CFG[server.base_url]="$META_FORGE_SERVER_URL"
    [[ -n "${META_FORGE_CONNECT_MS:-}" ]] && MF_CFG[server.connect_ms]="$META_FORGE_CONNECT_MS"
    [[ -n "${META_FORGE_TIMEOUT_MS:-}" ]] && MF_CFG[server.timeout_ms]="$META_FORGE_TIMEOUT_MS"
    return 0
}

# 加载全部配置源（默认 <- 配置文件 <- 环境变量）。
mf_config_load() {
    mf_config_load_file
    mf_config_load_env
    return 0
}

# 输出有效配置值。
mf_config_get() {
    local key="$(mf_config_normalize_key "$1")"
    printf '%s' "${MF_CFG[$key]:-}"
}

# 命令 flag 覆盖（最高优先级）。仅接受合法键，非法键静默忽略。
mf_config_set() {
    local key="$(mf_config_normalize_key "$1")"
    local val="${2:-}"
    mf_config_is_valid_key "$key" || return 1
    MF_CFG["$key"]="$val"
    return 0
}
