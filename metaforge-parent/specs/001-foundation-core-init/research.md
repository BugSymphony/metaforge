# Research: foundation-core 基座初始化

## 1. Java 21 虚拟线程配置最佳实践

**Decision**: 使用 Spring Boot 3.4+ 内置的虚拟线程支持，通过 `spring.threads.virtual.enabled=true` 配置属性全局启用。Tomcat 请求处理线程池和 `@Async` 异步任务线程池均切换到虚拟线程。

**Rationale**: Spring Boot 3.2+ 提供了对虚拟线程的一等支持。`TomcatProtocolHandlerCustomizer` 自动将请求处理切换到虚拟线程；`SimpleAsyncTaskExecutor` 在启用虚拟线程后自动使用 `Thread.ofVirtual().factory()`。这是最简洁且符合 Spring Boot 约定优于配置哲学的方案。

**Alternatives considered**:
- 手动配置 `ThreadPoolTaskExecutor`：需要显式定义多个 Bean，配置繁琐，且容易遗漏某个线程池的切换
- 使用 Loom 早期 API（`Thread.startVirtualThread`）：绕过 Spring 自动配置，需要自行管理线程生命周期

**Implementation**:
```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.threads", name = "virtual.enabled", havingValue = "true", matchIfMissing = true)
public class VirtualThreadConfig {
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
```

---

## 2. Maven 多模块 Monorepo 版本治理

**Decision**: 使用 `metaforge-parent` 作为根 POM（packaging=pom），通过 `<dependencyManagement>` 声明全平台 BOM，通过 `<modules>` 聚合所有子模块。使用 `flatten-maven-plugin` 解决 `${revision}` 变量在部署时的解析问题。使用 `maven-enforcer-plugin` 强制执行依赖规则。

**Rationale**: 单仓库多模块是 Java 生态中最成熟的单体拆分模式。父 POM 统一管理版本消除了版本碎片化风险，Maven reactor 保证构建顺序正确。`flatten-maven-plugin` 是 Maven 官方推荐的 CI 友好 POM 方案。

**Key version choices**:
| 依赖 | 版本 | 选择理由 |
|------|------|----------|
| Spring Boot | 3.4.3 | 截至 2026-08-01 最新稳定版，完整支持 Java 21 虚拟线程 |
| Spring AI | 1.0.0-M6 | MCP Server 发布所需，Milestone 版本在 MVP 阶段可接受 |
| Hibernate | 6.6.x | Spring Boot 3.4 默认版本，支持 JSONB 映射 |
| Flyway | 10.x | Spring Boot 3.4 默认版本，原生 PostgreSQL 支持 |
| Jackson | 2.18.x | Spring Boot 3.4 默认版本 |
| Caffeine | 3.1.x | Spring Boot 3.4 默认版本 |
| TestContainers | 1.20.x | 最新稳定版，PostgreSQL 16 镜像支持 |
| PostgreSQL Driver | 42.7.4 | 支持 PostgreSQL 16，与 TestContainers 版本兼容 |

**Alternatives considered**:
- Gradle 多模块：团队更熟悉 Maven，Maven 在企业 Java 项目中市场占有率更高，`flatten-maven-plugin` 成熟度优于 Gradle 的 `version-catalog`
- `pom.xml` 中硬编码版本号而非 `${revision}`：无法实现 CI 流水线中的动态版本注入

---

## 3. Spring Boot 3 AutoConfiguration 机制

**Decision**: 使用 Spring Boot 3.x 的新 AutoConfiguration 注册机制——通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件注册自动装配类（替代 Spring Boot 2.x 的 `spring.factories`）。每个横切能力对应一个独立的 `@AutoConfiguration` 类。

**Rationale**: Spring Boot 3.x 废弃了 `spring.factories` 机制，新机制按行声明更简洁且启动性能更好。独立装配类符合单一职责原则（FR-003），便于按需禁用的演进（尽管 MVP 阶段不需要开关）。

**AutoConfiguration 装配顺序**:
```java
@AutoConfiguration(before = {JacksonConfig.class, WebMvcConfig.class})
public class VirtualThreadConfig { ... }

@AutoConfiguration(after = VirtualThreadConfig.class, before = WebMvcConfig.class)
public class JacksonConfig { ... }

@AutoConfiguration(after = {VirtualThreadConfig.class, JacksonConfig.class})
public class WebMvcConfig { ... }
```

---

## 4. Flyway 多 BC 迁移脚本管理

**Decision**: 使用 Flyway 扁平目录结构，所有 BC 迁移脚本集中在 `metaforge-boot/src/main/resources/db/migration/`。脚本命名使用 `V<version>__<bc-name>_<purpose>.sql` 格式，按版本号全局顺序执行。多 BC 共享同一 PostgreSQL 数据库，通过独立 Schema 实现逻辑隔离。

**Rationale**: Flyway 原生支持扁平目录，按版本号排序确保迁移顺序确定性。BC 名称前缀使脚本归属可追溯。全局顺序执行避免了多 BC 独立 Flyway 实例的初始化竞争问题。

**脚本命名规范**:
```
V1__bc_sample_ddl.sql       # bc-sample 建表
V2__bc_sample_init.sql      # bc-sample 初始化数据
V3__bc_user_ddl.sql         # 用户 BC 建表
V4__bc_user_init.sql        # 用户 BC 初始化数据
```

**Alternatives considered**:
- 每个 BC 独立 Flyway 实例：多 Flyway 实例需要 `flyway.locations` 配置隔离，初始化顺序不确定，可能出现 Schema 依赖死锁
- 按 BC 子目录组织脚本：Flyway 默认不支持递归扫描子目录，需要额外 `flyway.locations` 配置，增加了配置复杂度

---

## 5. TestContainers 集成测试策略

**Decision**: 在 `metaforge-framework` 中提供 `BaseIntegrationTest` 抽象基类，使用 `@Testcontainers` + `@Container` 注解自动管理 PostgreSQL 容器生命周期。使用 `@ServiceConnection` 自动配置数据源连接到容器。

**Rationale**: TestContainers 是 Java 生态中集成测试容器化的事实标准。`@ServiceConnection`（Spring Boot 3.1+）自动替换 `spring.datasource.url` 等属性，无需手动设置 `@DynamicPropertySource`。测试结束后容器自动销毁，确保测试数据隔离。

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}
```

**BaseUnitTest 策略**:
- 不使用 `@SpringBootTest`，不启动 Spring 上下文
- 使用 `@ExtendWith(MockitoExtension.class)` 提供 Mockito 支持
- 目标：单测试方法执行 < 100ms

---

## 6. 统一 REST 响应体包装方案

**Decision**: 使用 `@RestControllerAdvice` + `ResponseBodyAdvice` 实现统一响应体自动包装，而非 AOP 或 Filter。通过 `@ControllerAdvice` 的 `basePackages` 限制扫描范围到 `com.metaforge`。

**Rationale**: `ResponseBodyAdvice` 是 Spring MVC 官方的响应体拦截机制，在 `HttpMessageConverter` 写入前执行，性能开销最小。AOP 方案需要创建代理对象，Filter 方案在 Servlet 层面操作响应流，都不如 `ResponseBodyAdvice` 优雅。

**Alternatives considered**:
- 自定义注解 `@ApiResponseWrapper`：需要开发者手动标注每个 Controller，违背零侵入原则
- HandlerInterceptor + 手动包装：需要在 Controller 返回后再次写入响应，容易导致响应体重复写入

---

## 7. TraceId 全链路透传方案

**Decision**: 使用 SLF4J MDC（Mapped Diagnostic Context）+ `OncePerRequestFilter` 实现。请求入口生成 UUID，写入 MDC，日志自动携带。异步线程通过 `TaskDecorator` 透传 MDC 上下文。响应头通过 `HttpServletResponse.addHeader("X-Trace-Id", ...)` 返回。同时配置 Tomcat 虚拟线程的 `ThreadFactory` 确保子线程继承父线程的 MDC。

**Rationale**: MDC 是 SLF4J 标准的诊断上下文机制，Logback/Log4j2 原生支持。Filter + TaskDecorator 组合覆盖了同步请求处理和 `@Async` 异步任务的 TraceId 透传。虚拟线程下 ThreadLocal（MDC 底层依赖）的继承行为需要显式处理。

---

## 8. Jackson 全局序列化配置

**Decision**: 通过 `Jackson2ObjectMapperBuilderCustomizer` Bean 全局配置：
- 日期格式：`yyyy-MM-dd HH:mm:ss`
- 时区：`Asia/Shanghai`（默认）
- 空值策略：`NON_NULL`
- 禁用 `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS`
- 注册 `JavaTimeModule`

JSONB 序列化工具在 `metaforge-common` 的 `JsonbUtils` 中提供，使用 `ObjectMapper` 实例处理 `PGobject` 与 Java 对象的双向转换。

**Rationale**: 全局一致的时间格式避免前端解析混乱，NON_NULL 减少 JSON 体积。JavaTimeModule 是 Java 8+ 时间 API 的必要支持。

---

## 9. Caffeine 缓存默认策略

**Decision**: 通过 `CaffeineCacheManager` 创建默认缓存配置：
- `expireAfterWrite`: 30 分钟
- `maximumSize`: 1000
- `recordStats`: true（接入 Actuator 指标）

业务 BC 通过 `CacheManager.getCache("<bc-name>:<entity>:<id>")` 模式使用缓存，CacheManager 自动创建缺失的 Cache 实例（使用默认配置）。

**Key 命名约定**: `<bc-name>:<entity>:<id>` 格式，如 `user-bc:user:42`。

---

## 10. Maven Enforcer 规则配置

**Decision**: 在 `metaforge-parent/pom.xml` 中配置 `maven-enforcer-plugin`：
1. `banTransitiveDependencies` 规则：禁止 common 传递引入 Spring/JPA/Servlet 等框架依赖
2. `requireUpperBoundDeps` 规则：确保版本收敛到 BOM 管理的版本
3. 自定义规则：禁止 BC 声明 `<dependencyManagement>`、禁止覆盖 BOM 版本属性、禁止依赖 `metaforge-boot`（除 boot 自身外的所有模块）

**构建校验命令**: `mvn validate` 在 Maven default lifecycle 中自动执行 enforcer 规则。
