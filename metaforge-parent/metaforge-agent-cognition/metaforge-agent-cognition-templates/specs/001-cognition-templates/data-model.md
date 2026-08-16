# Data Model: 认知模板配置层

**Feature**: cognition-templates | **Phase**: 1 | **Date**: 2026-08-11

> 本 BC 为纯配置层，数据模型仅描述 YAML 文件中声明的模板结构实体及其字段约束。不涉及数据库存储、对象关系映射或运行时状态管理。

## 实体关系图

```
认知模板 (CognitionTemplate)
    │
    ├── 1:N ── 算子条目 (OperatorEntry) ──── 引用 ──── operatorId (上游契约)
    │
    ├── 1:1 ── 输入契约 (InputSchema)         —— JSON Schema Draft 2020-12
    │
    ├── 1:1 ── Scope 行为 (ScopeBehavior)
    │
    ├── 1:1 ── 输出结构 (OutputSchema)
    │
    └── 1:1 ── 上下文元数据规则 (ContextMeta)
```

## 1. 认知模板 (CognitionTemplate)

核心聚合根，代表一个完整的 Agent 消费场景声明。

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `templateId` | string | **是** | 全局唯一；大写字母 + 数字 + 下划线；正则: `^[A-Z][A-Z0-9_]*$` |
| `templateName` | string | **是** | 简体中文显示名，如 `"元模型发现"` |
| `description` | string | **是** | 功能描述，说明 Agent 在什么场景下使用 |
| `version` | string | 否 | 语义化版本号，如 `"1.0.0"` |
| `stage` | string | 否 | 枚举: `P0` / `P1` / `P2` |
| `enabled` | boolean | 否 | 默认 `true`；`false` 时注册但不路由 |
| `operators` | OperatorEntry[] | **是** | 非空数组；跨分类扁平列表 |
| `inputSchema` | InputSchema | **是** | JSON Schema Draft 2020-12 对象 |
| `scopeBehavior` | ScopeBehavior | **是** | scope 行为声明 |
| `outputSchema` | OutputSchema | **是** | 输出结构定义 |
| `contextMeta` | ContextMeta | **是** | 上下文元数据生成规则 |

**状态转换**: 无运行时状态。模板以 YAML 文件存在，其生命周期由 `enabled` 字段控制（启用/停用），或通过文件增删控制（注册/移除）。

**标识规则**: `templateId` 全局唯一。重复 templateId → 校验失败，第二个文件被拒绝注册并告警（第一个保留）。

## 2. 算子条目 (OperatorEntry)

模板中引用的单个认知算子声明。一个模板可包含 2–10 个算子条目。

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `operatorId` | string | **是** | 格式: `{category}.{能力名}`；必须存在于 operator-catalog-contract 的 24 个算子清单中 |
| `priority` | int | 否 | 非负整数，默认 0；数值越大越优先 |
| `required` | boolean | **是** | `true` = 必须成功，失败导致模板整体失败；`false` = 失败仅跳过该算子 |
| `timeoutMs` | int | 否 | 该算子单独超时毫秒数；不设则使用全局默认 |
| `archetypes` | string[] | **是** | 非空数组；封闭枚举 `{execution, exploration, audit, orchestration}` 的子集 |
| `description` | string | 否 | 可读说明文本 |

**交叉约束**:
- `operators` 数组中至少包含一个 `required: true` 的算子，否则模板无强制保留算子
- `archetypes` 不能为空数组（空数组 = 算子对任何 Agent 原型都不可执行，模板整体不应注册）

## 3. 输入契约 (InputSchema)

JSON Schema Draft 2020-12 格式的入参定义。

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `type` | string | **是** | 固定值 `"object"` |
| `required` | string[] | 否 | 必填参数名列表，列出的参数名必须在 `properties` 中存在 |
| `properties` | object | **是** | 参数名 → 参数定义的映射，每个参数的 `type` 必须为合法 JSON Schema 类型 (`string` / `integer` / `boolean` / `object` / `array` / `number`)，可选 `description`、`default`、`enum`、`maximum` 等约束字段 |

**交叉约束**:
- `required` 中声明但 `properties` 中不存在的字段 → 模板结构自相矛盾，视为无效
- 各模板的 `inputSchema` 差异仅在 `required` 和 `properties` 内容上，顶层结构一致

## 4. Scope 行为声明 (ScopeBehavior)

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `acceptsScope` | boolean | **是** | 是否接受 scope 入参。所有 6 个模板均为 `true` |
| `scopeRequired` | boolean | **是** | scope 是否必填。仅 `DELEGATE` 为 `true` |
| `producesUpdatedScope` | boolean | **是** | 是否产出 updated_scope。`DISCOVER` / `ORIENT` / `DELEGATE` 为 `true` |
| `scopeFields` | string[] | **是** | 非空数组；封闭枚举 `{bundles, packages, domain_groups, domains, entity_schemas}` 的子集 |
| `description` | string | 否 | scope 行为说明 |

**交叉约束**:
- FR-007: `scopeRequired: true` 时 `acceptsScope` 自动视为 `true`，无论声明值
- `producesUpdatedScope: true` 时输出 `context_meta.updated_scope`，记录执行过程中发现的 entities/bundles/domains

## 5. 输出结构 (OutputSchema)

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `type` | string | **是** | 输出结构类型名，如 `DISCOVER_RESULT`。命名约定: `{TEMPLATE_ID}_RESULT` |
| `formats` | string[] | **是** | 非空数组；至少包含 `["json", "prompt"]` |

## 6. 上下文元数据规则 (ContextMeta)

| 字段 | 类型 | 必填 | 约束 |
|------|------|------|------|
| `includeVersionAnchors` | boolean | **是** | 是否在 context_meta 中包含 version_anchors |
| `includeScopeApplied` | boolean | **是** | 是否在 context_meta 中包含 scope_applied |
| `includeTokenEstimate` | boolean | **是** | 是否在 context_meta 中包含 token_estimate |
| `includeSkippedEntities` | boolean | 否 | 是否在 context_meta 中包含 skipped_entities 列表。仅 `BRIEF` 和 `DELEGATE` 为 `true` |
