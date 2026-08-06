---
name: metaforge-guidance
description: 自由视角组合查询——按需组合任意认知视角子集，获取定制化认知聚合（含 bundle-scope 场景：schema_inventory + instance_catalog）。调用 /metaforge.guidance 命令或 .metaforge/scripts/metaforge-pro.sh。
metadata:
  capability: free-perspective-composition
  template: cognition-guidance
  command: metaforge.guidance
---

## 用途

为 Agent 提供**自由视角组合查询**能力：通过 `--perspectives` 参数按需组合任意认知视角子集（14 个视角），获取恰好符合需求的定制化认知聚合。组合 `schema_inventory` + `instance_catalog` 可获取平台的元模型与实例可视清单（承接原"库范围"查询场景）。内容可直接注入上下文（零解析开销）。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--perspectives <a,b,...>` | 否 | 认知视角子集（如 `schema_inventory,instance_catalog`） |
| `--bundles <a,b,...>` | 否 | Bundle FQN 列表 |
| `--entity-fqn <fqn>` | 否 | 实体 FQN（实体级上下文） |
| `--depth <L1\|L2\|L3>` | 否 | 认知深度（默认 L2） |
| `--max-tokens <n>` | 否 | Token 预算（默认 8000） |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json） |
| `--verbose` | 否 | 排障：输出原始请求/响应与 X-Trace-Id |

## 调用示例

```bash
# 定制组合：Schema 清单 + 实例目录（承接 bundle-scope）
.metaforge/scripts/metaforge-pro.sh cognition execute cognition-guidance \
  --bundles order:1.0.0 --perspectives schema_inventory,instance_catalog

# 含影响追溯视角
.metaforge/scripts/metaforge-pro.sh cognition execute cognition-guidance \
  --bundles order:1.0.0 --entity-fqn order:1.0.0.Step_CheckInventory --perspectives impact_trace,relationship_graph
```

或直接调用命令：`/metaforge.guidance --bundles order:1.0.0 --perspectives schema_inventory,instance_catalog`

## 输出格式

- **json（默认）**: `ApiResponse<T>` 原样透传，恰好包含所请求视角章节
- **视角章节**: `schema_inventory` 元模型结构清单、`instance_catalog` 实例列表、`impact_trace` 影响分层等
- **版本锚**: 各 Bundle 已发布版本号 + 查询时间戳
- **错误**: 服务端错误码与网络错误 → 简体中文提示

## 约束

- 不直接访问服务端（FR-DLV-009）——REST 调用统一经 `.metaforge/scripts/` 脚本承载
- 无状态幂等，可安全重复执行
