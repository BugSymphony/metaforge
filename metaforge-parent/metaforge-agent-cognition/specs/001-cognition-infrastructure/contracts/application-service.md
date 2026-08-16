---
id: agent-cognition.application-service
protocol: Java Interface
version: 1.0.0
owner: metaforge-agent-cognition
description: 认知查询 Application Service 接口契约。下游 BC 通过 Maven 依赖 metaforge-agent-cognition-api 模块后进行进程内调用。提供统一模板驱动的认知查询执行能力。
type: business
---

# Application Service Contract: metaforge-agent-cognition

**Protocol**: Application Service（进程内 Java Interface 调用）
**Module**: `metaforge-agent-cognition-api`
**Version**: 1.0.0

> 下游 BC 通过 Maven 依赖 `metaforge-agent-cognition-api` 模块，注入以下接口的 Spring Bean 实例进行进程内调用。严禁依赖 `metaforge-agent-cognition-core` 模块。

---

## Maven 依赖

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition-api</artifactId>
    <version>${revision}</version>
</dependency>
```

---

## 1. CognitionQueryService

**职责**: 统一模板驱动的认知查询执行入口。按 templateId 路由模板配置，编排认知算子执行，返回结构化认知结果。REST、MCP、Application Service 三层共享同一入口语义。

### @OpenHostService

```java
/**
 * 统一认知查询服务。
 * <p>
 * 按模板 ID 路由到对应的模板配置（TemplateDefinition），解析算子清单（operators），
 * 按 agent_archetype 白名单过滤算子，按 priority 排序调度执行，按 cognition_depth 裁剪结果，
 * 最终按 output format（json/prompt）经 OutputFormatter SPI 组装输出。
 * <p>
 * 本服务为认知引擎的唯一进程内调用入口，与 REST 端点 {@code POST /api/v1/cognition/{templateId}}
 * 和 MCP Tool {@code cognition_execute} 共享完全等价的路由语义。
 * <p>
 * Scope 从请求体传入，贯穿全管线：入参校验 → 算子查询上下文注入 → 算子内部裁剪 →
 * 输出中 context_meta.scope_applied 标注。DELEGATE 模板产出 delegated_scope 供子 Agent 复用。
 *
 * @see TemplateDefinition
 * @see CognitionOperator
 * @see OutputFormatter
 */
public interface CognitionQueryService {

    /**
     * 执行一次模板驱动的认知查询。
     * <p>
     * 执行流程：
     * <ol>
     *   <li>从 {@code TemplateRegistry} 解析 templateId 获取 {@link TemplateDefinition}</li>
     *   <li>解析模板 {@code operators} 清单，通过 {@code OperatorRegistry} 加载算子实例</li>
     *   <li>按请求 {@code agent_archetype} 过滤算子（模板算子条目 archetypes 白名单）</li>
     *   <li>校验 scope 合法性（scopeRequired 时必填；bundle FQN 有效性校验）</li>
     *   <li>按 priority 降序排序算子，依次执行 {@link CognitionOperator#execute(CognitionQueryContext)}</li>
     *   <li>按 {@code cognition_depth} 深度裁剪（required=true 豁免，required=false 按比例+最小保留数）</li>
     *   <li>若 max_tokens 超限，自动降级深度</li>
     *   <li>生成 {@link ContextMeta}（version_anchors, scope_applied, token_estimate, etc.）</li>
     *   <li>按 format 经 OutputFormatter SPI 组装输出</li>
     * </ol>
     * <p>
     * 错误场景：
     * <ul>
     *   <li>templateId 不存在 → {@code TEMPLATE_NOT_FOUND} (34001)</li>
     *   <li>scopeRequired=true 且 scope 为空 → {@code MISSING_SCOPE} (34005)</li>
     *   <li>scope 中 bundle FQN 无效 → {@code INVALID_SCOPE} (34003)</li>
     *   <li>entity 越界 scope → {@code ENTITY_OUT_OF_SCOPE} (34004)</li>
     *   <li>archetype 在模板中无算子配置 → {@code ARCHETYPE_NOT_SUPPORTED} (34012)</li>
     *   <li>required=true 算子执行失败 → {@code OPERATOR_EXECUTION_ERROR} (34009)</li>
     *   <li>required=true 算子超时 → {@code OPERATOR_TIMEOUT} (34008)</li>
     *   <li>format 无效 → {@code INVALID_FORMAT} (34010)</li>
     *   <li>上游 BC 不可用 → {@code UPSTREAM_UNAVAILABLE} (34011)</li>
     * </ul>
     *
     * @param templateId 模板唯一标识（如 "DISCOVER", "ORIENT", "BRIEF" 等）
     *                   对应 TemplateRegistry 中已注册的模板
     * @param request    认知查询请求，包含 scope、params、format、cognition_depth、
     *                   agent_archetype、max_tokens 等参数
     * @return 认知执行响应，包含 template、context_meta、dimensions 等结构化结果
     * @throws TemplateNotFoundException 如果 templateId 在注册表中不存在
     * @throws MissingScopeException 如果 scopeRequired=true 且 scope 为空
     * @throws InvalidScopeException 如果 scope 中 bundle FQN 无效
     * @throws EntityOutOfScopeException 如果查询的实体超出 scope 边界
     * @throws ArchetypeNotSupportedException 如果请求 archetype 在模板中无任何算子配置
     * @throws OperatorExecutionException 如果 required=true 的算子执行失败
     * @throws OperatorTimeoutException 如果 required=true 的算子执行超时
     * @throws InvalidFormatException 如果 format 参数值不在 OutputFormat 枚举中
     * @throws UpstreamUnavailableException 如果调用上游 BC 失败
     */
    CognitionResponse execute(String templateId, CognitionRequest request);
}
```

---

## 2. 输入/输出 DTO

### CognitionRequest

```java
/**
 * 认知查询请求 DTO。
 * <p>
 * 入参由调用方传入，均为确定性结构化参数，不接受自然语言。
 * 所有字段均有合理默认值（除 DELEGATE 模板 scope 必填外）。
 *
 * @param scope           认知边界五字段（bundles/packages/domainGroups/domains/entitySchemas），
 *                        为 null 且 scopeRequired=false 时以 Scope.EMPTY 处理（全量无过滤）
 * @param params          模板专用参数，由模板 inputSchema 定义合法键值对
 * @param format          输出格式（"json" 或 "prompt"），默认 "json"
 * @param cognitionDepth  认知深度（"L1"/"L2"/"L3"），默认 L2
 * @param agentArchetype  Agent 原型（"execution"/"exploration"/"audit"/"orchestration"），默认 "execution"
 * @param maxTokens       最大 Token 预算，默认 8000；< 500 自动降为 L1
 */
public record CognitionRequest(
    Scope scope,
    Map<String, Object> params,
    String format,
    String cognitionDepth,
    String agentArchetype,
    Integer maxTokens
) {
    /** 默认 JSON 格式 */
    public static CognitionRequest withDefaults(Scope scope, Map<String, Object> params) {
        return new CognitionRequest(scope, params, "json", "L2", "execution", 8000);
    }
}
```

### Scope

```java
/**
 * 认知边界五字段值对象。
 * <p>
 * 贯穿 API 入参 → 校验 → 算子上下文 → 输出裁剪全管线。
 * Scope.EMPTY 表示无边界约束（全量无过滤）。
 * DELEGATE 模板出参的 delegated_scope 可直接作为子 Agent scope 入参。
 *
 * @param bundles       Bundle FQN 列表（如 ["order:1.0.0", "payment:1.0.0"]）
 *                      作为 Agent 授权白名单的承载
 * @param packages      Package FQN 列表
 * @param domainGroups  域组 FQN 列表
 * @param domains       域 FQN 列表
 * @param entitySchemas EntitySchema FQN 列表
 */
public record Scope(
    List<String> bundles,
    List<String> packages,
    List<String> domainGroups,
    List<String> domains,
    List<String> entitySchemas
) {
    /** 无边界约束（全量无过滤） */
    public static final Scope EMPTY = new Scope(null, null, null, null, null);

    /** 是否为空 scope */
    public boolean isEmpty() {
        return bundles == null && packages == null && domainGroups == null
            && domains == null && entitySchemas == null;
    }
}
```

### CognitionResponse

```java
/**
 * 认知查询响应 DTO。
 * <p>
 * 当 format="json" 时返回结构化 JSON；当 format="prompt" 时返回 Markdown 文本。
 * 两种格式的核心语义信息完全等价。
 *
 * @param template     执行的模板 ID
 * @param contextMeta  上下文元信息（版本锚、scope 应用、Token 估算、裁剪标记等）
 * @param dimensions   按分类分组的认知算子执行结果（key = category 小写名，如 "ontological"）
 * @param format       输出格式指示（"json" 或 "prompt"）
 * @param content      当 format="prompt" 时的 Markdown 文本；json 格式时为 null
 */
public record CognitionResponse(
    String template,
    ContextMeta contextMeta,
    Map<String, Object> dimensions,
    String format,
    String content
) {
    /** 创建 JSON 格式响应 */
    public static CognitionResponse json(String template, ContextMeta meta, Map<String, Object> dims) {
        return new CognitionResponse(template, meta, dims, "json", null);
    }

    /** 创建 Prompt 格式响应 */
    public static CognitionResponse prompt(String template, ContextMeta meta, String content) {
        return new CognitionResponse(template, meta, null, "prompt", content);
    }
}
```

### ContextMeta

```java
/**
 * 上下文元信息。
 * <p>
 * 每份输出自包含此元信息，确保消费端无需二次查询底层 BC。
 *
 * @param template             执行的模板标识
 * @param versionAnchors       各 Bundle 版本锚（FQN → 版本 FQN 映射）
 * @param scopeApplied         实际应用（生效）的 scope
 * @param tokenEstimate        Token 估算值（近似，非精确）
 * @param generatedAt          生成时间戳
 * @param skippedEntities      被 scope 跳过（越界）的实体 FQN 列表
 * @param truncatedPerspectives 被认知深度裁剪的算子所属分类名称列表
 */
public record ContextMeta(
    String template,
    Map<String, String> versionAnchors,
    Scope scopeApplied,
    int tokenEstimate,
    Instant generatedAt,
    List<String> skippedEntities,
    List<String> truncatedPerspectives
) {}
```

---

## Error Codes

本 BC 使用错误码范围 **34000-34099**（定义于 `AgentCognitionErrorCodes` 常量类）。

| 错误码 | 常量名 | HTTP 状态 | 描述 |
|--------|--------|----------|------|
| 34001 | `TEMPLATE_NOT_FOUND` | 404 | 指定 templateId 的模板未注册 |
| 34002 | `TEMPLATE_INVALID` | 422 | 模板配置校验失败 |
| 34003 | `INVALID_SCOPE` | 400 | scope 中 bundle/package FQN 无效 |
| 34004 | `ENTITY_OUT_OF_SCOPE` | 403 | 查询实体超出 scope 边界 |
| 34005 | `MISSING_SCOPE` | 400 | scopeRequired 模板缺少 scope |
| 34006 | `UNSUPPORTED_OPERATOR` | 422 | 模板引用未注册的算子 operatorId |
| 34007 | `UNKNOWN_OPERATOR_REF` | 422 | 算子注册表无法解析操作符引用 |
| 34008 | `OPERATOR_TIMEOUT` | 504 | required=true 算子执行超时 |
| 34009 | `OPERATOR_EXECUTION_ERROR` | 500 | required=true 算子执行失败 |
| 34010 | `INVALID_FORMAT` | 400 | format 参数值无效 |
| 34011 | `UPSTREAM_UNAVAILABLE` | 502 | 上游 BC 服务不可用 |
| 34012 | `ARCHETYPE_NOT_SUPPORTED` | 400 | 请求 archetype 在模板中无任何算子配置 |
| 34013 | `INVALID_LEVEL` | 400 | 请求指定的 level 无法解析为有效 EntitySchema 类型 |
| 34014 | `INVALID_OPERATOR_SELECTION` | 400 | 请求的 selectOperators 无任何算子匹配模板声明 |

---

## 调用示例

```java
// 引入 metaforge-agent-cognition-api 模块依赖后直接注入
@Autowired
private CognitionQueryService cognitionQueryService;

// JSON 格式 DISCOVER 查询
CognitionResponse jsonResp = cognitionQueryService.execute("DISCOVER",
    new CognitionRequest(
        new Scope(List.of("order:1.0.0"), null, null, null, null),
        Map.of("parent_fqn", ""),
        "json", "L2", "execution", 8000
    ));

// Prompt 格式 BRIEF 查询（单实体）
CognitionResponse promptResp = cognitionQueryService.execute("BRIEF",
    new CognitionRequest(
        new Scope(List.of("order:1.0.0"), null, null, null, null),
        Map.of("entity_fqn", "order:1.0.0.pkg_order.Order_001"),
        "prompt", "L3", "execution", 4000
    ));

// DELEGATE 模板（scope 必填）
CognitionResponse delegateResp = cognitionQueryService.execute("DELEGATE",
    new CognitionRequest(
        new Scope(List.of("order:1.0.0"), null, null, null, null),
        Map.of("subtask_type", "EXECUTION", "target_entity", "Order_001"),
        "json", "L2", "orchestration", 6000
    ));
```

---

## Transaction Boundary

本 BC 为纯无状态计算层，不参与事务管理。每个 `execute()` 调用为一次计算操作，不涉及数据库写操作。
