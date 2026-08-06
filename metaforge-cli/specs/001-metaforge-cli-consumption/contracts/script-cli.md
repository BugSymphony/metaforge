# Contract: metaforge-pro.sh 脚本 CLI 接口

**版本**: 1.0.0 | **类型**: 本 BC 内部脚本接口（REST 通信唯一承载，FR-DLV-009/FR-DEV）| **日期**: 2026-08-01

> 这是命令/Skill 与脚本层之间的接口契约。命令/Skill 文件只调用本脚本入口，不直接出现 REST 细节。

## 入口

```text
.metaforge/scripts/metaforge-pro.sh <namespace> <subcommand> [flags]
```

## 子命令清单

### env root

获取项目根目录（向上搜索 `.metaforge/` 标记，`META_FORGE_ROOT` 环境变量可覆盖）。

```text
metaforge-pro.sh env root
# 输出: /data/ext/source-8/metaforge
```

### env summary

输出环境摘要（key=value）。

```text
metaforge-pro.sh env summary
# 输出示例:
# META_FORGE_ROOT=/data/ext/source-8/metaforge
# META_FORGE_SERVER_URL=http://localhost:8080
```

### cognition execute \<templateId\>

调用 `POST {server-url}/api/v1/cognition/{templateId}`（camelCase 字段）。

```text
metaforge-pro.sh cognition execute task-brief \
  --bundles order:1.0.0 \
  --entity-fqn order:1.0.0.Step_CheckInventory \
  --depth L2 --archetype execution --max-tokens 8000 \
  --format json --verbose
```

**Flags 与请求字段映射**（FR-DEV-003）：

| CLI flag | 请求字段 | 类型/默认 |
|---|---|---|
| `--bundles <a,b,...>` | `bundleFqns` | string[] |
| `--entity-fqn <fqn>` | `entityFqn` | string |
| `--entity-types <a,b,...>` | `entityTypes` | string[] |
| `--subject-domain <fqn>` | `subjectDomainFqn` | string |
| `--scope-mode <mode>` | `scopeMode` | INHERITED / PURE |
| `--depth <L1\|L2\|L3>` | `cognitionDepth` | 默认 L2 |
| `--archetype <type>` | `agentArchetype` | 默认 execution |
| `--max-tokens <n>` | `maxTokens` | number |
| `--expand <lazy\|all>` | `expand` | 默认 lazy |
| `--format <json\|prompt>` | `format` | 默认 json |
| `--page-size <n>` | `pageSize` | number |
| `--cursor <n>` | `cursor` | number |
| `--json <body>` | 原样请求体 | 覆盖所有 flag |
| `--verbose` | — | 输出原始请求/响应 + X-Trace-Id |

**退出码**: `0` 成功；`1` 业务错误（已映射中文提示）；`2` 用法错误（必填缺失）。

### cognition templates

列出服务端实际注册的模板 ID（动态解析，不硬编码）。

```text
metaforge-pro.sh cognition templates
# 输出: bundle-catalog / cognition-guidance / task-brief / step-guide / navigate / sub-task-brief
```

### cognition resolve \<自然语言描述\>

基于服务端认知接口推测 FQN（Bundle/主题域/实体），输出唯一命中或候选列表；零命中终止并给出原因与平台现有清单。

```text
metaforge-pro.sh cognition resolve "订单处理任务"
# 唯一命中: order:1.0.0.Task_OrderProcessing
# 多候选:  列出 fqn/name/description 请用户选择
# 零命中:  终止 + 原因 + 平台已发布 Bundle 清单
```

### health

检查服务端健康状态（`GET {server-url}/actuator/health`）。

```text
metaforge-pro.sh health
# 输出: HEALTH OK / HEALTH FAIL: <中文原因>
```

## 通用行为契约

- **配置覆盖优先级**: flag > env > config > 默认（FR-020/Q3）
- **瞬时故障重试**: 网络错误/34006/34005 自动重试 1 次（FR-025）
- **错误提示**: 34001~34006 与网络错误 → 简体中文提示，不暴露堆栈（FR-018）
- **幂等**: 无状态，可重复执行（FR-015）
- **认证**: MVP 无认证直连，不硬编码凭据（FR-024）
