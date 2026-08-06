# Application Service 契约

**Feature**: [spec.md](../spec.md) | **BC**: metamodel-governance

本文件定义 `metaforge-metamodel-api` 模块对外暴露的 Application Service 接口契约。以下游 BC（metadata-management、semantic-query-engine、semantic-relation-network、agent-consumption）通过 Maven 依赖引入 api 模块后可直接调用。

---

## 1. BundleManagementService

### 接口定义

```java
public interface BundleManagementService {
    BundleDto createBundle(CreateBundleRequest request);
    BundleDto getBundle(String fqn);
    List<BundleDto> listBundles(PageRequest pageRequest);
    BundleDto updateBundle(String fqn, UpdateBundleRequest request);
}
```

### 方法说明

| 方法 | 描述 | 前置条件 | 后置条件 |
|------|------|---------|---------|
| `createBundle` | 创建新 Bundle | bundle-code 符合正则，全局唯一 | 返回 BundleDto，FQN = bundle-code |
| `getBundle` | 按 FQN 查询 Bundle | FQN 存在 | 返回 BundleDto |
| `listBundles` | 分页查询 Bundle 列表 | — | 返回 PageResult\<BundleDto\> |
| `updateBundle` | 更新 Bundle 元信息 | Bundle 存在 | 更新 name / description / owner |

### 输入/输出 DTO

```java
public record CreateBundleRequest(
    @NotBlank @Pattern(regexp = "[a-z][a-z0-9_-]{2,63}") String fqn,
    @NotBlank String name,
    @NotBlank String description,
    @NotBlank String owner
) {}

public record UpdateBundleRequest(
    @NotBlank String name,
    @NotBlank String description,
    @NotBlank String owner
) {}

public record BundleDto(
    String fqn,
    String name,
    String description,
    String owner,
    boolean isSystem,
    Map<String, Object> embedding,  // JSONB - MVP阶段仅占位
    LocalDateTime createdTime,
    LocalDateTime updatedTime
) {}
```

---

## 2. BundleVersionManagementService

### 接口定义

```java
public interface BundleVersionManagementService {
    BundleVersionDto createDraft(String bundleFqn, UpgradeLevel upgradeLevel);
    BundleVersionDto getVersion(String versionFqn);
    BundleVersionDto publish(String versionFqn);
    List<BundleVersionDto> listVersions(String bundleFqn);
}
```

### 方法说明

| 方法 | 描述 | 前置条件 | 后置条件 |
|------|------|---------|---------|
| `createDraft` | 从最新已发布版本创建草稿 | Bundle 存在且有已发布版本；同一 Bundle 无其他草稿 | 全量复制源版本，新版本号计算 |
| `getVersion` | 按 FQN 查询 BundleVersion | 存在 | 返回 BundleVersionDto |
| `publish` | 发布草稿版本 | 状态为 DRAFT；全量校验通过 | 状态 → PUBLISHED，json_schema 生成 |
| `listVersions` | 查询 Bundle 的所有版本 | Bundle 存在 | 按版本号降序排列 |

### 输入/输出 DTO

```java
public record BundleVersionDto(
    String fqn,
    String bundleFqn,
    String status,
    String sourceVersionFqn,
    UpgradeLevel upgradeLevel,
    LocalDateTime createdTime
) {}
```

---

## 3. ElementDefinitionService

### 接口定义

```java
public interface ElementDefinitionService {
    // EntitySchema
    EntitySchemaDto createEntitySchema(CreateEntitySchemaRequest request);
    EntitySchemaDto getEntitySchema(String fqn);
    EntitySchemaDto updateEntitySchema(String fqn, UpdateEntitySchemaRequest request);
    void deleteEntitySchema(String fqn);
    PageResult<EntitySchemaDto> listEntitySchemas(ElementQueryRequest query);

    // RelationSchema
    RelationSchemaDto createRelationSchema(CreateRelationSchemaRequest request);
    RelationSchemaDto getRelationSchema(String fqn);
    RelationSchemaDto updateRelationSchema(String fqn, UpdateRelationSchemaRequest request);
    void deleteRelationSchema(String fqn);
    PageResult<RelationSchemaDto> listRelationSchemas(ElementQueryRequest query);

    // AttributeTemplate
    AttributeTemplateDto createAttributeTemplate(CreateAttributeTemplateRequest request);
    AttributeTemplateDto getAttributeTemplate(String fqn);
    AttributeTemplateDto updateAttributeTemplate(String fqn, UpdateAttributeTemplateRequest request);
    void deleteAttributeTemplate(String fqn);
}

public class ElementQueryRequest {
    List<String> fqnPrefixes;   // FQN 前缀集合（多值 OR 逻辑），如 ["order:1.0.0.pkg_order."]
    PageRequest pageRequest;    // 分页参数
}
```

### 方法说明

| 方法 | 描述 | 前置条件 | 后置条件 |
|------|------|---------|---------|
| `createEntitySchema` | 创建 EntitySchema | Package 存在，FQN 全局唯一，name 必填，description 必填 | 返回 EntitySchemaDto |
| `getEntitySchema` | 按 FQN 查询 | FQN 存在 | 返回 EntitySchemaDto |
| `updateEntitySchema` | 更新 EntitySchema 元信息 | 所属版本为 DRAFT | 更新各字段 |
| `deleteEntitySchema` | 删除 EntitySchema | 所属版本为 DRAFT；无外部引用（MV 可忽略） | 删除记录 |
| `listEntitySchemas` | 按 FQN 前缀集合过滤查询 | — | 返回分页列表，支持多前缀 OR 匹配 |
| `createRelationSchema` | 创建 RelationSchema | Package 存在，两端 FQN 可达，association_type 合法 | 返回 RelationSchemaDto |
| `deleteRelationSchema` | 删除 RelationSchema | 所属版本为 DRAFT | 删除记录 |
| `listRelationSchemas` | 按 FQN 前缀集合过滤查询 | — | 返回分页列表 |
| `createAttributeTemplate` | 创建 AttributeTemplate | BundleVersion 存在（DRAFT），FQN 不含 Package 路径 | 返回 AttributeTemplateDto |
| `updateAttributeTemplate` | 更新 AttributeTemplate | 所属版本为 DRAFT | 更新属性定义 |

### 核心 DTO

```java
public record CreateEntitySchemaRequest(
    @NotBlank String packageFqn,
    @NotBlank String segment,       // FQN 最后一段（如 "Order"）
    @NotBlank String name,
    @NotBlank String description,
    List<NativeAttributeDto> nativeAttributes,
    List<String> mountedTemplateFqns
) {}

public record EntitySchemaDto(
    String fqn,
    String packageFqn,
    String name,
    String description,
    List<NativeAttributeDto> nativeAttributes,
    List<String> mountedTemplateFqns,
    Map<String, Object> jsonSchema,     // 仅 PUBLISHED 非空
    Map<String, Object> embedding,      // MV 仅占位
    boolean enabled,                     // 派生
    String shortName,                    // 派生
    String bundleCode,                   // 派生
    String version,                      // 派生
    LocalDateTime createdTime,
    LocalDateTime updatedTime
) {}

public record CreateRelationSchemaRequest(
    @NotBlank String packageFqn,
    @NotBlank String segment,
    @NotBlank String name,
    @NotBlank String description,
    @NotBlank String sourceFqn,
    @NotBlank String targetFqn,
    @NotBlank String associationType,
    @NotBlank String cardinalitySource,
    @NotBlank String cardinalityTarget,
    List<NativeAttributeDto> nativeAttributes,
    List<String> mountedTemplateFqns
) {}

public record CreateAttributeTemplateRequest(
    @NotBlank String bundleVersionFqn,
    @NotBlank String segment,
    @NotBlank String name,
    String description,
    @NotEmpty List<AttributeDefinitionDto> attributeDefinitions
) {}

public record NativeAttributeDto(
    @NotBlank String name,
    @NotBlank String type,           // string / number / integer / boolean / array
    boolean required,
    String description,
    Map<String, Object> constraints  // pattern, minimum, enum, etc.
) {}
```

---

## 4. PackageManagementService

### 接口定义

```java
public interface PackageManagementService {
    PackageDto createPackage(CreatePackageRequest request);
    PackageDto getPackage(String fqn);
    PackageDto updatePackage(String fqn, UpdatePackageRequest request);
    void deletePackage(String fqn);
    List<PackageDto> listPackages(String bundleVersionFqn);
}
```

### 方法说明

| 方法 | 描述 | 前置条件 | 后置条件 |
|------|------|---------|---------|
| `createPackage` | 创建 Package | 父 Package 存在（或根层），嵌套深度 ≤ 4 | FQN = parentFqn + "." + segment |
| `deletePackage` | 删除 Package | 所属版本为 DRAFT；Package 下无元素 | 级联删除子 Package |
| `listPackages` | 列出某版本下所有 Package | BundleVersion 存在 | 树形结构返回 |

---

## 5. ExportManifestService

### 接口定义

```java
public interface ExportManifestService {
    ExportManifestDto configureExport(String bundleVersionFqn, List<String> packageFqns);
    ExportManifestDto getExport(String bundleVersionFqn);
    boolean isPackageExported(String bundleVersionFqn, String packageFqn);
}
```

### 方法说明

| 方法 | 描述 | 前置条件 | 后置条件 |
|------|------|---------|---------|
| `configureExport` | 配置导出清单 | 版本为 DRAFT | 保存 Package FQN 白名单 |
| `getExport` | 查询导出清单 | 已配置 | 返回导出清单 |
| `isPackageExported` | 判断 Package 是否导出 | — | 返回 boolean |

---

## 6. BundleDependencyService

### 接口定义

```java
public interface BundleDependencyService {
    void declareDependency(String sourceVersionFqn, String targetVersionFqn);
    void removeDependency(String sourceVersionFqn, String targetVersionFqn);
    List<BundleDependencyDto> listDependencies(String sourceVersionFqn);
    DependencyGraphDto getDependencyGraph(String bundleFqn);
}
```

### 方法说明

| 方法 | 描述 | 前置条件 | 后置条件 |
|------|------|---------|---------|
| `declareDependency` | 声明依赖 | 目标版本存在且已发布，无循环依赖 | 创建 BundleDependency |
| `removeDependency` | 移除依赖 | 所属版本为 DRAFT | 删除依赖记录 |
| `getDependencyGraph` | 获取依赖图 | Bundle 存在 | 返回有向图结构，含循环检测结果 |

---

## 7. ImportExportService

### 接口定义

```java
public interface ImportExportService {
    String exportBundle(String bundleVersionFqn, ExportFormat format);
    String exportPackage(String packageFqn, ExportFormat format);
    ImportResult importBundle(String content, ImportFormat format, ImportStrategy strategy);
}
```

### 方法说明

| 方法 | 描述 | 前置条件 | 后置条件 |
|------|------|---------|---------|
| `exportBundle` | 导出完整 Bundle | 版本为 PUBLISHED | 返回 YAML/JSON 格式内容 |
| `exportPackage` | 导出单个 Package | 版本为 PUBLISHED，Package 存在 | 含依赖的属性模板组 |
| `importBundle` | 导入 Bundle | 解析顺序合法，依赖完整 | 生成草稿版本（不自动发布） |

---

## 8. ValidationService

### 接口定义

```java
public interface ValidationService {
    ValidationReport validateSave(String bundleVersionFqn);
    ValidationReport validatePublish(String bundleVersionFqn);
    ValidationReport previewPublish(String bundleVersionFqn);
}
```

### 方法说明

| 方法 | 描述 | 前置条件 | 后置条件 |
|------|------|---------|---------|
| `validateSave` | 写入轻量校验 | 版本为 DRAFT | 返回校验报告 |
| `validatePublish` | 发布前全局校验 | 版本为 DRAFT | 执行所有校验项，返回报告 |
| `previewPublish` | 预览发布（仅校验不落库） | 版本为 DRAFT | 不修改数据，返回完整校验报告 |

### 校验报告 DTO

```java
public record ValidationReport(
    boolean passed,
    List<ValidationError> errors,
    List<ValidationWarning> warnings
) {}

public record ValidationError(
    String elementFqn,    // 精准定位到元素
    String field,          // 精准定位到字段
    String errorCode,      // 错误码
    String message         // 人类可读消息
) {}
```

---

## 错误码约定

本 BC 使用错误码范围 **30100-30199**（参见 foundation-adaptation.md 异常处理章节）。

### 调用方（下游 BC）使用示例

```java
// 引入 api 模块依赖后直接注入
@Autowired
private BundleManagementService bundleService;

BundleDto bundle = bundleService.getBundle("order");
```

### 事务边界

每个 Application Service 方法为一个事务边界。发布操作（`publish`）为原子事务——校验失败全量回滚。
