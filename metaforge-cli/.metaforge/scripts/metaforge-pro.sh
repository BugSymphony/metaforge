#!/usr/bin/env bash
# metaforge-pro.sh — MetaForge 消费端（metaforge-cli）唯一的对外 CLI 入口。
#
# 设计（FR-DEV-002/005）：单入口 + 命名空间子命令路由，风格与
#   .specify/scripts/bash/speckit-pro.sh 对齐。
# 职责（FR-DEV-001/FR-DLV-009）：本目录（.metaforge/scripts/）是全部 REST 调用
#   的唯一承载处；AI 命令文件（.opencode/commands/metaforge.*.md）与 Skill 定义
#   文件只允许经本入口调用脚本，不得直接出现 REST URL / HTTP 方法 / curl。
#
# 子命令实现位于 lib/（基础库）与 modules/（命名空间模块），按需 source；
# 路由到对应的处理函数，并把函数返回码作为进程退出码透传。

set -uo pipefail

SCRIPT_DIR="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$SCRIPT_DIR/lib"
MODULES_DIR="$SCRIPT_DIR/modules"

# 基础库（存在即加载，支持增量演进）。
for _lib in config.sh http.sh errors.sh fqn-resolve.sh; do
    if [[ -f "$LIB_DIR/$_lib" ]]; then
        # shellcheck source=/dev/null
        source "$LIB_DIR/$_lib"
    fi
done
unset _lib

# 加载配置（默认值 <- 配置文件 <- 环境变量，FR-CFG-003）。
mf_config_load

# 命名空间模块（存在即加载，支持增量演进）。
for _mod in env.sh cognition.sh health.sh; do
    if [[ -f "$MODULES_DIR/$_mod" ]]; then
        # shellcheck source=/dev/null
        source "$MODULES_DIR/$_mod"
    fi
done
unset _mod

usage() {
    cat >&2 <<'EOF'
Usage: metaforge-pro.sh <namespace> <subcommand> [args...] [flags...]

Namespaces:
  env root                           定位项目根（向上搜索 .metaforge/ 标记；META_FORGE_ROOT 可覆盖）
  env summary                        输出 key=value 环境摘要（META_FORGE_ROOT / META_FORGE_SERVER_URL 等）
  cognition execute <templateId>     调用认知查询（POST /api/v1/cognition/{templateId}，camelCase 字段）
  cognition templates                列出服务端实际注册的模板 ID（动态解析，不硬编码）
  cognition resolve <自然语言描述>    基于服务端数据推测 Bundle/主题域/实体 FQN
  health                             检查服务端健康状态（GET /actuator/health）

Flag 与请求字段映射、错误码、退出码约定见 specs/001-metaforge-cli-consumption/contracts/script-cli.md。
EOF
}

main() {
    if [[ $# -lt 1 ]]; then usage; return 1; fi
    local namespace="$1"; shift || true
    local sub="${1:-}"; [[ $# -gt 0 ]] && shift || true

    local fn=""
    case "$namespace" in
        env)
            case "$sub" in
                root)    fn="mf_env_root" ;;
                summary) fn="mf_env_summary" ;;
            esac
            ;;
        cognition)
            case "$sub" in
                execute)  fn="mf_cognition_execute" ;;
                templates) fn="mf_cognition_templates" ;;
                resolve)  fn="mf_cognition_resolve" ;;
            esac
            ;;
        health)
            # health 无子命令；首个参数若是 flag 则视为 health 自身的 flag（透传）
            case "$sub" in
                "" | check) fn="mf_health" ;;
                -*)
                    fn="mf_health"
                    # 将 flag 放回参数队列，交由 mf_health 处理
                    set -- "$sub" "$@"
                    ;;
                *) fn="" ;;
            esac
            ;;
    esac

    if [[ -z "$fn" ]]; then
        echo "错误：未知命令 '$namespace ${sub:-}'" >&2
        usage
        return 1
    fi
    if ! declare -F "$fn" >/dev/null 2>&1; then
        echo "错误：子命令 '$namespace $sub' 的实现（$fn）尚未加载，请确认对应模块已就绪" >&2
        return 1
    fi

    "$fn" "$@"
    return $?
}

main "$@"
exit $?
