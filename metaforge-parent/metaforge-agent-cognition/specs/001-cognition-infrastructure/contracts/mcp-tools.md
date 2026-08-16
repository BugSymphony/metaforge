---
id: agent-cognition.mcp-tools
protocol: MCP
version: 1.0.0
owner: metaforge-agent-cognition
description: 认知引擎 MCP Tool 契约。通过 Spring AI MCP Server 发布认知查询工具，供 Agent 生态原生接入。
type: business
---

# MCP Tools Contract: metaforge-agent-cognition

**Protocol**: MCP (Model Context Protocol)，通过 Spring AI MCP Server 发布
**Server**: `cognition-mcp-server`
**Version**: 1.0.0

> MCP Tool 与 REST 端点 `POST /api/v1/cognition/{templateId}` 和 Application Service `CognitionQueryService.execute()` 共享完全等价的模板路由语义。MCP Tool 通过 Spring AI 框架自动发布，无需手动实现协议层。

---

## Tool: `cognition_execute`

**Description**: 执行一次模板驱动的认知查询，按模板 ID 编排认知算子、裁剪结果、返回结构化认知输出。结果可直接注入 Agent 上下文。

### Input Schema

```json
{
  "type": "object",
  "properties": {
    "templateId": {
      "type": "string",
      "description": "模板唯一标识。可选值: DISCOVER, ORIENT, BRIEF, GUIDE, FORECAST, DELEGATE",
      "enum": ["DISCOVER", "ORIENT", "BRIEF", "GUIDE", "FORECAST", "DELEGATE"]
    },
    "scope": {
      "type": "object",
      "description": "认知边界五字段。DELEGATE 模板必填。scope 中的 bundles 白名单即为 Agent 授权依据",
      "properties": {
        "bundles": {
          "type": "array",
          "items": { "type": "string" },
          "description": "Bundle FQN 白名单列表（如 [\"order:1.0.0\"]）"
        },
        "packages": {
          "type": "array",
          "items": { "type": "string" },
          "description": "Package FQN 白名单列表"
        },
        "domain_groups": {
          "type": "array",
          "items": { "type": "string" },
          "description": "域组 FQN 白名单列表"
        },
        "domains": {
          "type": "array",
          "items": { "type": "string" },
          "description": "域 FQN 白名单列表"
        },
        "entity_schemas": {
          "type": "array",
          "items": { "type": "string" },
          "description": "EntitySchema FQN 白名单列表"
        }
      }
    },
    "params": {
      "type": "object",
      "description": "模板专用参数，键值对由模板 inputSchema 定义。常用键: parent_fqn, entity_fqn, task_type, target_entity, change_entity_fqn",
      "additionalProperties": true
    },
    "format": {
      "type": "string",
      "description": "输出格式。json=结构化JSON（适合程序处理），prompt=Markdown文本（可直接注入LLM上下文）",
      "enum": ["json", "prompt"],
      "default": "json"
    },
    "cognition_depth": {
      "type": "string",
      "description": "认知深度。L1=概览（最少信息量），L2=标准，L3=全量（最丰富信息量）",
      "enum": ["L1", "L2", "L3"],
      "default": "L2"
    },
    "agent_archetype": {
      "type": "string",
      "description": "Agent 原型。决定可用算子集合的白名单过滤",
      "enum": ["execution", "exploration", "audit", "orchestration"],
      "default": "execution"
    },
    "max_tokens": {
      "type": "integer",
      "description": "最大 Token 预算（近似）。<500 自动降为 L1 深度",
      "minimum": 1,
      "maximum": 32000,
      "default": 8000
    }
  },
  "required": ["templateId"]
}
```

### Output Schema

**json format**:

```json
{
  "type": "object",
  "properties": {
    "template": {
      "type": "string",
      "description": "执行的模板 ID"
    },
    "context_meta": {
      "type": "object",
      "description": "上下文元信息——自包含、无需二次查询底层 BC",
      "properties": {
        "template": { "type": "string" },
        "version_anchors": {
          "type": "object",
          "description": "各 Bundle 版本锚（FQN → 版本 FQN）",
          "additionalProperties": { "type": "string" }
        },
        "scope_applied": { "$ref": "#/definitions/Scope" },
        "token_estimate": { "type": "integer", "description": "Token 估算值" },
        "generated_at": { "type": "string", "format": "date-time" },
        "skipped_entities": {
          "type": "array",
          "items": { "type": "string" },
          "description": "被 scope 跳过的实体 FQN 列表（越界标注）"
        },
        "truncated_perspectives": {
          "type": "array",
          "items": { "type": "string" },
          "description": "被深度裁剪的算子所属分类名称列表"
        }
      }
    },
    "dimensions": {
      "type": "object",
      "description": "按认知分类分组的算子输出（key=分类名小写，如 ontological/structural/relational）",
      "additionalProperties": true
    },
    "format": { "type": "string", "enum": ["json"] },
    "content": { "type": "null" }
  }
}
```

**prompt format**:

```json
{
  "type": "object",
  "properties": {
    "template": { "type": "string" },
    "context_meta": { "type": "object" },
    "format": { "type": "string", "enum": ["prompt"] },
    "content": {
      "type": "string",
      "description": "Markdown 格式的认知简报，可直接注入 LLM 上下文"
    }
  }
}
```

### Annotations

```java
@Tool(name = "cognition_execute", description = """
    执行认知查询：按模板ID编排认知算子，产出结构化认知简报。
    结果可直接注入LLM上下文——低理解成本、自包含、带完整来源标注。
    模板ID可选: DISCOVER(元模型发现), ORIENT(业务域定位), BRIEF(实体全景),
    GUIDE(执行指南), FORECAST(影响链路), DELEGATE(子任务委派).
    """)
public CognitionResponse executeCognition(
    @ToolParam(description = "模板ID (DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE)") String templateId,
    @ToolParam(description = "认知边界五字段。scope中bundles白名单即授权依据") Scope scope,
    @ToolParam(description = "模板专用参数，见各模板inputSchema") Map<String, Object> params,
    @ToolParam(description = "输出格式 (json/prompt)，默认json") String format,
    @ToolParam(description = "认知深度 (L1概览/L2标准/L3全量)，默认L2") String cognitionDepth,
    @ToolParam(description = "Agent原型 (execution/exploration/audit/orchestration)") String agentArchetype,
    @ToolParam(description = "最大Token预算，默认8000；<500自动降L1") Integer maxTokens
) {
    return cognitionQueryService.execute(templateId, CognitionRequest.fromMcpParams(
        scope, params, format, cognitionDepth, agentArchetype, maxTokens));
}
```

---

## 内置模板速查（供 Agent 消费）

| templateId | Template Name | 典型场景 | 关键 parameters | scope 要求 |
|------------|---------------|---------|-----------------|-----------|
| DISCOVER | 元模型发现 | 探索 Bundle 下的 EntitySchema/RelationSchema 结构 | `parent_fqn` | 可选 |
| ORIENT | 业务域定位 | 快速定位实体所属的业务域/包上下文 | `entity_fqn` | 可选 |
| BRIEF | 任务/实体全景 | 获取单个实体的全维度 360° 认知简报 | `entity_fqn` | 可选 |
| GUIDE | 单步执行指南 | 获取执行下一步操作的完整上下文与关联信息 | `entity_fqn`, `task_type` | 可选 |
| FORECAST | 变更影响链路 | 评估实体变更的上下游影响传播路径 | `change_entity_fqn` | 可选 |
| DELEGATE | 子任务上下文委派 | 生成子 Agent 专用归约 scope 与执行上下文 | `subtask_type`, `target_entity` | **必填** |

---

## MCP Server 注册

```java
@Configuration
public class CognitionMcpServerConfig {

    @Bean
    public ToolCallbackProvider cognitionTools(CognitionQueryService service) {
        return ToolCallbackProvider.builder()
            .toolBeans(new CognitionMcpTools(service))
            .build();
    }
}

@Component
public class CognitionMcpTools {

    private final CognitionQueryService service;

    public CognitionMcpTools(CognitionQueryService service) {
        this.service = service;
    }

    @Tool(name = "cognition_execute", description = "...")
    public CognitionResponse executeCognition(
        @ToolParam(description = "模板ID") String templateId,
        @ToolParam(description = "认知边界") Scope scope,
        @ToolParam(description = "模板参数") Map<String, Object> params,
        @ToolParam(description = "输出格式") String format,
        @ToolParam(description = "认知深度") String cognitionDepth,
        @ToolParam(description = "Agent原型") String agentArchetype,
        @ToolParam(description = "Token预算") Integer maxTokens
    ) {
        return service.execute(templateId, new CognitionRequest(
            scope, params, format, cognitionDepth, agentArchetype, maxTokens));
    }
}
```

---

## 语义等价保证

- REST `POST /api/v1/cognition/{templateId}` 与 MCP `cognition_execute` Tool 共享相同模板路由语义
- 入参 schema 完全等价（仅传输协议不同）
- 输出 schema 完全一致（`CognitionResponse` 结构统一）
- 同一 templateId + 相同入参在两种通道下产出语义等价结果
