---
description: 自由视角组合查询——按需组合任意认知视角子集，获取定制化认知聚合（自由视角组合能力，承接 bundle-scope 场景）
handoffs: 
  - label: 影响感知
    agent: metaforge.step-guide
    prompt: 我已获得定制认知，需要进一步评估某个变更的影响范围
    send: true
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## 用途

本命令为 Agent 提供**自由视角组合查询**能力（spec §4.1 自由视角组合）：针对具体问题，通过 `--perspectives` 参数按需组合任意认知视角子集（共 14 个视角），获取恰好符合需求的定制化认知聚合。预置模板之外的认知诉求均由该能力承载；组合 `schema_inventory` + `instance_catalog` 可获取平台的元模型与实例可视清单（承接原"库范围"查询场景）。输出可直接注入 Agent 上下文（零解析开销）。

**指导的问题**: 针对我的具体问题，定制组合哪些视角？
**指导的行为**: 定制化认知。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--perspectives <a,b,...>` | 否 | 认知视角子集（14 个视角按需组合，如 `schema_inventory,instance_catalog`） |
| `--bundles <a,b,...>` | 否 | Bundle FQN 列表 |
| `--entity-fqn <fqn>` | 否 | 实体 FQN（实体级上下文） |
| `--depth <L1\|L2\|L3>` | 否 | 认知深度（默认 L2） |
| `--max-tokens <n>` | 否 | Token 预算（默认 8000） |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json） |
| `--verbose` | 否 | 输出原始请求/响应与 X-Trace-Id |

## 执行方式

本命令**不直接访问服务端**（FR-DLV-009），仅调用脚本入口：

```bash
# 定制组合：Schema 清单 + 实例目录（承接 bundle-scope）
.metaforge/scripts/metaforge-pro.sh cognition execute cognition-guidance \
  --bundles order:1.0.0 --perspectives schema_inventory,instance_catalog

# 含影响追溯视角（影响感知）
.metaforge/scripts/metaforge-pro.sh cognition execute cognition-guidance \
  --bundles order:1.0.0 --entity-fqn order:1.0.0.Step_CheckInventory --perspectives impact_trace,relationship_graph
```

## 输出说明

- **json（默认）**: 原样透传 `ApiResponse<T>`，恰好包含所请求视角章节
- **视角章节**: 每个视角为独立章节（如 `schema_inventory` 元模型结构清单、`instance_catalog` 实例列表、`impact_trace` 影响分层）
- **版本锚**: 各 Bundle 已发布版本号 + 查询时间戳
- **错误提示**: 服务端错误码与网络错误映射为简体中文提示
