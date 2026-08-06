# 基础平台适配设计: 元模型治理核心能力 MVP

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Foundation**: foundation-core v1.0.0

## 1. 虚拟线程

### 接入动作

BC 不进行任何线程池配置。HTTP 请求处理与 `@Async` 方法自动使用 foundation-core 预配置的 Java 21 虚拟线程。

### 落地方案

- 不在 `application.yml` 或任何 `@Configuration` 类中定义 `TaskExecutor` Bean
- 不声明 `@EnableAsync`（由 foundation-core 统一启用）
- 异步操作使用 `@Async` 注解，例如发布时异步生成 JSON Schema（如需要）

### 合规结论

**PASS** — BC 不创建任何线程池配置，满足 foundation-core 虚拟线程约束。

---

## 2. 日志脱敏

### 接入动作

BC 不配置自定义脱敏规则。如需对特定字段脱敏，实现 `LogMaskSpi` 扩展接口。

### 落地方案

- 不在 BC 中引入 logback/log4j2 自定义配置
- 不创建日志脱敏工具类
- 如有特殊敏感字段（如 `fqn` 本身不敏感，无特殊需求），不实现 `LogMaskSpi`

### 合规结论

**PASS** — BC 不自定义日志脱敏配置，保持 foundation-core 默认规则。

---

## 3. OpenAPI 文档

### 接入动作

REST Controller 使用 `@Tag(name = "metamodel")` 标注，由 foundation-core 的 SpringDoc 自动生成 OpenAPI 3.0 文档。

### 落地方案

```java
@RestController
@RequestMapping("/api/v1/metamodel")
@Tag(name = "metamodel")  // Swagger UI 分组名称
public class BundleController {
    // ...
}
```

- 不在 BC 的 `pom.xml` 中添加 `springdoc-openapi` 依赖
- 不自定义 `OpenAPI` Bean
- 不在 `application.yml` 中配置 `springdoc.*`（除非需要覆盖默认值）

### 合规结论

**PASS** — 仅使用 `@Tag` 标注，不自定义 SpringDoc 配置。

---

## 4. 国际化 i18n

### 接入动作

注入 Spring `MessageSource` 进行消息国际化。BC 消息文件添加至 `metaforge-boot/src/main/resources/i18n/` 目录。

### 落地方案

资源文件：
- `i18n/messages_metamodel_zh_CN.properties` — 简体中文
- `i18n/messages_metamodel_en_US.properties` — 英文

使用方式：
```java
@Autowired
private MessageSource messageSource;

String msg = messageSource.getMessage(
    "metamodel.error.fqn_duplicate",
    new Object[]{fqn},
    LocaleContextHolder.getLocale()
);
```

- 不定义独立的 `MessageSource` Bean
- 消息 key 命名规范: `metamodel.<category>.<detail>`

### 合规结论

**PASS** — 使用 foundation-core 预配置的 MessageSource，添加 BC 专属消息文件。

---

## 5. 可观测性

### 接入动作

不自定义 Actuator 配置。如需自定义健康检查，实现 `HealthCheckSpi` 扩展接口。

### 落地方案

```java
@Component
public class MetamodelHealthCheck implements HealthCheckSpi {
    @Override
    public HealthCheckResult check() {
        // 检查数据库连接可用性
        return new HealthCheckResult(
            "metamodel-governance",
            true,
            "元模型治理 BC 运行正常"
        );
    }
}
```

- 不配置 `management.*` 属性（使用 foundation-core 默认值）
- 自定义健康检查通过 `HealthCheckSpi` 注册

### 合规结论

**PASS** — 通过 SPI 扩展健康检查，不自定义 Actuator 配置。

---

## 6. 安全基线

### 接入动作

BC 不配置安全过滤器、CORS 配置。基础安全（XSS、请求体大小限制、SQL 注入防护）由 foundation-core 统一处理。

### 落地方案

- 不添加 `CorsFilter` Bean
- 不实现 `WebMvcConfigurer.addCorsMappings()`
- 不引入 Spring Security 依赖
- 若 MVP 阶段需要跨域配置，在 `application.yml` 中配置全局 CORS

### 合规结论

**PASS** — BC 不自定义安全过滤器。

---

## 7. 数据源与事务

### 接入动作

使用 foundation-core 预配置的 HikariCP 连接池与统一事务管理器。BC 仅操作 `metamodel_governance` Schema。

### 落地方案

- 所有 JPA Repository 的 `@Query` 中不显式指定 Schema（由 Hibernate `default_schema` 配置自动路由）
- JPO 实体通过 `@Table(schema = "metamodel_governance")` 声明所属 Schema
- 跨 Schema SELECT 仅在合约范围内执行（如查询 `metamodel_governance` 外无此需求）

### 合规结论

**PASS** — BC 不自定义数据源配置，仅操作自身 Schema。

---

## 8. 跨 Schema 写校验

### 接入动作

BC 写入操作严格限定在 `metamodel_governance` Schema，由 foundation-core 的跨 Schema 写校验机制保护。

### 落地方案

- 所有 INSERT/UPDATE/DELETE 操作均通过 JPA Repository 在 `metamodel_governance` Schema 内执行
- 不存在跨 BC Schema 写需求

### 合规结论

**PASS** — 无跨 Schema 写入场景。

---

## 9. Flyway 迁移

### 接入动作

BC 提供 Flyway 迁移脚本，按 `V<n>__metamodel_governance_<type>.sql` 命名，提交至 `metaforge-boot/src/main/resources/db/migration/`。

### 落地方案

| 版本 | 文件 | 说明 |
|------|------|------|
| V1 | `V1__metamodel_governance_ddl.sql` | 创建 Schema 及所有实体表 |
| V2 | `V2__metamodel_governance_init.sql` | 预置 `metaforge` Bundle v1.0.0 |

- BC 的 `pom.xml` 中不添加 `flyway-core` 依赖
- 不配置 `spring.flyway.*` 属性

### 合规结论

**PASS** — 迁移脚本提交至 foundation-core 统一管理路径。

---

## 10. 测试基类

### 接入动作

BC 单元测试继承 `BaseUnitTest`，集成测试继承 `BaseIntegrationTest`。不引入 TestContainers 依赖。

### 落地方案

```java
// 单元测试
class FqnGeneratorTest extends BaseUnitTest {
    @Test
    void testParseFqn() { ... }
}

// 集成测试
@SpringBootTest
class BundleRepositoryIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private BundleRepository bundleRepository;

    @Test
    void testSaveAndFindByFqn() { ... }
}
```

- BC 的 `pom.xml` 中通过 test-jar scope 引入 `metaforge-framework`
- 不添加 `testcontainers` 或 `junit-jupiter` 的直接依赖

### 合规结论

**PASS** — 继承 foundation-core 测试基类，不引入 TestContainers 直接依赖。

---

## 11. 统一响应格式

### 接入动作

所有 REST 接口返回 `ApiResponse<T>` 包装，使用 foundation-core 的统一响应格式。

### 落地方案

```java
@GetMapping("/{fqn}")
public ApiResponse<BundleDto> getByFqn(@PathVariable String fqn) {
    BundleDto dto = bundleService.findByFqn(fqn);
    return ApiResponse.success(dto);
}

@PostMapping
public ApiResponse<BundleDto> create(@Valid @RequestBody CreateBundleRequest req) {
    BundleDto dto = bundleService.create(req);
    return ApiResponse.success(dto, "Bundle 创建成功");
}
```

- 不自定义响应包装类
- 全局异常处理映射到 foundation-core 错误码体系

### 合规结论

**PASS** — 使用 `ApiResponse<T>` 统一响应格式。

---

## 12. 分页组件

### 接入动作

使用 `PageRequest` / `PageResult<T>` + `PageHelper` 进行分页查询处理。

### 落地方案

```java
public ApiResponse<PageResult<BundleDto>> list(PageRequest pageRequest) {
    Pageable pageable = PageHelper.toSpringPageRequest(pageRequest);
    Page<Bundle> page = bundleRepository.findAll(pageable);
    PageResult<BundleDto> result = PageHelper.fromSpringPage(page.map(mapper::toDto));
    return ApiResponse.success(result);
}
```

### 合规结论

**PASS** — 使用 foundation-core 提供的分页 DTO 和工具类。

---

## 13. JSONB 序列化

### 接入动作

使用 `JsonbUtils.toJsonb()` / `fromJsonb()` 进行 JSONB 字段的序列化与反序列化。

### 落地方案

```java
// 持久化
@Entity
@Table(schema = "metamodel_governance")
class EntitySchemaJpo {
    @Column(columnDefinition = "jsonb")
    private String nativeAttributes;  // JSONB -> String

    public void setNativeAttributesObj(List<NativeAttribute> attrs) {
        this.nativeAttributes = JsonbUtils.toJsonb(attrs);
    }

    public List<NativeAttribute> getNativeAttributesObj() {
        return JsonbUtils.fromJsonb(nativeAttributes, ...);
    }
}
```

- 不自定义 Jackson `ObjectMapper` 或序列化工具
- JSONB 列使用 `@Column(columnDefinition = "jsonb")` + `String` Java 类型

### 合规结论

**PASS** — 使用 foundation-core 的 `JsonbUtils`。

---

## 14. 缓存

### 接入动作

使用 Caffeine `CacheManager`，Key 命名格式: `metamodel:<entity>:<id>`。

### 落地方案

缓存适用场景：
- `metamodel:bundle:<fqn>` — Bundle 按 FQN 查询
- `metamodel:export_manifest:<versionFqn>` — 导出清单缓存（发布后写入，变更频率极低）

不缓存场景：
- 草稿态实体（频繁变更，缓存收益低）
- 校验结果（实时性要求高）

### 合规结论

**PASS** — 使用 foundation-core Caffeine 缓存，Key 命名符合规范。

---

## 15. 异常处理与常量管理

### 接入动作

自定义业务异常通过 `ExceptionHandlerSpi` 注册，错误码范围使用 30100-30199（metamodel-governance BC 分配范围）。

**常量集中管理**：所有错误码、异常码统一定义在 `metaforge-metamodel-api` 模块的 `constants/ErrorCodes.java` 中，确保单一数据源（SSOT）。业务层代码通过 `ErrorCodes.FQN_DUPLICATE` 等常量引用，严禁硬编码数值。

### 落地方案

错误码分配（定义于 `api/constants/ErrorCodes.java`）：

| 常量名 | 错误码 | 对应异常 | 说明 |
|--------|--------|----------|------|
| `FQN_DUPLICATE` | 30101 | `FqnDuplicateException` | FQN 全局重复 |
| `FQN_NOT_FOUND` | 30102 | `FqnNotFoundException` | FQN 引用目标不存在 |
| `VERSION_NOT_DRAFT` | 30103 | `VersionNotDraftException` | 非草稿态版本不可编辑 |
| `UPGRADE_LEVEL_MISMATCH` | 30104 | `UpgradeLevelMismatchException` | 升级等级与变更不匹配 |
| `CIRCULAR_DEPENDENCY` | 30105 | `CircularDependencyException` | 循环依赖检测到环 |
| `ATTR_NAME_CONFLICT` | 30106 | `AttributeNameConflictException` | 属性名冲突 |
| `PACKAGE_DEPTH_EXCEEDED` | 30107 | `PackageDepthExceededException` | Package 嵌套深度超限 |
| `EXPORT_VALIDATION_FAILED` | 30108 | `ExportValidationException` | 导出清单校验失败 |
| `PUBLISHED_IMMUTABLE` | 30109 | `PublishedImmutableException` | 已发布版本不可修改 |
| `PREDEFINED_BUNDLE_PROTECTED` | 30110 | `PredefinedBundleProtectedException` | 预置 Bundle 受保护 |
| `DEPENDENCY_TARGET_NOT_FOUND` | 30111 | `DependencyTargetNotFoundException` | 依赖目标不存在 |
| `IMPORT_PARSE_FAILED` | 30112 | `ImportParseException` | 导入解析失败 |

```java
// api/constants/ErrorCodes.java
public final class ErrorCodes {
    private ErrorCodes() {}

    public static final int FQN_DUPLICATE = 30101;
    public static final int FQN_NOT_FOUND = 30102;
    // ... 其余错误码

    public static final int VERSION_NOT_DRAFT = 30103;
    public static final int UPGRADE_LEVEL_MISMATCH = 30104;
    public static final int CIRCULAR_DEPENDENCY = 30105;
    public static final int ATTR_NAME_CONFLICT = 30106;
    public static final int PACKAGE_DEPTH_EXCEEDED = 30107;
    public static final int EXPORT_VALIDATION_FAILED = 30108;
    public static final int PUBLISHED_IMMUTABLE = 30109;
    public static final int PREDEFINED_BUNDLE_PROTECTED = 30110;
    public static final int DEPENDENCY_TARGET_NOT_FOUND = 30111;
    public static final int IMPORT_PARSE_FAILED = 30112;
}
```

### 合规结论

**PASS** — 使用 SPI 注册异常映射，错误码在 30000-49999 范围内。所有错误码通过 api 模块 `constants/` 常量类集中管理。

---

## 16. Maven 构建

### 接入动作

BC 的 `pom.xml` 继承 `metaforge-parent`，仅声明 `metaforge-framework` 依赖，不自定义版本号。

### 落地方案

```xml
<parent>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-parent</artifactId>
    <version>${revision}</version>
</parent>

<artifactId>metaforge-metamodel</artifactId>
<name>BC: metamodel-governance</name>

<dependencies>
    <dependency>
        <groupId>com.metaforge</groupId>
        <artifactId>metaforge-framework</artifactId>
    </dependency>
</dependencies>
```

- 不声明 `<dependencyManagement>`
- 不覆盖 `${revision}` 或其他版本属性
- 不在 `<dependencies>` 中使用 `<version>` 标签
- 通过根 `pom.xml` 的 `<modules>` 注册 BC

### 合规结论

**PASS** — 遵循 foundation-core 的 Maven 构建规范。
