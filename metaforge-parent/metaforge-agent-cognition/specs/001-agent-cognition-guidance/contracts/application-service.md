# Application Service Contract

## Maven Dependency

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition-api</artifactId>
    <version>${revision}</version>
</dependency>
```

## Core Interface

### `CognitionQueryService`

```java
/**
 * 统一认知查询执行服务。
 * <p>
 * 通过模板ID路由到对应的认知模板配置，按模板声明的视角组合、深度、原型、
 * 输出格式，编排视角执行器完成多BC查询与结果聚合。
 * 所有查询无状态、幂等——不持有任何任务上下文或会话状态。
 * 新增业务场景仅需在 cognition-templates.yml 中声明模板配置，
 * 无需新增接口方法或发布代码。
 */
@OpenHostService
public interface CognitionQueryService {

    /**
     * 执行认知查询。
     * <p>
     * 通过 templateId 路由到对应的 YAML 模板配置（视角组合、深度级别、
     * 代理原型适配、输出格式、Token预算），编排视角执行器完成多BC查询，
     * 按深度和Token预算裁剪后组装为自包含的 GuidanceResult 输出。
     * <p>
     * 输出附带 context_meta.data_version_anchors，含各依赖 Bundle 的
     * 当前已发布版本号与查询时间戳，供消费端追溯数据版本。
     *
     * @param templateId 模板标识（如 "task-brief"、"step-guide"、"bundle-catalog"）
     * @param request 认知查询请求（含 bundle_fqns、entity_fqn、参数覆盖等）
     * @return 自包含的统一认知查询结果，含 context_meta + 各认知视角章节
     * @throws TemplateNotFoundException 如果 templateId 未在模板配置中注册
     * @throws InvalidBundleFqnException 如果 bundle_fqns 格式非法
     * @throws EmptyBundleFqnsException 如果 bundle_fqns 为空
     * @throws InvalidEntityFqnException 如果 entity_fqn 前缀不属于任何已发布 Bundle
     */
    GuidanceResult execute(String templateId, CognitionRequest request);
}
```

## Supporting Interfaces

### `TemplateRegistryService`

```java
/**
 * 认知模板注册与校验服务。
 * <p>
 * 从 cognition-templates.yml 加载模板配置，提供模板存在性校验、
 * 模板配置解析、以及模板元数据查询能力。
 * 模板配置为声明式，新增场景无需修改代码。
 */
@OpenHostService
public interface TemplateRegistryService {

    boolean isRegistered(String templateId);

    TemplateConfig resolve(String templateId) throws TemplateNotFoundException;

    Set<String> listTemplateIds();
}
```

### `CognitionOutputService`

```java
/**
 * 认知输出格式化服务。
 * <p>
 * 支持 JSON 与 Prompt (Markdown) 双格式输出。
 * JSON 格式面向程序化消费，Prompt 格式面向 LLM 直接注入。
 */
@OpenHostService
public interface CognitionOutputService {

    String formatAsJson(GuidanceResult result);

    String formatAsPrompt(GuidanceResult result);
}
```

## Data Transfer Objects

### `CognitionRequest`

```java
/**
 * 认知查询请求。
 */
public class CognitionRequest {

    /** Bundle FQN 列表，如 ["order:1.0.0", "refund:1.0.0"] */
    private List<String> bundleFqns;

    /** 实体 FQN，如 "order:1.0.0.Step_CheckInventory" */
    private String entityFqn;

    /** 实体类型列表，如 ["order:1.0.0.ExecutionRule"] */
    private List<String> entityTypes;

    /** 主体域 FQN，如 "order:1.0.0.SubjectDomain_Order" */
    private String subjectDomainFqn;

    /** 作用域模式：ENTITY_LEVEL / PACKAGE / BUNDLE / INHERITED */
    private String scopeMode;

    /** 认知深度：L1 / L2 / L3 */
    private String cognitionDepth;

    /** 代理原型：execution / exploration / audit / orchestration */
    private String agentArchetype;

    /** 最大 Token 数 */
    private Integer maxTokens;

    /** 展开模式：eager / lazy */
    private String expand;

    /** 输出格式：json / prompt */
    private String format;

    /** 分页游标 */
    private Integer cursor;

    /** 分页大小 */
    private Integer pageSize;

    /** 上下文参数，键值对透传 */
    private Map<String, String> contextParameters;
}
```

### `GuidanceResult`

```java
/**
 * 自包含的统一认知查询结果。
 */
public class GuidanceResult {

    /** 所使用的模板 ID */
    private String templateId;

    /** 各认知视角的输出结果，key 为 perspectiveId */
    private Map<String, PerspectiveResult> perspectives;

    /** 上下文元信息 */
    private ContextMeta contextMeta;
}
```

### `PerspectiveResult`

```java
/**
 * 单个认知视角的执行结果。
 */
public class PerspectiveResult {

    /** 视角标识 */
    private String perspectiveId;

    /** 执行状态 */
    private String status;

    /** 视角返回的认知数据 */
    private Object data;

    /** 是否因 Token 预算被截断 */
    private boolean truncated;

    /** 截断原因 */
    private String truncatedReason;

    /** 错误信息（status 非成功时） */
    private String errorMessage;
}
```

### `ContextMeta`

```java
/**
 * 上下文元信息。
 */
public class ContextMeta {

    /** 模板 ID */
    private String templateId;

    /** 上下文模式 */
    private String contextMode;

    /** 数据版本锚点，key 为 Bundle 名称 */
    private Map<String, DataVersionAnchor> dataVersionAnchors;

    /** 被截断的视角列表 */
    private List<TruncatedPerspective> truncatedPerspectives;

    /** 被跳过的视角列表 */
    private List<SkippedPerspective> skippedPerspectives;
}
```

### Supporting Records

```java
public record DataVersionAnchor(String version, Instant queriedAt) {}

public record TruncatedPerspective(String perspectiveId, boolean truncated, String reason) {}

public record SkippedPerspective(String perspectiveId, String reason) {}
```

## Error Codes (34000–34099)

| Code  | Name                    | Description                            |
|-------|-------------------------|----------------------------------------|
| 34001 | TEMPLATE_NOT_FOUND      | templateId not registered              |
| 34002 | INVALID_BUNDLE_FQN      | bundleFqn format invalid               |
| 34003 | EMPTY_BUNDLE_FQNS       | bundleFqns list empty                  |
| 34004 | INVALID_ENTITY_FQN      | entityFqn prefix not in any Bundle     |
| 34005 | PERSPECTIVE_TIMEOUT     | Single perspective timed out (200ms)   |
| 34006 | UPSTREAM_UNAVAILABLE    | Upstream BC unavailable                |

## Exception Classes

```java
public class TemplateNotFoundException extends RuntimeException {
    public TemplateNotFoundException(String templateId) { ... }
}

public class InvalidBundleFqnException extends RuntimeException {
    public InvalidBundleFqnException(String fqn) { ... }
}

public class EmptyBundleFqnsException extends RuntimeException {
    public EmptyBundleFqnsException() { ... }
}

public class InvalidEntityFqnException extends RuntimeException {
    public InvalidEntityFqnException(String fqn) { ... }
}
```
