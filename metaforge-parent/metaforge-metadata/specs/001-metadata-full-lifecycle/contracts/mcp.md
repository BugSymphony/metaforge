---
id: metadata-management.mcp
protocol: MCP
version: 1.0.0
owner: metadata-management
description: 元数据管理 BC 面向 Agent 消费的 MCP (Model Context Protocol) 工具集契约。通过 Spring AI MCP Server 将元数据查询与历史追溯能力发布为 MCP 工具，Agent 可直接注入结构化语义上下文。
type: business
---

# MCP Contract: metadata-management

**Protocol**: MCP (Model Context Protocol)
**MCP Server**: `MetadataMcpTools` (Spring AI `@Tool` 标注)
**Version**: 1.0.0

> Agent 通过 MCP 协议连接 MetaForge 平台后，可直接调用以下工具获取元数据语义上下文。所有返回结果均为 Agent 友好型结构化格式（JSON Schema 兼容），可直接注入 Agent 推理上下文，无需二次解析。

---

## MCP 工具清单

| 工具名 | 描述 | 调用频率 | 优先级 |
|--------|------|---------|--------|
| `getMetadataEntity` | FQN 精准查询生效元数据完整内容 | 高 | P1 |
| `queryMetadataByPrefix` | FQN 前缀范围查询（多前缀 OR 并集） | 高 | P1 |
| `queryMetadataBySchema` | 按 EntitySchema 类型查询 | 中 | P2 |
| `queryMetadataByAttribute` | 按属性条件组合查询 | 低 | P2 |
| `getEntityVersionHistory` | 查询全历史版本列表 | 低 | P2 |
| `getEntityVersionDetail` | 按版本号查询历史详情 | 低 | P2 |
| `compareVersions` | 两个历史版本差异对比 | 低 | P2 |

---

## 工具详细定义

### 1. getMetadataEntity

**描述**: 通过精准 FQN 查询一条生效元数据的完整内容，包含所有属性字段、关联元模型 FQN、当前版本号、审计信息。

**参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| `fqn` | `String` | 是 | 元数据 FQN，如 `SalesOrder_001` |

**返回**: `MetadataEntityDto` 完整结构（含 `content` 的 JSONB 展开、`entitySchemaFqn`、`currentVersion`、`createdTime`、`updatedTime`）。

**异常提示**:
- 不存在或已下线 → "元数据实体 '{fqn}' 不存在或已下线"

---

### 2. queryMetadataByPrefix

**描述**: 按 FQN 前缀集合查询生效元数据列表。多个前缀按 **OR 并集** 逻辑处理（返回匹配任意前缀的结果），结果按 FQN 升序排列，支持分页。适合获取组合层级子树（如查询 `SalesOrder_001` 下的所有子实体）。

**参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| `fqnPrefixes` | `List<String>` | 是 | FQN 前缀集合，如 `["SalesOrder_", "OrderReport_"]` |
| `page` | `int` | 否 | 页码，默认 1 |
| `size` | `int` | 否 | 每页条数，默认 20（上限 100） |

**返回**: `PageResult<MetadataEntityDto>` 分页结构。

---

### 3. queryMetadataBySchema

**描述**: 按 EntitySchema FQN（元模型类型）查询该类型下的所有生效元数据。适合 Agent 按业务类型批量获取领域概念。

**参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| `entitySchemaFqn` | `String` | 是 | EntitySchema 全限定名，如 `order:1.0.0.pkg_order.Order` |
| `page` | `int` | 否 | 页码，默认 1 |
| `size` | `int` | 否 | 每页条数，默认 20（上限 100） |

**返回**: `PageResult<MetadataEntityDto>` 分页结构。

---

### 4. queryMetadataByAttribute

**描述**: 按属性内容字段条件组合查询生效元数据。支持精准匹配（字段值完全相等）和模糊前缀匹配。适合 Agent 按业务属性值查找相关元数据。

**参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| `fields` | `List<String>` | 是 | 属性字段名列表，如 `["status", "priority"]` |
| `values` | `List<String>` | 是 | 属性值列表（与 fields 顺序对应） |
| `matchMode` | `String` | 否 | `EXACT`（精准）/ `PREFIX`（前缀），默认 `EXACT` |
| `page` | `int` | 否 | 页码，默认 1 |
| `size` | `int` | 否 | 每页条数，默认 20 |

**返回**: `PageResult<MetadataEntityDto>` 分页结构。

---

### 5. getEntityVersionHistory

**描述**: 查询指定 FQN 的所有历史版本列表。按版本号倒序排列（最新版本在前），每条含版本号、生效时间、操作人。默认不返回完整 content（减少 Token 消耗）。

**参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| `fqn` | `String` | 是 | 元数据 FQN |

**返回**: `List<EntityVersionDto>`（精简版，不含 content 字段）。

---

### 6. getEntityVersionDetail

**描述**: 查询指定 FQN + 版本号的完整历史版本快照，包含该版本生效时的完整属性内容、元模型 FQN、时间戳。

**参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| `fqn` | `String` | 是 | 元数据 FQN |
| `version` | `int` | 是 | 版本号（正整数） |

**返回**: `EntityVersionDto`（完整版，含 `content` 全量快照）。

---

### 7. compareVersions

**描述**: 对比指定 FQN 的任意两个历史版本间的字段级差异。按 **ADDED（新增）**、**MODIFIED（修改）**、**DELETED（删除）** 三类分类展示变更内容。帮助 Agent 理解版本演化轨迹。

**参数**:

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| `fqn` | `String` | 是 | 元数据 FQN |
| `versionA` | `int` | 是 | 版本 A（较旧版本） |
| `versionB` | `int` | 是 | 版本 B（较新版本） |

**返回**: `VersionDiffDto` 结构化差异结果。

**返回示例**:
```json
{
  "fqn": "SalesOrder_001",
  "versionA": 1,
  "versionB": 3,
  "addedFields": [
    { "fieldPath": "status", "oldValue": null, "newValue": "active" }
  ],
  "modifiedFields": [
    { "fieldPath": "name", "oldValue": "A", "newValue": "B" }
  ],
  "deletedFields": [
    { "fieldPath": "priority", "oldValue": "high", "newValue": null }
  ]
}
```

---

## MCP 调用约定

### 权限过滤

MCP 请求中隐含 Agent 身份信息。本 BC 的 MCP 工具**不自行实现权限过滤**——Agent 身份与白名单过滤由上游 `agent-consumption` BC 在 MCP Server 层统一处理。本 BC 的 MCP 工具输出所有符合查询条件的生效元数据。

### 数据范围

- **默认仅返回生效版本**（主表 `metadata_entity` 数据）
- **草稿不可见**（对外查询不暴露草稿表数据）
- **已下线数据返回明确提示**："元数据实体 '{fqn}' 不存在或已下线"

### 返回格式

所有返回数据均为 Agent 友好型结构化 JSON，可直接注入 Agent 上下文：

- 使用 Jackson 默认序列化（遵循 foundation-core Jackson 全局配置）
- 日期格式: `yyyy-MM-dd HH:mm:ss`
- 时区: `Asia/Shanghai`
- Null 值策略: `NON_NULL`（减少 Token 消耗）

### 错误处理

- 业务异常（如 FQN 不存在）返回包含明确错误提示的结构化响应
- 系统异常通过全局 `ExceptionHandlerSpi` 转换为 `ApiResponse<?>` 格式
- 所有响应包含 `traceId` 用于问题追踪

---

## MCP Server 配置参考

本 BC 的 MCP 工具通过 Spring AI 在 `metaforge-boot` 启动时自动注册。BC 内部仅需实现 `@Tool` 注解的 Java 方法。

```java
@Component
public class MetadataMcpTools {

    private final MetadataQueryService metadataQueryService;
    private final MetadataHistoryService metadataHistoryService;

    public MetadataMcpTools(MetadataQueryService metadataQueryService,
                            MetadataHistoryService metadataHistoryService) {
        this.metadataQueryService = metadataQueryService;
        this.metadataHistoryService = metadataHistoryService;
    }

    @Tool(description = "通过 FQN 精准查询一条生效元数据的完整内容")
    public MetadataEntityDto getMetadataEntity(
            @ToolParam(description = "元数据 FQN，如 'SalesOrder_001'") String fqn) {
        return metadataQueryService.getByFqn(fqn);
    }

    @Tool(description = "按 FQN 前缀集合查询生效元数据列表（多前缀 OR 并集，按 FQN 排序，支持分页）")
    public PageResult<MetadataEntityDto> queryMetadataByPrefix(
            @ToolParam(description = "FQN 前缀集合") List<String> fqnPrefixes,
            @ToolParam(description = "页码，默认 1") int page,
            @ToolParam(description = "每页条数，默认 20") int size) {
        return metadataQueryService.listByFqnPrefixes(
            fqnPrefixes, PageRequest.of(page, size));
    }

    // ... 其他工具方法
}
```
