# Tasks: foundation-core 基座初始化

**Input**: Design documents from `/specs/001-foundation-core-init/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested in spec. Test tasks only included where spec mandates testing (SC-005, SC-006). Unit/Integration test base classes (BaseUnitTest, BaseIntegrationTest) covered under US5.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4, US5)
- Include exact file paths in descriptions

## Path Conventions

**BC root**: `metaforge-parent/` (Maven monorepo multi-module)
- All module paths relative to `metaforge-parent/`

---

## Phase 1: Setup (Project Scaffolding)

**Purpose**: Create Maven multi-module project structure, parent POM with BOM, docker-compose

- [X] T001 Create `metaforge-parent/pom.xml` — 父 POM (packaging=pom)，声明 `<groupId>com.metaforge</groupId>`, `<artifactId>metaforge-parent</artifactId>`, `<version>${revision}</version>`；配置 `<properties>` 包含所有核心依赖版本（Spring Boot 3.4.3、Spring AI 1.0.0-M6、Jackson 2.18.x、Caffeine 3.1.x、PostgreSQL Driver 42.7.4、Flyway 10.x、TestContainers 1.20.x、Hibernate 6.6.x、SpringDoc 2.7.x）；配置 `<dependencyManagement>` BOM（spring-boot-dependencies、spring-ai-bom）；配置 `<modules>` 聚合 metaforge-common、metaforge-framework、metaforge-server、metaforge-boot、bc-sample
- [X] T002 [P] Configure flatten-maven-plugin in `metaforge-parent/pom.xml` — `<flattenMode>oss</flattenMode>`，`${revision}` 变量在 deploy 时解析为实际版本号
- [X] T003 [P] Create `metaforge-parent/docker-compose.yml` — PostgreSQL 16 服务定义（端口 5432、用户名/密码 metaforge/metaforge、数据库名 metaforge），与 `application.yml` 数据源配置一致
- [X] T004 [P] Configure maven-enforcer-plugin in `metaforge-parent/pom.xml` — `<requireUpperBoundDeps/>` 版本收敛规则（FR-001、FR-005）；bannedDependencies 禁止依赖 `com.metaforge:metaforge-boot`；banDependencyManagementScope 禁止 BC 声明 `<dependencyManagement>`（FR-005）
- [X] T005 Create `metaforge-parent/.gitignore` — 忽略 target/、.flattened-pom.xml、*.iml、.idea/

**Checkpoint**: `mvn validate` 通过，Maven reactor 结构就绪

---

## Phase 2: Foundational — metaforge-common + metaforge-framework (Blocking Prerequisites)

**Purpose**: 纯 Java 工具层（common）和框架工具层（framework），所有下游模块和用户故事的基础

**⚠️ CRITICAL**: 所有用户故事依赖此阶段完成

### metaforge-common 模块（FR-002）

- [X] T006 Create `metaforge-common/pom.xml` — 继承 `metaforge-parent`；声明依赖 Jackson（core + databind + annotations）和 SLF4J API（compile scope）；严禁引入 Spring、Servlet API、JPA、Caffeine、Flyway 等框架依赖
- [X] T007 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/exception/BaseException.java` — abstract 异常基类，含 code(int)、message(String)、detail(String) 字段及构造方法
- [X] T008 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/exception/SystemException.java` — 系统级异常（错误码 10000-19999）：SystemException(10001)、DatabaseException(10002)、RemoteCallException(10003)
- [X] T009 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/exception/BizException.java` — 业务异常基类（错误码范围供 BC 在 30000-49999 分配）；ValidationException(20002)
- [X] T010 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/constant/ErrorCodes.java` — 错误码常量类：SYSTEM_ERROR(10000)、VALIDATION_ERROR(20001) 等
- [X] T011 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/constant/TraceConstants.java` — TraceId 常量：TRACE_ID_HEADER="X-Trace-Id"、TRACE_ID_MDC_KEY="traceId"
- [X] T012 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/dto/ApiResponse.java` — 统一响应体：code(int)、message(String)、data(T)、traceId(String)；工厂方法 `success(data)`、`error(code, message)`
- [X] T013 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/dto/PageRequest.java` — 分页请求 DTO：page(int, min=1, default=1)、size(int, 1-100, default=20)、sort(String, pattern `field:asc|desc`)
- [X] T014 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/dto/PageResult.java` — 分页结果 DTO：content(List\<T\>)、total(long)、page、size、totalPages
- [X] T015 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/dto/BaseEntity.java` — 基础实体 DTO：id(Long)、createdTime(LocalDateTime)、updatedTime(LocalDateTime)
- [X] T016 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/util/PageUtils.java` — 内存分页工具：`paginate(List<T>, PageRequest) → PageResult<T>`
- [X] T017 [P] Create `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/util/JsonbUtils.java` — JSONB 序列化工具：`toJsonb(Object) → String`、`fromJsonb(String, Class<T>) → T`、`fromJsonbList(String, Class<T>) → List<T>`；日期格式 `yyyy-MM-dd HH:mm:ss`，NON_NULL
- [X] T018 [P] Create SPI 扩展点接口 in `metaforge-parent/metaforge-common/src/main/java/com/metaforge/common/spi/`：
  - `ExceptionHandlerSpi.java` — `ApiResponse<?> handle(Exception e)`，返回 null 表示不处理
  - `RequestInterceptorSpi.java` — preHandle/postHandle/afterCompletion 默认方法
  - `LogMaskSpi.java` — `String mask(String fieldName, String fieldValue)`
  - `HealthCheckSpi.java` — `HealthCheckResult check()`，返回 name/healthy/detail
  - `SerializationSpi.java` — `customizeSerializer(mapper)` / `customizeDeserializer(mapper)`
  - `ValidationSpi.java` — `registerValidators(registry)`
- [X] T019 Verify metaforge-common compile scope 依赖仅含 Jackson + SLF4J（`mvn dependency:list -pl metaforge-common -DincludeScope=compile`）——对应 SC-005

### metaforge-framework 模块（FR-002a）

- [X] T020 Create `metaforge-framework/pom.xml` — 继承 `metaforge-parent`；依赖 `metaforge-common`、spring-boot-starter-data-jpa、spring-boot-starter-web、caffeine、hibernate-core（compile scope）
- [X] T021 [P] Create `metaforge-parent/metaforge-framework/src/main/java/com/metaforge/framework/web/PageHelper.java` — common `PageRequest` ↔ Spring `Pageable` 转换（`toSpringPageable`、`fromSpringPage`）（FR-019）
- [X] T022 [P] Create `metaforge-parent/metaforge-framework/src/main/java/com/metaforge/framework/spring/SpringContextHolder.java` — 实现 `ApplicationContextAware`，提供静态方法获取 Spring Bean
- [X] T023 [P] Create `metaforge-parent/metaforge-framework/src/main/java/com/metaforge/framework/web/RequestUtils.java` — HTTP 请求工具：获取客户端 IP、获取请求体、获取 TraceId
- [X] T024 [P] Create `metaforge-parent/metaforge-framework/src/main/java/com/metaforge/framework/jpa/JpaQueryHelper.java` — JPA 查询辅助：Specification 动态条件构建工具
- [X] T025 [P] Create `metaforge-parent/metaforge-framework/src/main/java/com/metaforge/framework/cache/CacheTemplate.java` — Caffeine 缓存操作模板：`put(bcName, entity, id, value)`、`get(bcName, entity, id)`、`evict(bcName, entity, id)`，Key 格式 `<bc-name>:<entity>:<id>`

**Checkpoint**: `mvn clean install -pl metaforge-common,metaforge-framework` 通过；common 依赖边界验证通过

---

## Phase 3: User Story 1 — 业务 BC 接入基座并启动应用 (Priority: P1) 🎯 MVP

**Goal**: 业务 BC 开发者通过 4 步接入（继承 parent → 依赖 framework → 注册到 boot → 配置应用名）即可获得完整运行时环境，应用启动后日志含 TraceId、虚拟线程生效、REST 响应格式统一

**Independent Test**: 创建 bc-sample 模块，在 boot 中注册，启动应用，访问 `/api/sample/hello` 验证响应含 code/message/data/traceId 四个字段

### metaforge-server 模块（FR-003）

- [X] T026 Create `metaforge-server/pom.xml` — 继承 `metaforge-parent`；依赖 `metaforge-common`、`metaforge-framework`、spring-boot-starter-web、spring-boot-starter-data-jpa、spring-boot-starter-validation、spring-boot-starter-actuator、springdoc-openapi-starter-webmvc-ui、flyway-database-postgresql
- [X] T027 [P] [US1] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/VirtualThreadConfig.java` — `@AutoConfiguration`，注册 `TomcatProtocolHandlerCustomizer` 启用 `Executors.newVirtualThreadPerTaskExecutor()`（FR-006）
- [X] T028 [P] [US1] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/JacksonConfig.java` — `@AutoConfiguration(after=VirtualThreadConfig.class)`，注册 `Jackson2ObjectMapperBuilderCustomizer`：日期格式 `yyyy-MM-dd HH:mm:ss`、时区 `Asia/Shanghai`、`NON_NULL`、禁用 `WRITE_DATES_AS_TIMESTAMPS`、注册 `JavaTimeModule`（FR-012）
- [X] T029 [P] [US1] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/web/TraceIdFilter.java` — `OncePerRequestFilter`，在请求入口生成 32 位 UUID（去掉连字符），写入 MDC `traceId`，设置 `X-Trace-Id` 响应头（FR-008）
- [X] T030 [P] [US1] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/web/GlobalResponseBodyAdvice.java` — `@RestControllerAdvice(basePackages="com.metaforge")` 实现 `ResponseBodyAdvice`，自动将非 `ApiResponse` 类型返回值包装为 `ApiResponse.success()`（FR-007）
- [X] T031 [P] [US1] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/web/GlobalExceptionHandler.java` — `@RestControllerAdvice`，处理 `MethodArgumentNotValidException`(400→20001)、`HttpMessageNotReadableException`(400→20003)、`BaseException`（按 code 映射 HTTP 状态码）、`Exception`(500→10000)（FR-009）
- [X] T032 [P] [US1] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/WebMvcConfig.java` — `@AutoConfiguration(after={VirtualThreadConfig.class,JacksonConfig.class})` 实现 `WebMvcConfigurer`，注册 `TraceIdFilter`、配置 CORS（FR-017）
- [X] T033 [P] [US1] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/LogConfig.java` — 日志脱敏规则：内置 LogMaskSpi 默认实现处理 password/secret/token/phone/email/idCard 字段（FR-010）
- [X] T033a [P] [US1] Create `metaforge-parent/metaforge-boot/src/main/resources/logback-spring.xml` — 标准化日志格式：`[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] [%level] [%X{traceId}] [%logger{36}] - %msg%n`
- [X] T034 [US1] Create `metaforge-parent/metaforge-server/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — 注册阶段3的AutoConfiguration类：VirtualThreadConfig、JacksonConfig、TraceIdFilter、GlobalResponseBodyAdvice、GlobalExceptionHandler、WebMvcConfig、LogConfig。后续阶段中新增的配置类（CacheConfig/T058、OpenApiConfig/T060、DataSourceConfig/T062、FlywayConfig/T063、I18nConfig/T066、ActuatorConfig/T068、SecurityConfig/T070、SchemaGuardConfig/T072、SpiRegistry/T075）各自在此文件末尾追加一行

### metaforge-boot 模块（FR-004）

- [X] T035 Create `metaforge-boot/pom.xml` — 继承 `metaforge-parent`；依赖 `metaforge-server`（平台级能力）；注册 `bc-sample` 为 `<dependency>`（FR-004、FR-005）
- [X] T036 [US1] Create `metaforge-parent/metaforge-boot/src/main/java/com/metaforge/MetaforgeApplication.java` — `@SpringBootApplication` + `@ComponentScan("com.metaforge")` 统一扫描根包（FR-005b）
- [X] T037 [US1] Create `metaforge-parent/metaforge-boot/src/main/resources/application.yml` — 完整配置：数据源(postgresql://localhost:5432/metaforge)、HikariCP、Flyway、Caffeine 缓存(expireAfterWrite=30m,maximumSize=1000)、Jackson(date-format=yyyy-MM-dd HH:mm:ss,NON_NULL)、虚拟线程(enabled=true)、国际化(basename=i18n/messages)、Actuator(health,info,metrics)、SpringDoc(tags-sorter=alpha)、安全基线(request-size=10MB)（FR-005c）
- [X] T038 [US1] Create `metaforge-parent/metaforge-boot/src/main/resources/i18n/messages_zh_CN.properties` — 中文消息：system.error=系统错误、validation.error=参数校验失败、resource.not_found=资源不存在
- [X] T039 [US1] Create `metaforge-parent/metaforge-boot/src/main/resources/i18n/messages_en_US.properties` — 英文消息：system.error=System Error、validation.error=Validation Failed、resource.not_found=Resource Not Found

### bc-sample 模块（FR-005d）

- [X] T040 Create `bc-sample/pom.xml` — 继承 `metaforge-parent`；依赖 `metaforge-framework`（不声明版本号，不声明 `<dependencyManagement>`）作为 BC 接入标准模板
- [X] T041 [P] [US1] Create `metaforge-parent/bc-sample/src/main/java/com/metaforge/sample/model/SampleEntity.java` — JPA Entity：`@Entity`、`@Table`、含 id(Long, auto)、name(String)、createdTime(LocalDateTime)、updatedTime(LocalDateTime)
- [X] T042 [P] [US1] Create `metaforge-parent/bc-sample/src/main/java/com/metaforge/sample/repository/SampleRepository.java` — `@Repository` 接口继承 `JpaRepository<SampleEntity, Long>`
- [X] T043 [US1] Create `metaforge-parent/bc-sample/src/main/java/com/metaforge/sample/service/SampleService.java` — `@Service`，演示 CacheManager 注入与缓存读写、抛出自定义异常触发全局异常处理
- [X] T044 [US1] Create `metaforge-parent/bc-sample/src/main/java/com/metaforge/sample/controller/SampleController.java` — `@RestController` + `@Tag(name="bc-sample")`，提供端点：`GET /api/sample/hello`(验证统一响应)、`POST /api/sample/validate`(验证参数校验，使用 `@Valid @NotNull`)、`GET /api/sample/error-test`(验证全局异常)、`POST /api/sample/cache-test`(验证缓存读写)
- [X] T045 [US1] Add `bc-sample` to `metaforge-parent/pom.xml` `<modules>` list

### Flyway 迁移脚本（FR-020a）

- [X] T046 [P] [US1] Create `metaforge-parent/metaforge-boot/src/main/resources/db/migration/V1__bc_sample_ddl.sql` — bc-sample 建表：`CREATE TABLE IF NOT EXISTS bc_sample.sample_entity (id BIGSERIAL PRIMARY KEY, name VARCHAR(255), created_time TIMESTAMP, updated_time TIMESTAMP)`；`CREATE SCHEMA IF NOT EXISTS bc_sample`
- [X] T047 [P] [US1] Create `metaforge-parent/metaforge-boot/src/main/resources/db/migration/V2__bc_sample_init.sql` — bc-sample 初始化数据：`INSERT INTO bc_sample.sample_entity (name, created_time, updated_time) VALUES ('Hello MetaForge', NOW(), NOW())`

### 集成验证（启动 + 全链路）

- [X] T048 [US1] Verify `mvn clean install -pl metaforge-boot -am` 全量构建通过
- [X] T049 [US1] Verify `mvn spring-boot:run -pl metaforge-boot` 启动成功，控制台日志含 TraceId、虚拟线程标识、Flyway 迁移已执行
- [X] T050 [US1] Verify endpoints: `curl http://localhost:8080/api/sample/hello` 返回 `{"code":200,"message":"success","data":...,"traceId":"32位hex"}` 且响应头含 `X-Trace-Id`
- [X] T050a [P] [US1] SC-003 TraceId 全覆盖验证: 执行 `curl /api/sample/hello`，从应用日志中 grep 该请求的 traceId，确认在请求入口（TraceIdFilter）、业务处理（SampleService）、响应出口（GlobalResponseBodyAdvice）三级日志中均出现该 traceId，无日志行缺漏
- [X] T051 [US1] Verify `/actuator/health` 返回 `{"status":"UP"}`；Swagger UI `/swagger-ui.html` 可访问，API 按 Tag `bc-sample` 分组

**Checkpoint**: User Story 1 完整可验证 — 应用启动、统一响应、TraceId、健康检查全部就绪

---

## Phase 4: User Story 2 — 平台架构师统一管理全平台依赖版本 (Priority: P1)

**Goal**: metaforge-parent BOM 作为全平台依赖版本的唯一权威源，业务 BC 不声明版本号即可使用统一版本，Enforcer 插件阻止版本覆盖和违规依赖

**Independent Test**: bc-sample 依赖 spring-boot-starter-web 不写 version，`mvn dependency:tree` 确认版本来自 BOM；在 bc-sample 的 `<properties>` 中尝试覆盖版本，`mvn validate` 构建失败

### BOM 治理强化

- [X] T052 [US2] Verify `metaforge-parent/pom.xml` — `<dependencyManagement>` 中已声明全量核心依赖版本：spring-boot-dependencies、spring-ai-bom、postgresql、jackson-bom、caffeine、flyway-database-postgresql、testcontainers-bom、springdoc-openapi、hibernate-core（FR-001）
- [X] T053 [P] [US2] In `metaforge-parent/pom.xml` — 为 maven-enforcer-plugin 添加自定义规则：`<requireProperty>` 阻止 BC 覆盖 `spring-boot.version`、`postgresql.version`、`jackson.version`、`caffeine.version`、`hibernate.version`、`flyway.version`、`testcontainers.version` 属性
- [X] T054 [P] [US2] In `metaforge-parent/pom.xml` — 为 maven-enforcer-plugin 添加自定义禁止规则：扫描所有非 boot 模块的依赖树，若发现依赖 `com.metaforge:metaforge-boot` 则构建失败（FR-004、FR-005）
- [X] T055 [US2] Verify: 在 bc-sample POM 的 `<properties>` 中临时添加 `<spring-boot.version>3.4.0</spring-boot.version>`，执行 `mvn validate -pl bc-sample` 确认构建失败并输出违规信息
- [X] T056 [US2] Verify: 在 bc-sample POM 中临时添加对 `com.metaforge:metaforge-boot` 的依赖声明，执行 `mvn validate -pl bc-sample` 确认构建失败
- [X] T057 [US2] Verify `mvn dependency:tree -pl bc-sample` 输出中 spring-boot-starter-web 等版本均来自 BOM 管理，无版本冲突警告

**Checkpoint**: BOM 版本治理生效，Enforcer 阻止版本覆盖和违规依赖

---

## Phase 5: User Story 3 — 业务 BC 开发者使用横切技术能力 (Priority: P2)

**Goal**: 缓存、JSONB 序列化、参数校验、OpenAPI 文档、i18n、可观测性、安全基线等横切能力开箱即用，无需业务 BC 手动配置

**Independent Test**: 在 bc-sample 中注入 CacheManager 验证缓存读写、注入 ObjectMapper 验证序列化、带 `@Valid` 的 Controller 验证校验异常、访问 Swagger UI 和 /actuator/metrics

### Caffeine 缓存（FR-011）

- [X] T058 [P] [US3] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/CacheConfig.java` — `@AutoConfiguration`，注册 `CaffeineCacheManager` Bean：`expireAfterWrite=30m`、`maximumSize=1000`、`recordStats=true`；在 `AutoConfiguration.imports` 末尾追加一行 `com.metaforge.server.config.CacheConfig`
- [X] T059 [US3] Update `bc-sample/src/main/java/.../service/SampleService.java` — 注入 `CacheManager`，实现 `GET /api/sample/cache-test/{key}` 读缓存、`POST /api/sample/cache-test` 写缓存，Key 格式 `bc-sample:cache:{key}`

### OpenAPI 文档（FR-014）

- [X] T060 [P] [US3] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/OpenApiConfig.java` — `@AutoConfiguration`，注册 `OpenAPI` Bean（设置 title/version/description），配置 `GroupedOpenApi`("all"，paths="/**")；在 `AutoConfiguration.imports` 末尾追加一行 `com.metaforge.server.config.OpenApiConfig`
- [X] T061 [US3] Verify Swagger UI: 启动应用后访问 `http://localhost:8080/swagger-ui.html`，bc-sample 的端点按 `bc-sample` Tag 分组展示

### 数据源 + 事务（FR-018）

- [X] T062 [P] [US3] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/DataSourceConfig.java` — `@AutoConfiguration`，注册 HikariCP DataSource（通过 `application.yml` 的 `spring.datasource.*` 属性自动配置），`@EnableTransactionManagement`；在 `AutoConfiguration.imports` 末尾追加一行 `com.metaforge.server.config.DataSourceConfig`

### Flyway 数据库迁移（FR-020a）

- [X] T063 [P] [US3] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/FlywayConfig.java` — `@AutoConfiguration`，Spring Boot 默认已自动配置 Flyway，本类可选（保证显式声明顺序）；在 `AutoConfiguration.imports` 末尾追加一行 `com.metaforge.server.config.FlywayConfig`。确认 `spring.flyway.enabled=true` 且 `locations=classpath:db/migration`

### 参数校验（FR-013）

- [X] T064 [P] [US3] Verify 参数校验已生效（spring-boot-starter-validation 已由 metaforge-server 引入）：在 `SampleController` 中已有 `@Valid` + `@NotNull` 注解端点 `POST /api/sample/validate`
- [X] T065 [US3] Create exception handler in `GlobalExceptionHandler.java` — 处理 `MethodArgumentNotValidException` 和 `ConstraintViolationException`，返回统一格式错误响应（code=20001, message 包含字段级校验失败详情）

### i18n 国际化（FR-015）

- [X] T066 [P] [US3] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/I18nConfig.java` — `@AutoConfiguration`，注册 `LocaleChangeInterceptor`（解析 `Accept-Language` 请求头），注册 `LocaleResolver`（默认 `zh-CN`）；在 `AutoConfiguration.imports` 末尾追加一行 `com.metaforge.server.config.I18nConfig`
- [X] T067 [US3] Verify i18n: 启动应用后 `curl -H "Accept-Language: zh-CN" http://localhost:8080/api/sample/hello` 返回中文消息，`-H "Accept-Language: en-US"` 返回英文消息

### 可观测性（FR-016）

- [X] T068 [P] [US3] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/ActuatorConfig.java` — `@AutoConfiguration`，确认 Actuator 端点暴露配置（health/info/metrics 已在 application.yml 配置）；在 `AutoConfiguration.imports` 末尾追加一行 `com.metaforge.server.config.ActuatorConfig`
- [X] T069 [US3] Verify: 访问 `/actuator/health`(UP)、`/actuator/metrics`(含 jvm.* 和 http.server.requests)、`/actuator/info`

### 安全基线（FR-017）

- [X] T070 [P] [US3] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/SecurityConfig.java` — `@AutoConfiguration`，实现 `FilterRegistrationBean` 注册 XSS 过滤 Servlet Filter，在 WebMvcConfig 中配置 CORS，在 `application.yml` 中设置请求体大小限制（`spring.servlet.multipart.max-request-size=10MB`）；在 `AutoConfiguration.imports` 末尾追加一行 `com.metaforge.server.config.SecurityConfig`
- [X] T071 [US3] Verify XSS 防护：发送含 `<script>` 标签的 JSON 请求到 `POST /api/sample/echo`（如已实现），确认响应中转义或过滤

### 跨 Schema 校验（FR-020）

- [X] T072 [P] [US3] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/config/SchemaGuardConfig.java` — `@AutoConfiguration`，提供 `SchemaGuard` Bean，支持声明 BC 拥有的 Schema 白名单（FR-020）；在 `AutoConfiguration.imports` 末尾追加一行 `com.metaforge.server.config.SchemaGuardConfig`
- [X] T073 [US3] Add `com.metaforge.server.config.SchemaGuard` utility class — 提供 `checkWritePermission(bcName, schemaName)` 方法，校验 BC 是否可写目标 Schema

### 跨能力集成验证

- [X] T074 [US3] Verify 全量横切能力：CacheManager 注入可用、ObjectMapper 序列化符合规范、参数校验异常返回统一格式、Swagger UI 可访问、i18n 中英文切换正常、Actuator 端点可用

**Checkpoint**: 全部横切能力在 bc-sample 中验证通过，开箱即用

---

## Phase 6: User Story 4 — 业务 BC 开发者通过 SPI 扩展点定制基座行为 (Priority: P2)

**Goal**: 六大 SPI 扩展点注册与生命周期管理就绪，业务 BC 可实现扩展并自动生效，扩展仅对声明 BC 模块内生效

**Independent Test**: 在 bc-sample 中实现 ExceptionHandlerSpi 自定义异常处理，验证自定义异常被正确拦截，且其他 BC 不受影响

### SPI 注册机制（FR-021）

- [X] T075 [US4] Create `metaforge-parent/metaforge-server/src/main/java/com/metaforge/server/spi/SpiRegistry.java` — `@AutoConfiguration`，自动扫描所有 SPI 接口实现 Bean，按 `@Order` 排序，提供各 SPI 类型的调用门面方法：`handleException(Exception)` → 链式调用 ExceptionHandlerSpi 实现列表、`preHandle(request, response)` → 链式调用 RequestInterceptorSpi；在 `AutoConfiguration.imports` 末尾追加一行 `com.metaforge.server.spi.SpiRegistry`
- [X] T076 [US4] In `SpiRegistry.java` — 实现 ExceptionHandlerSpi 调用：按 `@Order` 排序遍历处理器列表，首个返回非 null 结果即时短路；所有处理器返回 null 则使用默认兜底处理器
- [X] T077 [US4] In `SpiRegistry.java` — 实现 RequestInterceptorSpi 调用：preHandle 任意返回 false 则中断请求链
- [X] T078 [US4] In `SpiRegistry.java` — 实现 LogMaskSpi 聚合调用：遍历所有脱敏处理器，对每个日志字段依次应用所有 mask 规则
- [X] T079 [US4] In `SpiRegistry.java` — 实现 HealthCheckSpi 聚合调用：收集所有健康检查结果，注入到 Actuator HealthContributor

### SPI BC 级隔离保证（FR-022）

- [X] T080 [US4] In `SpiRegistry.java` — 每个 SPI 扩展在注册时记录其来源模块信息，确保跨 BC 隔离：扩展列表按 `@ComponentScan` 范围过滤，A BC 的 SPI 扩展不可被 B BC 的请求触发
- [X] T081 [US4] Update `GlobalExceptionHandler.java` — 在异常处理流程中调用 `SpiRegistry.handleException()`，基座默认处理器作为 fallback

### bc-sample SPI 扩展示例（FR-005d 验证用）

- [X] T082 [US4] Create `metaforge-parent/bc-sample/src/main/java/com/metaforge/sample/spi/SampleExceptionHandler.java` — 实现 `ExceptionHandlerSpi`，`@Component` + `@Order(100)`，处理 `IllegalArgumentException` 返回 code=30101 的 ApiResponse
- [X] T083 [US4] Create `metaforge-parent/bc-sample/src/main/java/com/metaforge/sample/spi/SampleHealthCheck.java` — 实现 `HealthCheckSpi`，`@Component`，返回 bc-sample 健康状态
- [X] T084 [US4] Verify: 访问 `/api/sample/error-test`（由 SampleController 抛出 IllegalArgumentException），预期返回 `{"code":30101,"message":"BC 自定义: ...","data":null,"traceId":"..."}`
- [X] T085 [US4] Verify: `/actuator/health` 返回的 health details 中包含 bc-sample 的健康检查结果

**Checkpoint**: SPI 扩展机制就绪，BC 级隔离验证通过

---

## Phase 7: User Story 5 — 业务 BC 开发者使用测试基座编写测试 (Priority: P3)

**Goal**: BaseUnitTest 和 BaseIntegrationTest（含 TestContainers）提供标准化测试基座，业务 BC 继承后可编写单元测试和集成测试

**Independent Test**: 在 bc-sample 中创建测试类继承 BaseUnitTest 和 BaseIntegrationTest，分别运行验证执行速度和 TestContainers 自动启停

### BaseUnitTest（FR-028）

- [X] T086 [US5] Create `metaforge-parent/metaforge-framework/src/main/java/com/metaforge/framework/test/BaseUnitTest.java` — `@ExtendWith(MockitoExtension.class)`，不加载 Spring 上下文，提供 Mockito 支持
- [X] T087 [US5] Create `metaforge-parent/bc-sample/src/test/java/com/metaforge/sample/service/SampleServiceUnitTest.java` — 继承 `BaseUnitTest`，使用 `@Mock` 模拟 Repository，测试 Service 方法的业务逻辑（验证执行时间 < 100ms）

### BaseIntegrationTest（FR-029）

- [X] T088 [US5] Add TestContainers dependency to `metaforge-framework/pom.xml` — testcontainers-bom、testcontainers-junit-jupiter、testcontainers-postgresql（test scope）
- [X] T089 [US5] Create `metaforge-parent/metaforge-framework/src/main/java/com/metaforge/framework/test/BaseIntegrationTest.java` — `@Testcontainers` + `@SpringBootTest(webEnvironment=RANDOM_PORT)`，`@Container @ServiceConnection static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")`
- [X] T090 [US5] Create `metaforge-parent/metaforge-framework/src/test/resources/application-test.yml` — 测试环境配置：test profile 数据源（由 `@ServiceConnection` 自动覆盖）、禁用 Flyway（可选）、log level=DEBUG
- [X] T091 [US5] Create `metaforge-parent/bc-sample/src/test/java/com/metaforge/sample/controller/SampleControllerIntegrationTest.java` — 继承 `BaseIntegrationTest`，使用 `TestRestTemplate` 测试 `/api/sample/hello` 端点，验证响应结构（code/message/data/traceId）正确
- [X] T092 [US5] Verify BaseUnitTest: `mvn test -pl bc-sample` 通过，单测试方法耗时 < 100ms（SC-006）
- [X] T093 [US5] Verify BaseIntegrationTest: `mvn test -pl bc-sample -Dtest="*IntegrationTest"` 通过，TestContainers 自动启动 PostgreSQL，测试结束后容器自动销毁

**Checkpoint**: 测试基座就绪，单元测试和集成测试均验证通过

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 端到端验证、文档完善、额外质量保障

- [X] T094 [P] Run `mvn clean install -pl metaforge-boot -am` — 全量构建 + Enforcer 校验确认通过
- [X] T095 [P] Verify common layer dependency boundary: `mvn dependency:list -pl metaforge-common -DincludeScope=compile` 输出仅含 Jackson + SLF4J，无 Spring/JPA 等框架依赖（SC-005）
- [X] T096 Run quickstart.md validation scenario: 按 `quickstart.md` 中的所有验证步骤逐项执行确认全部通过
- [X] T098 [P] Verify BC 4-step access: 在 `metaforge-parent/` 根目录创建临时 BC 模块 `bc-test`，按 4 步接入流程（parent→framework依赖→boot注册→配置），验证 SC-001（纯操作时间 < 5 分钟，不含依赖下载）
- [X] T099 Verify all error paths: 未配置数据源时启动失败（清晰的错误提示）、端口被占用时启动失败（清晰的端口冲突信息）

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational — common (Phase 2.A)**: Depends on Setup completion (parent POM for version inheritance) — BLOCKS all user stories
- **Foundational — framework (Phase 2.B)**: Depends on common (T006-T019) — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational (T006-T025). server depends on common+framework; boot depends on server; bc-sample depends on framework
- **US2 (Phase 4)**: Depends on Setup (T004 for Enforcer plugin). Can run in parallel with US1
- **US3 (Phase 5)**: Depends on US1 server config baseline. Most tasks are additive (new AutoConfiguration classes)
- **US4 (Phase 6)**: Depends on US1 (SpiRegistry uses GlobalExceptionHandler). SPI interfaces defined in common are available from Phase 2
- **US5 (Phase 7)**: Depends on US1 (bc-sample must be ready for test execution)
- **Polish (Phase 8)**: Depends on all desired user stories complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational. No dependency on US2-US5. Core runtime base
- **US2 (P1)**: Can start after Setup. Runs parallel with US1. BOM governance is independent of runtime
- **US3 (P2)**: Depends on US1 runtime base (server + boot + bc-sample exist). Adds individual capability configs
- **US4 (P2)**: Depends on US1 (global exception handler must exist). SPI mechanism builds on top of server
- **US5 (P3)**: Depends on US1 (bc-sample module with endpoints). Test base classes need a target module

### Within Each User Story

- server module → boot module → bc-sample module (because boot depends on server, bc-sample depends on framework)
- AutoConfiguration classes within a phase marked [P] can run in parallel
- Configs before integration tests
- Flyway SQL files are independent [P]

### Parallel Opportunities

- **Phase 1**: T002, T003, T004 can run in parallel (different files)
- **Phase 2**: T007-T018 (common layer DTOs/SPI/utils) can all run in parallel; T021-T025 (framework layer) can run in parallel
- **Phase 3**: T027-T034 (all server AutoConfiguration + imports file) can run in parallel; T041-T042 (bc-sample model/repo) can run in parallel; T046-T047 (Flyway scripts) can run in parallel
- **Phase 4**: T053-T054 (Enforcer rules) can run in parallel
- **Phase 5**: T058, T060, T062, T063, T066, T068, T070, T072 (all server configs) can run in parallel
- **Phase 7**: T086, T088-T089 (base classes) can run in parallel
- **Phase 8**: T094, T095 can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all server AutoConfiguration classes in parallel:
Task: "T027 VirtualThreadConfig.java"
Task: "T028 JacksonConfig.java"
Task: "T029 TraceIdFilter.java"
Task: "T030 GlobalResponseBodyAdvice.java"
Task: "T031 GlobalExceptionHandler.java"
Task: "T032 WebMvcConfig.java"
Task: "T033 LogConfig.java"
Task: "T033a logback-spring.xml"

# After server configs done, launch boot + bc-sample in sequence:
Task: "T035-T036 boot module (pom + Application)"
Task: "T037-T039 boot resources (application.yml + i18n)"

# Then bc-sample in parallel:
Task: "T041 SampleEntity.java"
Task: "T042 SampleRepository.java"
Task: "T046 V1__bc_sample_ddl.sql"
Task: "T047 V2__bc_sample_init.sql"
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Complete Phase 1: Setup (T001-T005)
2. Complete Phase 2: Foundational — common + framework (T006-T025)
3. Complete Phase 3: US1 — 运行时基座 + bc-sample (T026-T051)
4. Complete Phase 4: US2 — BOM 依赖治理 (T052-T057)
5. **STOP and VALIDATE**: Start application, verify all endpoints, verify Enforcer rules
6. Deploy/demo: docker compose up → mvn spring-boot:run → full validation

### Incremental Delivery

1. Setup + Foundational → Common tools library ready (T001-T025)
2. + US1 → Runtime base ready, bc-sample running (T026-T051) **← MVP Ship Decision**
3. + US2 → BOM governance enforced (T052-T057)
4. + US3 → Cross-cutting capabilities validated (T058-T074)
5. + US4 → SPI mechanism operational (T075-T085)
6. + US5 → Test infrastructure ready (T086-T093)
7. + Polish → Production readiness (T094-T099)

### Parallel Team Strategy

With 2 developers:
1. Team completes Setup + Foundational together (Phase 1-2)
2. Once Foundational done:
   - Developer A: US1 runtime base (Phase 3 — T026-T051)
   - Developer B: US2 BOM governance (Phase 4 — T052-T057)
3. After US1 done:
   - Developer A: US3 capabilities (Phase 5)
   - Developer B: US4 SPI mechanism (Phase 6)
4. After US1 done, either:
   - Developer A continues US5 testing (Phase 7)
5. Polish together (T094-T096, T098-T099)

---

## Notes

- [P] tasks = different files, no dependencies — can be submitted simultaneously
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- MVP scope = US1 only (4-step BC access + runtime startup). US1 + US2 delivers full foundation-core MVP
- Common layer SC-005 validation (T019) is a gate — must pass before proceeding to US1
- All configuration uses Spring Boot native property names, no `metaforge.*` namespace
- BC modules: no `<dependencyManagement>`, no version property overrides, no `metaforge-boot` dependency
