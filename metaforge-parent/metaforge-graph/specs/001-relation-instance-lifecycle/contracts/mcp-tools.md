# MCP 工具契约：metaforge-graph

**Protocol**: MCP (Model Context Protocol)
**Module**: `metaforge-graph-core` (MCP Tool Provider)
**发布方**: `agent-consumption` BC（统一聚合发布）
**Version**: 1.0.0

> **注意**: 全局架构约束要求 MCP Server 由 `agent-consumption` BC 统一发布。本 BC 通过内部 MCP 工具提供者（`GraphMcpToolProvider`）声明工具语义与签名，最终由 `agent-consumption` BC 通过 Spring AI 聚合发布为 MCP 工具集。本契约定义的工具即为对外承诺的 MCP 能力边界。

---

## 工具概览

| 工具名 | 描述 | 适用场景 |
|--------|------|----------|
| `get_relation_by_fqn` | 精准查询单条关系实例 | Agent 需获取特定关系的完整上下文 |
| `list_outbound_relations` | 查询指定实体的出边关系 | Agent 浏览实体对外关联的语义拓扑 |
| `list_inbound_relations` | 查询指定实体的入边关系 | Agent 追踪实体被依赖的语义拓扑 |
| `multi_filter_relations` | 多维过滤查询生效关系 | Agent 按多维度条件组合搜索关系 |
| `get_relation_topology` | 查询实体为中心的关系拓扑 | Agent 获取实体的一度邻接关系全貌 |
| `list_relation_versions` | 查询关系的历史版本列表 | Agent 追溯关系定义的演变历史 |
| `compare_relation_versions` | 对比两个版本的差异 | Agent 分析关系定义变更的影响范围 |

---

## 1. get_relation_by_fqn

**工具名**: `get_relation_by_fqn`

**描述**: 通过关系 FQN 精准查询一条生效关系实例的完整属性，返回包含 name、description、content、关联元模型、端点实体信息与审计时间戳的结构化上下文。

**输入 Schema**:
```json
{
  "type": "object",
  "properties": {
    "fqn": {
      "type": "string",
      "description": "关系实例的全限定名 (FQN)，格式为 {源实体FQN}#{关系类型FQN}#{目标实体FQN}"
    }
  },
  "required": ["fqn"]
}
```

**输出 Schema**: `RelationInstanceDto` 的完整 JSON Schema 表示

**输出示例**:
```json
{
  "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003",
  "name": "订单项组成关系",
  "description": "订单与订单项的组成关联",
  "sourceEntityFqn": "Order_001",
  "targetEntityFqn": "OrderItem_003",
  "relationType": "COMPOSITION",
  "relationSchemaFqn": "order:1.0.0.COMPOSITION",
  "content": { "quantity": 2 },
  "currentVersion": 1,
  "createdTime": "2026-08-01 10:00:00",
  "updatedTime": "2026-08-01 10:01:00"
}
```

---

## 2. list_outbound_relations

**工具名**: `list_outbound_relations`

**描述**: 查询指定实体的所有出边关系（以该实体为源端的关联关系），支持按关系类型和/或目标实体类型过滤。用于建立实体与其影响范围的拓扑认知。

**输入 Schema**:
```json
{
  "type": "object",
  "properties": {
    "entityFqn": {
      "type": "string",
      "description": "源实体 FQN"
    },
    "relationType": {
      "type": "string",
      "description": "关系类型过滤（可选），如 COMPOSITION、DEPENDENCY_INFLUENCE"
    },
    "targetEntityType": {
      "type": "string",
      "description": "目标实体类型过滤（可选），如 OrderItem"
    }
  },
  "required": ["entityFqn"]
}
```

**输出 Schema**: `RelationInstanceDto[]` 列表

---

## 3. list_inbound_relations

**工具名**: `list_inbound_relations`

**描述**: 查询指定实体的所有入边关系（以该实体为目标端的关联关系），支持按关系类型和/或源实体类型过滤。用于建立实体的被依赖认知。

**输入 Schema**:
```json
{
  "type": "object",
  "properties": {
    "entityFqn": {
      "type": "string",
      "description": "目标实体 FQN"
    },
    "relationType": {
      "type": "string",
      "description": "关系类型过滤（可选）"
    },
    "sourceEntityType": {
      "type": "string",
      "description": "源实体类型过滤（可选）"
    }
  },
  "required": ["entityFqn"]
}
```

**输出 Schema**: `RelationInstanceDto[]` 列表

---

## 4. multi_filter_relations

**工具名**: `multi_filter_relations`

**描述**: 按多维度条件组合过滤查询生效关系实例。维度间为 AND 逻辑，维度内为 OR 逻辑。支持关系类型、端点类型、端点 FQN、RelationSchema FQN、名称/描述关键词、时间范围等维度。适用于 Agent 自主构建精确或宽泛的语义搜索。

**输入 Schema**:
```json
{
  "type": "object",
  "properties": {
    "relationTypes": {
      "type": "array",
      "items": { "type": "string" },
      "description": "关系类型集合（多值 OR），如 [\"COMPOSITION\", \"DEPENDENCY_INFLUENCE\"]"
    },
    "sourceEntityTypes": {
      "type": "array",
      "items": { "type": "string" },
      "description": "源实体类型集合（多值 OR）"
    },
    "targetEntityTypes": {
      "type": "array",
      "items": { "type": "string" },
      "description": "目标实体类型集合（多值 OR）"
    },
    "sourceEntityFqns": {
      "type": "array",
      "items": { "type": "string" },
      "description": "源实体 FQN 集合（多值 OR + 前缀匹配）"
    },
    "targetEntityFqns": {
      "type": "array",
      "items": { "type": "string" },
      "description": "目标实体 FQN 集合（多值 OR + 前缀匹配）"
    },
    "relationSchemaFqns": {
      "type": "array",
      "items": { "type": "string" },
      "description": "RelationSchema FQN 集合（多值 OR）"
    },
    "nameKeyword": {
      "type": "string",
      "description": "名称关键词（子串包含匹配）"
    },
    "descriptionKeyword": {
      "type": "string",
      "description": "描述关键词（子串包含匹配）"
    },
    "page": {
      "type": "integer",
      "default": 1,
      "description": "页码"
    },
    "size": {
      "type": "integer",
      "default": 20,
      "description": "每页条数"
    }
  }
}
```

**输出 Schema**: 分页结果 `{ content: RelationInstanceDto[], total: number, page: number, size: number }`

---

## 5. get_relation_topology

**工具名**: `get_relation_topology`

**描述**: 查询以指定实体为中心的一度关系拓扑全貌，同时返回实体的出边与入边关系列表。适用于 Agent 建立实体在语义网络中的局部语境认知。

**输入 Schema**:
```json
{
  "type": "object",
  "properties": {
    "entityFqn": {
      "type": "string",
      "description": "中心实体 FQN"
    }
  },
  "required": ["entityFqn"]
}
```

**输出 Schema**:
```json
{
  "type": "object",
  "properties": {
    "entityFqn": { "type": "string" },
    "outboundCount": { "type": "integer", "description": "出边关系总数" },
    "inboundCount": { "type": "integer", "description": "入边关系总数" },
    "outboundRelations": {
      "type": "array",
      "items": { "$ref": "RelationInstanceDto" }
    },
    "inboundRelations": {
      "type": "array",
      "items": { "$ref": "RelationInstanceDto" }
    }
  }
}
```

---

## 6. list_relation_versions

**工具名**: `list_relation_versions`

**描述**: 查询指定关系 FQN 的所有历史正式版本列表（倒序），每条包含版本号、生效时间与操作人，不返回完整属性内容。适用于 Agent 追溯关系定义的版本演变历史。

**输入 Schema**:
```json
{
  "type": "object",
  "properties": {
    "fqn": {
      "type": "string",
      "description": "关系 FQN"
    }
  },
  "required": ["fqn"]
}
```

**输出 Schema**: 版本列表 `[{ version: number, activatedBy: string, activatedTime: string }]`

---

## 7. compare_relation_versions

**工具名**: `compare_relation_versions`

**描述**: 对比同一关系 FQN 下任意两个历史版本的字段级差异，按新增（ADDED）、修改（MODIFIED）、删除（DELETED）三类分类展示变更内容。适用于 Agent 理解关系定义从 vA 到 vB 的具体变化。

**输入 Schema**:
```json
{
  "type": "object",
  "properties": {
    "fqn": {
      "type": "string",
      "description": "关系 FQN"
    },
    "versionA": {
      "type": "integer",
      "description": "较旧版本号"
    },
    "versionB": {
      "type": "integer",
      "description": "较新版本号"
    }
  },
  "required": ["fqn", "versionA", "versionB"]
}
```

**输出 Schema**:
```json
{
  "type": "object",
  "properties": {
    "fqn": { "type": "string" },
    "versionA": { "type": "integer" },
    "versionB": { "type": "integer" },
    "addedFields": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "fieldPath": { "type": "string", "description": "字段路径（JSON Pointer）" },
          "newValue": { "description": "新增的字段值" }
        }
      }
    },
    "modifiedFields": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "fieldPath": { "type": "string" },
          "oldValue": { "description": "修改前的值" },
          "newValue": { "description": "修改后的值" }
        }
      }
    },
    "deletedFields": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "fieldPath": { "type": "string" },
          "oldValue": { "description": "被删除的原始值" }
        }
      }
    }
  }
}
```

---

## 消费方式

Agent 通过 `agent-consumption` BC 统一发布的 MCP Server 调用以上工具：

```
Agent → opencode CLI → MCP Client → agent-consumption (MCP Server)
    → semantic-query-engine (聚合查询) → metaforge-graph (关系拓扑查询)
```

MCP 工具调用完成后，结果以结构化 JSON 格式直接注入 Agent 推理上下文，无需二次解析。

## 权限说明

MCP 工具的所有查询结果自动经过 `agent-consumption` BC 的全链路白名单权限过滤，仅返回 Agent 已导入授权范围内的语义数据。

## 性能约束

| 工具 | 目标响应时间 |
|------|------------|
| `get_relation_by_fqn` | ≤ 20ms |
| `list_outbound_relations` / `list_inbound_relations` | ≤ 50ms（百级结果集） |
| `multi_filter_relations` | ≤ 100ms（3 维度组合，百级结果集） |
| `get_relation_topology` | ≤ 100ms |
| `list_relation_versions` | ≤ 100ms |
| `compare_relation_versions` | ≤ 50ms |
