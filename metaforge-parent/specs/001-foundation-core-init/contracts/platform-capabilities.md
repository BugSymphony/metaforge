# Platform Capabilities: foundation-core 平台能力声明清单

本文档逐项声明 foundation-core 已默认启用且业务 BC 禁止重复实现的平台能力。每个能力项简要说明能力范围与 BC 的约束（"BC 无需/禁止重复"）。

---

## 能力清单

### 1. 虚拟线程 (Virtual Threads)

**能力来源**: FR-006

**能力说明**: 全平台 HTTP 请求处理线程池和 `@Async` 异步任务线程池均使用 Java 21 虚拟线程。Tomcat 请求处理、Spring `@Async` 注解标记的方法自动运行在虚拟线程上。

**BC 约束**: BC 无需配置任何线程池。严禁 BC 自定义 `ThreadPoolTaskExecutor` 或覆盖 `TaskExecutor` Bean。

---

### 2. 日志脱敏

**能力来源**: FR-010

**能力说明**: 日志输出自动对敏感字段（password, secret, token, phone, email, idCard 等）执行脱敏。logback/log4j2 日志格式已标准化，自动包含 TraceId、时间戳、线程名等字段。

**BC 约束**: BC 无需配置日志脱敏规则（常用敏感字段已覆盖）。如需自定义脱敏规则，通过实现 `LogMaskSpi` 扩展点添加，禁止在 BC 内部自行编写日志脱敏工具。

---

### 3. API 文档 (OpenAPI 3.0 / SpringDoc)

**能力来源**: FR-014

**能力说明**: 所有 REST 接口自动生成 OpenAPI 3.0 文档。Swagger UI 可通过 `/swagger-ui.html` 访问。BC 的 Controller 通过 `@Tag(name = "<bc-name>")` 标注，Swagger UI 自动按 Tag 分组展示。

**BC 约束**: BC 无需配置 SpringDoc 或引入额外的 API 文档库。严禁 BC 在自身模块中添加 `springdoc-openapi` 依赖或自定义 `OpenAPI` Bean。

---

### 4. 国际化 (i18n)

**能力来源**: FR-015

**能力说明**: 全局 `MessageSource` 已配置，资源文件路径为 `i18n/messages`，默认支持 `zh-CN` 和 `en-US`。请求头 `Accept-Language` 自动识别。Spring 的 `MessageSource` Bean 已就绪，BC 可直接注入使用。

**BC 约束**: BC 无需配置 `MessageSource`。如需扩展国际化消息，在 `metaforge-boot/src/main/resources/i18n/` 下添加 `messages_<bc-name>_<locale>.properties` 文件，由 `spring.messages.basename` 统一引入。严禁 BC 在自身模块中定义独立的 `MessageSource` Bean。

---

### 5. 可观测性 (Actuator + Metrics)

**能力来源**: FR-016

**能力说明**: 已暴露 `/actuator/health`（健康检查）和 `/actuator/info`（应用信息）、`/actuator/metrics`（指标）端点。JVM 指标（内存、GC、线程）、HTTP 请求指标（请求数、响应时间分布）已自动采集。

**BC 约束**: BC 无需配置 Actuator。如需添加自定义健康检查项，实现 `HealthCheckSpi` 扩展点。严禁 BC 自定义 Actuator 端点路径或覆盖默认指标注册。

---

### 6. 安全基线

**能力来源**: FR-017

**能力说明**: 已配置 `XSSFilter`（防 XSS 注入）和请求体大小限制（默认 10MB）。跨域 CORS 已标准化配置（默认允许同源，跨域需在 `metaforge-boot` 的 `application.yml` 中按需配置）。SQL 注入防护由 JPA/Hibernate 参数化查询天然防护。

**BC 约束**: BC 无需配置安全过滤器。如需自定义 CORS 规则，在 `metaforge-boot` 的 `application.yml` 中配置，禁止在 BC 内部添加 `CorsFilter` 或 `WebMvcConfigurer.addCorsMappings()`。

---

### 7. 数据源 (HikariCP 连接池 + 事务管理)

**能力来源**: FR-018

**能力说明**: PostgreSQL 数据源已配置（HikariCP 连接池），事务管理器已配置，`@Transactional` 注解默认生效。Flyway 在启动时自动执行数据库迁移。

**BC 约束**: BC 无需配置数据源或事务管理器。所有数据库连接使用统一数据源，严禁 BC 在自身模块中定义独立的数据源 Bean。多 BC 通过独立 Schema 实现逻辑隔离。

---

### 8. 跨 Schema 写校验

**能力来源**: FR-020

**能力说明**: 提供跨 Schema 写操作校验能力，强制遵循单一数据所有权原则（一个 BC 仅可写自身 Schema）。通过 Flyway 初始化时按 BC 创建独立 Schema，应用层通过注解或配置声明 BC 的 Schema 所有权。

**BC 约束**: BC 只能在自身 Schema 内执行 DML 操作（INSERT/UPDATE/DELETE）。跨 BC Schema 的 SELECT 查询允许，但需遵循契约定义的读取字段范围。

---

### 9. Flyway 数据库迁移统一管理

**能力来源**: FR-020a

**能力说明**: Flyway 在应用启动时自动按版本号全局顺序执行所有 BC 的迁移脚本。迁移脚本存放在 `metaforge-boot/src/main/resources/db/migration/`，使用扁平目录结构（不按 BC 子目录隔离。所有 BC 共享同一数据库，通过独立 Schema 逻辑隔离。

**BC 约束**: BC 严禁在自身模块中配置 Flyway 或引入 flyway-core 依赖。每个 BC 按 `V<n>__<bc-name>_ddl.sql` 和 `V<n+1>__<bc-name>_init.sql` 命名规范提供迁移脚本，提交到 `metaforge-boot` 的 `db/migration/` 目录。

---

### 10. 测试基座 (BaseUnitTest / BaseIntegrationTest + TestContainers)

**能力来源**: FR-028 / FR-029

**能力说明**: `metaforge-framework` 提供 `BaseUnitTest`（纯单元测试基类，无 Spring 上下文，使用 Mockito）和 `BaseIntegrationTest`（集成测试基类，内置 TestContainers PostgreSQL 容器自动启停，使用 Spring Boot Test 上下文）。业务 BC 通过 test scope 依赖 `metaforge-framework` 继承这些基类。

**BC 约束**: BC 无需引入 TestContainers 依赖，无需配置测试数据源。严禁 BC 自定义 PostgreSQL 容器化测试基础设施。测试配置自动与生产配置隔离（`application-test.yml`）。
