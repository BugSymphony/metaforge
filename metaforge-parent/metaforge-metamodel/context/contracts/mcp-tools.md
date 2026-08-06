---
id: metamodel-governance.mcp-tools
protocol: MCP (Model Context Protocol)
version: 1.0.0
owner: metamodel-governance
description: 提供元模型治理 BC 通过 Spring AI 暴露的 MCP 工具集契约，覆盖元模型元素查询、FQN 解析、导出清单查询与版本校验等语义查询能力，由 agent-consumption BC 消费。
type: business
---

# MCP Tools Contract: metamodel-governance

**Protocol**: MCP (Model Context Protocol, Spring AI)
**Publisher**: `metamodel-governance`（工具定义方）/ `agent-consumption`（统一代理发布方）
**Version**: 1.0.0

> `metamodel-governance` BC 通过 Spring AI 将以下语义查询能力暴露为 MCP Server 工具集。MCP Tools 由 `agent-consumption` BC 消费（跨 BC 调用），最终 Agent 通过 MCP 协议获取结构化语义结果。

---

## Capability Overview

本契约定义元模型语义查询工具集 `metamodel-query`，覆盖以下能力：元模型元素结构查询、元素列表过滤查询、关系定义查询、Bundle 版本列表查询、导出清单查询、FQN 解析、属性模板组查询、版本校验。

---

## Tool List

### 工具集: metamodel-query

#### 1. getElementSchema

查询 EntitySchema 或 RelationSchema 的完整结构定义（含已发布的 JSON Schema）。

| 属性 | 值 |
|------|-----|
| **工具名** | `getElementSchema` |
| **描述** | 按 FQN 查询元模型元素的完整定义，包含属性结构、JSON Schema 及关联信息 |
| **输入参数** | `fqn` (String, required) — 元素的纯净 FQN 或带类型前缀的 FQN |
| **输出格式** | 结构化 JSON（含 properties、required、jsonSchema 等字段） |

**输入示例**:
```json
{
  "fqn": "order:1.0.0.pkg_order.Order"
}
```

**输出示例**:
```json
{
  "fqn": "order:1.0.0.pkg_order.Order",
  "type": "EntitySchema",
  "name": "订单实体",
  "description": "描述电商订单的核心概念...",
  "bundleCode": "order",
  "version": "1.0.0",
  "packagePath": "pkg_order",
  "jsonSchema": {
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "type": "object",
    "properties": {
      "orderAmount": { "type": "number", "minimum": 0 },
      "status": { "type": "string", "enum": ["pending", "confirmed", "shipped"] }
    },
    "required": ["orderAmount", "status"]
  }
}
```

---

#### 2. queryElements

分页查询元模型元素列表。支持 FQN 前缀集合过滤（多值 OR 逻辑）。

| 属性 | 值 |
|------|-----|
| **工具名** | `queryElements` |
| **描述** | 查询元模型元素列表，支持 FQN 前缀集合批量过滤 |
| **输入参数** | `fqnPrefixes` (List\<String\>, optional) — FQN 前缀集合，如 `["order:1.0.0.pkg_order.", "order:1.0.0.pkg_common."]`；`elementType` (String, optional) — ENTITY_SCHEMA / RELATION_SCHEMA；`page` (int, default=1)；`size` (int, default=20) |
| **输出格式** | 分页结果，包含元素列表和 total 计数 |

**输出示例**:
```json
{
  "content": [
    {
      "fqn": "order:1.0.0.pkg_order.Order",
      "type": "EntitySchema",
      "name": "订单实体",
      "shortName": "Order"
    },
    {
      "fqn": "order:1.0.0.pkg_order.Item",
      "type": "EntitySchema",
      "name": "订单项",
      "shortName": "Item"
    }
  ],
  "total": 5,
  "page": 1,
  "size": 20,
  "totalPages": 1
}
```

---

#### 3. getRelationSchema

查询两个实体之间的关联定义。

| 属性 | 值 |
|------|-----|
| **工具名** | `getRelationSchema` |
| **描述** | 查询指定的 RelationSchema 详情，包括关联类型、基数及关联两端实体信息 |
| **输入参数** | `fqn` (String, required) — RelationSchema 的 FQN |
| **输出格式** | 结构化 JSON，含 source/target 实体引用及关联约束 |

**输出示例**:
```json
{
  "fqn": "order:1.0.0.pkg_order.Order_contains_Item",
  "type": "RelationSchema",
  "name": "订单包含商品",
  "description": "订单与订单项之间的组成关系",
  "source": {
    "fqn": "order:1.0.0.pkg_order.Order",
    "name": "订单实体",
    "shortName": "Order"
  },
  "target": {
    "fqn": "order:1.0.0.pkg_order.Item",
    "name": "订单项",
    "shortName": "Item"
  },
  "associationType": "组成",
  "cardinality": {
    "source": "1",
    "target": "1..*"
  },
  "jsonSchema": { ... }
}
```

---

#### 4. listBundleVersions

查询 Bundle 的版本列表。

| 属性 | 值 |
|------|-----|
| **工具名** | `listBundleVersions` |
| **描述** | 查询指定 Bundle 的所有已发布版本（降序排列） |
| **输入参数** | `bundleFqn` (String, required) — Bundle 的 code；`includeDraft` (boolean, default=false) — 是否包含草稿版本 |
| **输出格式** | 版本列表，含版本号和状态 |

---

#### 5. getExportManifest

查询导出清单。

| 属性 | 值 |
|------|-----|
| **工具名** | `getExportManifest` |
| **描述** | 查询指定 Bundle 版本的导出清单（对外可见的 Package 命名空间） |
| **输入参数** | `versionFqn` (String, required) — BundleVersion FQN |
| **输出格式** | 导出 Package FQN 列表 |

**输出示例**:
```json
{
  "bundleVersionFqn": "order:1.0.0",
  "status": "PUBLISHED",
  "exportedPackages": [
    {
      "fqn": "order:1.0.0.pkg_order",
      "description": "订单领域子包"
    },
    {
      "fqn": "order:1.0.0.pkg_common",
      "description": "通用业务语义"
    }
  ]
}
```

---

#### 6. resolveFqn

FQN 解析工具。

| 属性 | 值 |
|------|-----|
| **工具名** | `resolveFqn` |
| **描述** | 解析 FQN（支持版本省略语法），返回完整带版本的 FQN 及各组成部分 |
| **输入参数** | `fqn` (String, required) — 完整或省略版本的 FQN |
| **输出格式** | 解析结果，含 bundleCode、version、segments、shortName、parentFqn、filePath |

**输入示例**:
```json
{
  "fqn": "order.pkg_order.Order"
}
```

**输出示例**:
```json
{
  "inputFqn": "order.pkg_order.Order",
  "resolvedFqn": "order:1.0.0.pkg_order.Order",
  "bundleCode": "order",
  "version": "1.0.0",
  "segments": ["pkg_order"],
  "shortName": "Order",
  "parentFqn": "order:1.0.0.pkg_order",
  "filePath": "order/1.0.0/pkg_order/Order.json"
}
```

---

#### 7. getAttributeTemplate

查询属性模板组定义。

| 属性 | 值 |
|------|-----|
| **工具名** | `getAttributeTemplate` |
| **描述** | 查询指定 AttributeTemplate 的属性定义集合 |
| **输入参数** | `fqn` (String, required) — AttributeTemplate 的 FQN |
| **输出格式** | 属性定义列表 |

---

#### 8. validateVersion

校验 Bundle 版本。

| 属性 | 值 |
|------|-----|
| **工具名** | `validateVersion` |
| **描述** | 对草稿版本执行发布前全量校验，返回校验报告 |
| **输入参数** | `versionFqn` (String, required) — 草稿版本 FQN |
| **输出格式** | 校验报告（含错误和警告列表） |

---

## MCP Tool Registration

通过 Spring AI 的 `@Tool` 注解注册：

```java
@Component
public class MetamodelMcpTools {

    @Tool(description = "按 FQN 查询元模型元素的完整定义")
    public ElementSchemaDto getElementSchema(
        @ToolParam(description = "元素的纯净 FQN 或带类型前缀的 FQN") String fqn
    ) { ... }

    @Tool(description = "查询指定 Bundle 版本内的元模型元素列表")
    public PageResult<ElementSummaryDto> queryElements(
        @ToolParam(description = "版本 FQN") String bundleVersionFqn,
        @ToolParam(description = "元素类型: ENTITY_SCHEMA / RELATION_SCHEMA") String elementType,
        @ToolParam(description = "Package FQN 过滤") String packageFqn,
        @ToolParam(description = "页码") int page,
        @ToolParam(description = "每页大小") int size
    ) { ... }

    @Tool(description = "查询 RelationSchema 详情")
    public RelationSchemaDto getRelationSchema(
        @ToolParam(description = "RelationSchema 的 FQN") String fqn
    ) { ... }

    @Tool(description = "解析 FQN（支持版本省略）")
    public FqnResolveResult resolveFqn(
        @ToolParam(description = "完整或省略版本的 FQN") String fqn
    ) { ... }
}
```

---

## 与 agent-consumption BC 的协作

`metamodel-governance` 的 MCP Tools 由 `agent-consumption` BC 统一代理发布。数据流：

```
Agent 请求 → agent-consumption BC (白名单过滤)
    → metamodel-governance MCP Tools (语义查询)
    → 返回结构化结果 → agent-consumption (格式化输出)
    → Agent 上下文注入
```

每条 MCP 工具调用均由 `agent-consumption` 执行权限过滤后方可访问底层数据。
