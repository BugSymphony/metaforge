# Feature Specification: foundation-core 基座初始化

**Feature Branch**: `001-foundation-core-init`

**Created**: 2026-07-19

**Status**: Draft

**Input**: User description: "MetaForge 全局通用域技术型 BC 基座初始化 —— 基于 Java 21 + Spring Boot 3 + Maven 多模块分层架构（metaforge-parent → metaforge-common → metaforge-framework → metaforge-server → metaforge-boot → 业务 BC），构建全平台统一开发脚手架与运行时基座，提供依赖版本治理、运行时环境标准化、横切技术能力、SPI 扩展机制、业务 BC 接入规范与测试基座能力。"

## Clarifications

### Session 2026-07-20

- Q: 启动入口与 BC 聚合机制 — metaforge-boot 作为唯一启动入口，BC 是否需要依赖 metaforge-server？ → A: metaforge-boot 是唯一启动入口，静态声明所有 BC 为依赖；metaforge-server 是平台级能力由 boot 直接引入；BC 不依赖 metaforge-server，通过 metaforge-boot 聚合所有 BC 获得完整运行时环境（boot → BC → server → common 链不成立，改为 boot → server + 全部 BC，BC 仅声明最小化框架依赖）。
- Q: BC 模块的框架依赖来源（BC 不依赖 metaforge-server 时如何获得 Spring 等框架类型）？ → A: BC 模块自己声明最小化框架依赖（如 spring-boot-starter-web），版本由 metaforge-parent BOM 统一管控，不依赖 metaforge-server。
- Q: BC 模块在源码树中的物理位置？ → A: BC 作为 metaforge-parent 的子模块，与 metaforge-common/metaforge-server/metaforge-boot 并列，标准 Maven 多模块 monorepo。
- Q: 多 BC 组件扫描策略？ → A: metaforge-boot 统一扫描根包 com.metaforge，所有 BC 按约定使用 com.metaforge.<bc-name> 子包，新增 BC 无需修改扫描配置。
- Q: 多 BC 配置管理策略？ → A: 所有配置集中在 metaforge-boot 模块的 resources 下，支持两种组织方式：单一 application.yml（通过 metaforge.bc.<bc-name> 命名空间前缀隔离）或按 BC 拆分 application-<bc-name>.yml 统一引入。BC 模块自身不携带运行时配置文件。
- Q: foundation-core 作为平台级能力提供方，需要导出哪些契约？ → A: 全面契约（C 级）：(1) SPI 扩展点接口契约（6 个扩展点）；(2) 统一 REST 响应体格式契约 + 错误码分类目录 + 配置属性完整 schema + 公共 DTO 基类契约；(3) 库级 API 契约（CacheManager 用法、ObjectMapper JSONB 序列化规范、CTE 查询模板接口、分页查询封装）+ 运行时行为规范（TraceId 格式、日志格式规范）。
- Q: 构建系统集成契约应以何种形式提供（Maven POM 模板、模块注册规则、依赖声明规则）？ → A: 在 spec 开发目录 `specs/<feature_dir>/contracts/` 下新增独立构建系统集成契约文档（Markdown + 嵌入式 Maven XML 模板），由专用 FR 强制要求产出，后续可通过 `speckit.contract.export` 导出到 `context/contracts/`。
- Q: BC 模块 Maven artifactId 是否需要强制命名约定（如 metaforge-bc-<name> 前缀）？ → A: 不强制，BC 模块自行决定 artifactId 命名，不做统一前缀/后缀约束。
- Q: BC 模块在 monorepo 中的物理目录位置？ → A: 直接平铺在 `metaforge-parent/` 根目录下，与 `metaforge-common`、`metaforge-server`、`metaforge-boot` 并列，不放入集中子目录。
- Q: BC 模块在 metaforge-boot pom.xml 中注册为 `<dependency>` 时使用什么 Maven scope？ → A: 默认 `compile` scope（Maven 默认值，无需显式声明），BC 参与编译期类型检查并打包到最终部署产物。
- Q: BC 模块 POM 中对 `<dependencyManagement>` 和 `<properties>` 应如何约束？ → A: 禁止 BC 模块声明 `<dependencyManagement>`；`<properties>` 仅允许声明与框架版本无关的自定义属性（如构建参数），禁止覆盖父 POM 已定义的版本属性。
- Q: 配置项命名空间策略——是否使用自定义 `metaforge.*` 前缀包装所有配置，还是保留 Spring Boot 原生配置键名？ → A: 数据库、日志、应用等基础设施配置保留 Spring Boot 原生键名（如 `spring.datasource.*`、`logging.*`），不新增 `metaforge.*` 包装层。横切技术能力为平台架构级别，无需开关配置，全部默认生效。
- Q: 横切技术能力是否需要独立开关配置？ → A: 不需要。横切技术能力（缓存、序列化、校验、文档、国际化、可观测性、安全基线）为 metaforge 平台通用架构级别能力，BC 开发者使用时无需关心启停，全部默认生效，不提供 `metaforge.foundation.<capability>.enabled` 开关。
- Q: 契约文档的生成目录与导出目录如何区分？ → A: 契约在 spec 开发阶段存放于 `specs/<feature_dir>/contracts/`（如 `specs/001-foundation-core-init/contracts/`），此为契约的首要产出目录；`context/contracts/` 由 `speckit.contract.export` 命令导出生成，计划和生成阶段不得直接写入 `context/contracts/`。
- Q: Docker 开发环境 — foundation-core 是否需要提供 docker-compose.yml 预制文件作为本地开发环境标准启动方式？ → A: 提供 docker-compose.yml（含 PostgreSQL），存放于 `metaforge-parent/` 根目录下，作为开发环境标准启动方式。
- Q: Maven archetype（BC 项目脚手架生成器）是否在本 feature 范围内？ → A: 不在本 feature 范围，推迟到后续 feature（如 `002-bc-scaffold`），本 feature 仅产出手动创建 BC 模块的文档步骤。
- Q: MVP/首年预期 BC 模块数量规模？ → A: 3-5 个
- Q: Redis 或分布式缓存是否在 MVP 范围？ → A: 不在范围，MVP 仅 Caffeine 本地缓存，docker-compose 不包含 Redis
- Q: 数据库 Schema 迁移策略？ → A: foundation-core 强制统一 Flyway，集中管理所有 BC 迁移脚本

### Session 2026-07-23

- Q: metaforge-common 依赖边界——允许引入哪些轻量级依赖？ → A: common 层允许 Jackson（core + databind + annotations）作为 JSON 序列化基础设施和 SLF4J API 作为日志门面；严禁引入 Spring Framework、Servlet API、JDBC、JPA/Hibernate、Caffeine、Flyway、RabbitMQ/Kafka 等容器/框架级依赖。Jackson 和 SLF4J API 均为纯 API 级依赖，无容器耦合，确保 common 可在非 Spring 环境复用。
- Q: 是否新增中间模块承载第三方框架通用工具/支持类？ → A: 新增 `metaforge-framework` 模块（位于 common 和 server 之间），封装 Spring 工具、JPA/Hibernate 查询辅助、Web 工具、缓存抽象模板、测试基类等框架感知工具。该模块允许依赖 Spring Framework、JPA/Hibernate、Servlet API、Caffeine 等框架库，仅提供工具封装，不包含自动装配逻辑（AutoConfiguration 由 metaforge-server 负责）。
- Q: 递归 CTE 图查询模板 / jOOQ 是否在本 feature 范围？ → A: 不在。递归 CTE 图遍历查询模板工具和 jOOQ 集成推迟到未来图查询/推理 BC 实现，foundation-core 不包含。
- Q: 测试基类（BaseUnitTest / BaseIntegrationTest）位于哪个模块？ → A: 由 `metaforge-framework` 模块提供，供业务 BC 继承使用。
- Q: 递归 CTE 图查询框架选型？ → A: 倾向于 jOOQ 作为 JPA 补充（类型安全 WITH RECURSIVE 支持），但该集成推迟到未来图查询/推理 BC。
- Q: 横切技术能力是否需要配置开关？ → A: 已确认不需要。所有横切能力默认生效，不提供 `metaforge.foundation.<capability>.enabled` 开关。
- Q: 分页封装——common 是否需要独立分页 DTO 而非仅依赖 Spring Data Pageable？ → A: `metaforge-common` 定义纯 Java 分页 DTO（`PageRequest`、`PageResult<T>`）和内存分页工具 `PageUtils`，不依赖 Spring Data。`metaforge-framework` 提供 `PageHelper` 转换工具（common DTO ↔ Spring `Pageable`/`Page`）。BC 编码层面使用 common DTO 解耦框架，内部可继续用 Spring Data JPA 分页。
- Q: 契约文档如何组织，避免过于分散？ → A: 精简为 6 份契约文档：`api-contracts.md`（SPI+DTO+API+异常+校验+运行时行为，BC 编码消费的一站式手册）、`rest-api-contract.md` + `.yaml`（REST 响应格式+错误码）、`platform-capabilities.md`（平台能力声明清单，逐项告知 BC"已提供勿重复"）、`configuration-schema.md`（配置项参考）、`build-system-integration.md`（构建集成模板）。
- Q: Jackson 全局日期/时间序列化格式？ → A: 统一使用 `yyyy-MM-dd HH:mm:ss`（如 `2026-07-23 14:30:00`），适用于 REST API JSON 响应和 PostgreSQL JSONB 列序列化。
- Q: 缓存 Key 命名约定？ → A: 统一使用 `<bc-name>:<entity>:<id>` 格式（如 `user-bc:user:42`、`agent-bc:mcp:config`），确保多 BC 共享 CacheManager 时 Key 不碰撞。
- Q: OpenAPI 文档分组机制？ → A: BC 在 Controller 类上标注 `@Tag(name = "<bc-name>")`，SpringDoc 自动按 Tag 分组显示 Swagger UI。
- Q: Flyway 迁移脚本目录与命名规范？ → A: 扁平目录（不按 BC 子目录隔离），存放于 `metaforge-boot/src/main/resources/db/migration/`。每个 BC 提供两个脚本文件：`V<n>__<bc-name>_ddl.sql`（DDL 建表）和 `V<n+1>__<bc-name>_init.sql`（初始化数据），按 BC 名称前缀区分。多 BC 共享同一数据库，Flyway 按版本号全局顺序执行。
- Q: Caffeine 缓存默认 TTL 与最大容量？ → A: 默认 TTL 30 分钟，最大容量 1000 条目的 Caffeine 本地缓存。
- Q: flatten-maven-plugin 是否纳入项目构建管理？ → A: 纳入。在 metaforge-parent 的 POM 中配置 flatten-maven-plugin，统一管理全模块 POM 扁平化输出，确保部署时变量全部解析为实际版本号。
- Q: 业务 BC 的框架依赖来源是否改为直接依赖 metaforge-framework？ → A: 是。业务 BC 直接依赖 metaforge-framework（framework 传递提供 common + Spring/JPA/Web/Cache 工具），无需再单独声明 spring-boot-starter 等框架依赖。BC 的 GAV 注册在 metaforge-boot 的 pom.xml 中。
- Q: metaforge-boot 是否允许被其他模块依赖？ → A: 不允许。metaforge-boot 是唯一运行入口，通过 Maven Enforcer 规则强制禁止任何模块（common/framework/server/BC）声明对 metaforge-boot 的依赖。
- Q: 是否需要 bc-sample 演示模块？ → A: 是。新增 bc-sample 模块，作为 foundation-core 最终产物的集成案例与测试 demo，演示 BC 标准接入方式并验证横切能力均正确生效。

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 业务 BC 开发者引入基座依赖并启动应用 (Priority: P1)

作为业务 BC 开发者，我需要通过极简的依赖引入与配置即可获得完整的运行时环境，无需关心底层技术组件装配细节。引入基座后，应用应能正常启动并在标准化运行时环境中运行，日志输出结构化、TraceId 自动生成、虚拟线程自动启用。

**Why this priority**: 这是 foundation-core 存在的核心价值——让业务 BC 开发者零门槛接入标准化运行时环境。若此场景不成立，基座所有其他能力均无消费方。

**Independent Test**: 创建一个空白业务 BC 模块（继承 metaforge-parent，声明最小化框架依赖如 spring-boot-starter-web），在 metaforge-boot 的 pom.xml 中注册该 BC 为依赖，在 metaforge-boot 的 application.yml 中配置应用名称，执行 `mvn spring-boot:run -pl metaforge-boot`，应用启动成功且日志中包含结构化 TraceId 和虚拟线程标识。

**Acceptance Scenarios**:

1. **Given** 业务 BC 开发者创建了一个空白的 Spring Boot 模块（继承 metaforge-parent，声明 spring-boot-starter-web 等最小化框架依赖），并在 metaforge-boot 的 pom.xml 中注册该 BC 为依赖，在 metaforge-boot 下配置了应用名称与数据源连接信息，
   **When** 启动应用，
   **Then** 应用成功启动，控制台输出含 TraceId 的结构化启动日志，HTTP 请求自动获得 TraceId 并在响应头返回，虚拟线程在 Web 请求处理中生效。

2. **Given** 应用正常运行中，
   **When** 业务 BC 发起一个 REST 请求，
   **Then** 请求被全局异常拦截器保护，正常响应以统一格式（含有 code、message、data、traceId 字段）返回；异常响应同样符合统一格式并包含结构化错误信息。

3. **Given** 应用启动时未配置数据源连接信息，
   **When** 业务 BC 启动应用，
   **Then** 启动过程失败，日志输出清晰的数据源配置缺失提示，启动流程终止。

---

### User Story 2 - 平台架构师统一管理全平台依赖版本 (Priority: P1)

作为平台架构师，我需要在 foundation-core 的父工程中定义并管理全平台所有通用技术依赖的版本，确保所有业务 BC 使用一致的依赖版本，杜绝版本冲突与碎片化。

**Why this priority**: 依赖版本治理是技术基座的核心治理能力，版本不一致会导致运行时冲突、安全漏洞、维护成本飙升。

**Independent Test**: 在 metaforge-parent 的 BOM 中声明 Spring Boot、Spring AI、PostgreSQL 驱动、Jackson、Caffeine 等依赖版本，业务 BC 不声明版本号直接使用，验证编译和运行时使用的是基座 BOM 管理版本。

**Acceptance Scenarios**:

1. **Given** metaforge-parent 的 BOM 中声明了 Spring Boot 3.x 版本，
   **When** 业务 BC 在 pom.xml 中引用 spring-boot-starter-web 但不声明版本号，
   **Then** Maven 自动解析为 BOM 管理的版本，编译通过且无版本冲突警告。

2. **Given** 业务 BC 尝试在自身 pom.xml 中通过 `<properties>` 覆盖 Spring Boot 版本，
   **When** CI 流水线执行依赖校验，
   **Then** Maven Enforcer 插件检测到版本覆盖并导致构建失败，输出违规信息指明禁止覆盖的核心依赖。

3. **Given** 两个业务 BC 分别依赖了不同版本的同一第三方库，
   **When** platform 架构师在 metaforge-parent BOM 中指定该库的统一版本，
   **Then** 两个 BC 的依赖自动收敛到 BOM 版本，`mvn dependency:tree` 显示无版本冲突。

---

### User Story 3 - 业务 BC 开发者使用横切技术能力 (Priority: P2)

作为业务 BC 开发者，我在开发过程中需要使用缓存、JSON 序列化（含 JSONB 支持）、参数校验、国际化、接口文档等通用技术能力，这些能力应开箱即用，无需逐一手动配置。

**Why this priority**: 横切能力是基座"消除重复基础建设"核心价值的体现，但依赖 P1 的基座接入才能生效。

**Independent Test**: 在已接入基座的业务 BC 中，分别注入 CacheManager 并验证缓存读写、注入 ObjectMapper 并验证 JSONB 序列化/反序列化、编写带 @Valid 注解的 Controller 方法并验证校验异常统一响应、访问 Swagger UI 验证接口文档自动生成。

**Acceptance Scenarios**:

1. **Given** 业务 BC 已接入 foundation-core，
   **When** 开发者在 Service 层通过 `@Autowired` 注入 CacheManager 并执行缓存 put/get 操作，
   **Then** 缓存读写成功，默认使用 Caffeine 实现，默认过期时间与容量符合基座配置规范。

2. **Given** 业务 BC 需要将元数据实体 JSON 对象序列化后存储到 PostgreSQL JSONB 列，
   **When** 开发者调用 foundation-core 提供的 JSONB 序列化工具将元数据对象写入数据库，
   **Then** JSON 序列化正确处理 Jackson 日期格式、空值策略，数据库 JSONB 列读写正常。

3. **Given** 业务 BC 的 Controller 方法参数标记了 `@Valid` 和 `@NotNull` 注解，
   **When** 客户端发起请求缺少必填字段，
   **Then** 系统返回统一格式的校验错误响应，包含字段级错误详情，HTTP 状态码为 400。

4. **Given** 业务 BC 的国际化资源文件中定义了中英文消息，
   **When** 客户端请求头 `Accept-Language` 分别设为 `zh-CN` 和 `en-US`，
   **Then** 系统分别返回对应的中文和英文消息内容。

---

### User Story 4 - 业务 BC 开发者通过 SPI 扩展点定制基座行为 (Priority: P2)

作为业务 BC 开发者，我需要在不动基座核心源码的前提下，通过预留的 SPI 扩展点对异常处理、请求拦截、日志脱敏等环节注入定制逻辑，且扩展仅对当前 BC 生效。

**Why this priority**: SPI 是基座"开闭原则"的核心实现机制，P2 优先级因先需验证基座本身稳定运行。

**Independent Test**: 在业务 BC 中实现一个自定义异常处理 SPI 扩展（新增一种业务异常类型映射），验证异常被正确拦截并转换为自定义错误响应格式，且该扩展不影响其他 BC 的异常处理行为。

**Acceptance Scenarios**:

1. **Given** foundation-core 暴露了异常处理扩展点 SPI 接口，
   **When** 业务 BC 开发者实现该 SPI 接口并注册自定义异常处理器（处理一种新的业务异常类型），
   **Then** 当该业务异常被抛出时，自定义处理器生效，返回符合统一响应格式的自定义错误；基础异常类型（如参数校验异常）仍由基座默认处理器处理，不受影响。

2. **Given** 两个不同的业务 BC 分别实现了同一个 SPI 扩展点的不同扩展实现，
   **When** 应用启动，
   **Then** 两个扩展各在其所属 BC 的命名空间内独立生效，互不干扰；基座核心流程不受扩展影响，正常运行。

---

### User Story 5 - 业务 BC 开发者使用测试基座编写测试 (Priority: P3)

作为业务 BC 开发者，我需要使用 foundation-core 提供的标准化测试基类与 TestContainers 支持，快速编写单元测试与集成测试，测试环境与生产环境配置隔离。

**Why this priority**: 测试能力是开发效率保障，但在基座初始构建中优先级低于运行时核心能力。

**Independent Test**: 在业务 BC 中继承 foundation-core 提供的集成测试基类，编写一个涉及 PostgreSQL 的集成测试，验证 TestContainers 自动启动 PostgreSQL 容器、测试数据与生产数据隔离、测试完成后容器自动销毁。

**Acceptance Scenarios**:

1. **Given** 业务 BC 开发者继承 foundation-core 提供的 `BaseIntegrationTest` 基类，
   **When** 运行一个编写了数据库读写操作的集成测试，
   **Then** TestContainers 在测试开始前自动启动 PostgreSQL 容器，测试方法执行 SQL 读写成功，测试结束后容器自动销毁，生产数据库不受任何影响。

2. **Given** 业务 BC 开发者编写单元测试，
   **When** 继承 foundation-core 提供的 `BaseUnitTest` 基类并运行测试，
   **Then** 测试运行时不加载 Spring 上下文，执行速度在毫秒级，Mock 依赖隔离正常。

---

### Edge Cases

- 当 `metaforge-server` 模块被引入但未使用 `metaforge-boot` 作为启动入口时，应用能否独立启动？（已明确：`metaforge-server` 仅为库模块不可独立启动，`metaforge-boot` 是唯一启动入口）
- 当 BC 模块未在 `metaforge-boot` 的 pom.xml 中注册为依赖时，该 BC 的组件能否被扫描装载？（预期：未注册则 BC 代码不在 classpath 中，无法装载——BC 必须显式注册到 boot）
- 当业务 BC 同时引入 `metaforge-server` 的多个不兼容版本时，依赖仲裁机制如何响应？（预期：Maven 最近原则自动选一，但 Enforcer 插件发出冲突警告）
- 当两个 SPI 扩展实现声明相同优先级时，执行顺序如何确定？（预期：按扩展注册顺序执行，日志记录扩展顺序）
- 当 common 层的工具类被非 Spring 环境（如命令行小工具）引入时，功能是否仍然正常？（预期：因 common 层纯 Java 无框架依赖，在非 Spring 环境下完全可用）
- 当应用在启动过程中端口被占用时，虚拟线程优雅停机机制是否生效？（预期：启动失败，输出端口冲突错误信息，不残留僵尸进程）

## Requirements *(mandatory)*

### Functional Requirements

#### 多模块架构

- **FR-001**: 系统 MUST 提供 `metaforge-parent` Maven 父工程（pom 类型），作为全平台 BOM 依赖版本唯一权威源，统一管理 Spring Boot、Spring AI、PostgreSQL 驱动、Jackson、Caffeine、Hibernate、Flyway、JUnit、TestContainers 等核心依赖版本。同时作为 Maven reactor 根模块聚合 `metaforge-common`、`metaforge-framework`、`metaforge-server`、`metaforge-boot`、`bc-sample` 及全部业务 BC 子模块。父 POM MUST 配置 `flatten-maven-plugin`，确保部署时 CI 友好 POM（`.flattened-pom.xml`）中所有 `${revision}` 等变量已解析为实际版本号。
- **FR-002**: 系统 MUST 提供 `metaforge-common` 模块，封装纯 Java 通用基础能力（异常基类体系、全局常量池、通用工具类、SPI 扩展接口定义、通用 DTO 基类），允许引入 Jackson（core + databind + annotations）和 SLF4J API 作为基础序列化和日志门面依赖。严禁引入 Spring Framework、Servlet API、JDBC、JPA/Hibernate、Caffeine、Flyway、RabbitMQ/Kafka 等容器/框架级依赖。
- **FR-002a**: 系统 MUST 提供 `metaforge-framework` 模块，封装第三方框架通用工具与支持类（Spring 应用上下文工具、JPA/Hibernate 查询辅助、Web 请求/响应工具、缓存抽象模板、测试基类 BaseUnitTest/BaseIntegrationTest 等）。该模块允许依赖 Spring Framework、JPA/Hibernate、Servlet API、Caffeine 等框架库，仅提供工具封装与模板，不包含自动装配逻辑（AutoConfiguration 由 metaforge-server 负责）。`metaforge-framework` 依赖 `metaforge-common`。
- **FR-003**: 系统 MUST 提供 `metaforge-server` 模块，集成 Spring Boot 运行时通用自动装配能力（Web 配置、数据源配置、异常治理、序列化、缓存、日志、接口文档、国际化、参数校验、可观测性），依赖 `metaforge-common` 和 `metaforge-framework`，且不得依赖任何业务 BC。`metaforge-server` 为平台级能力模块，仅由 `metaforge-boot` 引入，业务 BC 不直接依赖 `metaforge-server`。
- **FR-004**: 系统 MUST 提供 `metaforge-boot` 启动模块，作为单体应用唯一启动入口。`metaforge-boot` 依赖 `metaforge-server`（平台级能力），并在其 pom.xml 中静态声明全部业务 BC 模块为依赖，负责全模块组件扫描聚合，不包含功能实现逻辑。`metaforge-boot` MUST NOT 被任何其他模块依赖（通过 Maven Enforcer 反向依赖规则强制执行，common/framework/server/BC 依赖 boot 时构建直接失败）。
- **FR-005**: 模块间 MUST 严格遵循单向分层依赖：`metaforge-boot → metaforge-server → metaforge-framework → metaforge-common`，且 `metaforge-boot → 全部 BC 模块`。反向依赖（common/framework/server 依赖 BC、BC 依赖 boot、任意模块依赖 boot）构建时自动失败。业务 BC 直接依赖 `metaforge-framework`（framework 传递提供 common + Spring/JPA/Web/Cache 工具层），不再单独声明 spring-boot-starter 等框架依赖，版本由 metaforge-parent BOM 统一管控。BC 模块不依赖 `metaforge-server`。
- **FR-005a**: 业务 BC 模块 MUST 作为 `metaforge-parent` 的直接子模块，物理目录直接平铺在 `metaforge-parent/` 根目录下，与 `metaforge-common`、`metaforge-framework`、`metaforge-server`、`metaforge-boot`、`bc-sample` 并列，形成标准 Maven 多模块单体仓库（monorepo）。BC 模块的 source package 按 `com.metaforge.<bc-name>` 约定命名。
- **FR-005b**: `metaforge-boot` MUST 通过 `@ComponentScan("com.metaforge")` 或等效机制统一扫描根包 `com.metaforge`，自动发现并装载所有 BC 模块及 foundation-core 的 Spring Bean。新增 BC 无需修改启动类扫描配置。
- **FR-005c**: 全平台运行时配置 MUST 集中在 `metaforge-boot` 模块的 `src/main/resources` 目录下。支持两种配置组织方式：(1) 单一 `application.yml` 集中管理所有配置；(2) 按 BC 拆分 `application-<bc-name>.yml` 文件，由 `application.yml` 通过 `spring.config.import` 统一引入。BC 模块自身不携带运行时配置文件。数据库连接、日志、服务器端口等基础设施配置 MUST 使用 Spring Boot 原生配置键名（如 `spring.datasource.*`、`logging.*`、`server.port`），不得新增 `metaforge.*` 等自定义命名空间包装。
- **FR-005d**: 系统 MUST 提供 `bc-sample` 示例模块，作为 foundation-core 最终产物的集成案例与测试 demo。`bc-sample` 演示业务 BC 标准接入方式：继承 `metaforge-parent` → 依赖 `metaforge-framework` → 在 `metaforge-boot` 的 pom.xml 中注册 GAV。`bc-sample` MUST 提供示例 Controller、Service、Repository，用于验证 foundation-core 横切能力（TraceId 自动注入、统一响应体包装、全局异常处理、缓存读写、参数校验、i18n 国际化、OpenAPI 文档）均正确生效。

#### 运行时基座

- **FR-006**: 系统 MUST 全局启用 Java 21 虚拟线程（Virtual Threads），标准化 Web 请求线程池与异步任务线程池配置，全链路适配虚拟线程编程模型。
- **FR-007**: 系统 MUST 提供统一 REST 响应体自动包装能力，所有成功/失败响应包含标准字段（code、message、data、traceId）。
- **FR-008**: 系统 MUST 提供全链路 TraceId 能力——HTTP 请求入口自动生成，异步线程全场景透传，日志自动打印，响应头自动返回。
- **FR-009**: 系统 MUST 提供全局异常统一拦截机制，异常按层级（系统异常/参数校验异常/扩展异常）分类处理，错误响应含错误码追踪。
- **FR-010**: 系统 MUST 标准化日志输出格式，提供敏感字段自动脱敏规则、日志级别动态调整能力、多环境日志输出策略。

#### 横切能力

- **FR-011**: 系统 MUST 提供 Caffeine 内存缓存抽象能力，统一缓存管理器配置，默认过期策略为 TTL 30 分钟，最大容量 1000 条目。支持业务 BC 通过 CacheManager API 自定义缓存规则。
- **FR-012**: 系统 MUST 提供 Jackson 全局序列化/反序列化能力，统一日期/时间格式为 `yyyy-MM-dd HH:mm:ss`、空值策略为 `NON_NULL`（不序列化 null 字段），提供 PostgreSQL JSONB 专用序列化/反序列化工具。
- **FR-013**: 系统 MUST 集成 JSR-380 参数校验，校验异常统一转换为标准化错误响应，支持自定义校验注解扩展。
- **FR-014**: 系统 MUST 提供 OpenAPI 3.0 自动接口文档生成能力（SpringDoc），统一文档元信息配置。业务 BC 在 Controller 类上通过 `@Tag(name = "<bc-name>")` 声明分组，Swagger UI 自动按 BC 分组展示。
- **FR-015**: 系统 MUST 提供国际化能力，统一国际化资源文件加载规范，请求头语言自动识别，支持业务 BC 扩展自定义国际化资源。
- **FR-016**: 系统 MUST 提供可观测性监控能力——默认接口请求指标、JVM 运行指标、异常计数埋点，标准化健康检查端点，统一指标命名规范。
- **FR-017**: 系统 MUST 提供安全基线能力——请求 XSS/SQL 注入基础防御、请求体大小限制、跨域配置标准化。

#### 数据访问

- **FR-018**: 系统 MUST 提供 PostgreSQL 数据源统一配置（HikariCP 连接池），连接池参数标准化，事务管理器统一配置，`@Transactional` 注解默认生效。
- **FR-019**: 系统 MUST 提供分页能力：`metaforge-common` 中定义纯 Java 分页 DTO——`PageRequest`（page、size、sort 字段）和 `PageResult<T>`（content、total、page、size、totalPages 字段），以及 `PageUtils` 内存级分页工具（支持 `List<T>` 截取，不依赖 Spring Data）；`metaforge-framework` 中提供 `PageHelper` 转换工具（common `PageRequest` ↔ Spring `Pageable`、Spring `Page<T>` ↔ common `PageResult<T>`）。BC 编码使用 common DTO 解耦框架，内部可继续使用 Spring Data JPA 原生分页。递归 CTE 图遍历查询模板工具推迟到未来图查询/推理 BC 实现，不在 foundation-core 范围。
- **FR-020**: 系统 MUST 提供跨 Schema 写操作基础校验能力，强制遵循单一数据所有权原则。
- **FR-020a**: 系统 MUST 提供 Flyway 数据库迁移统一管理能力——所有 BC 的迁移脚本以扁平目录结构集中存放于 `metaforge-boot` 模块的 `src/main/resources/db/migration/` 目录下（不按 BC 子目录隔离，多 BC 共享同一数据库）。每个 BC 提供两个脚本文件：`V<n>__<bc-name>_ddl.sql`（DDL 建表）和 `V<m>__<bc-name>_init.sql`（初始化/种子数据），脚本按 `<bc-name>` 前缀区分。启动时 Flyway 按版本号全局顺序自动执行全量迁移，确保全平台 Schema 版本一致性与变更可追溯。业务 BC 不得独立声明或配置自身的迁移框架。

#### SPI 扩展

- **FR-021**: 系统 MUST 提供标准化 SPI 扩展点注册机制，预留异常处理、请求拦截、日志脱敏、健康检查、序列化、参数校验六大核心扩展点。
- **FR-022**: SPI 扩展实现 MUST 仅在声明 BC 模块内生效，不得泄露到其他 BC 或影响基座全局行为。

#### 业务 BC 接入

- **FR-023**: 业务 BC 接入 foundation-core 需完成 4 步：(1) 继承 `metaforge-parent` 父工程；(2) 在 pom.xml 中声明对 `metaforge-framework` 的依赖（framework 传递提供 common + Spring/JPA/Web/Cache 工具，无需再单独声明 spring-boot-starter 等框架依赖）；(3) 在 `metaforge-boot` 的 pom.xml 中将自身注册为 `<dependency>`；(4) 在 `metaforge-boot` 的 `application.yml`（或 `application-<bc-name>.yml`）中添加 BC 专属配置（如数据源）。
- **FR-024**: 系统 MUST 保证业务 BC 接入时无需修改自身业务代码、无需继承基座基类、无需手动启用组件，全部通用能力由 `metaforge-boot` 启动时通过自动装配统一生效。
- **FR-025**: 横切技术能力（缓存、序列化、参数校验、接口文档、国际化、可观测性、安全基线等）MUST 作为平台级通用架构能力默认生效，无需业务 BC 配置开关。这些能力属于 metaforge 平台架构基线的组成部分，BC 开发者使用时无需关心启停状态。

#### 开发环境

- **FR-026**: 系统 MUST 提供 `docker-compose.yml` 文件，存放于 `metaforge-parent/` 根目录，定义 PostgreSQL 作为本地开发环境标准服务，作为一键启动标准方式。
- **FR-027**: `docker-compose.yml` MUST 与 `metaforge-boot` 的 `application.yml` 中的默认数据源配置保持一致（端口、用户名、密码、数据库名），确保 `docker compose up` 后应用可直接启动连接。

#### 测试基座

- **FR-028**: 系统 MUST 在 `metaforge-framework` 模块中提供标准化单元测试基类（`BaseUnitTest`）与集成测试基类（`BaseIntegrationTest`），测试配置与生产配置隔离。业务 BC 可通过 test scope 依赖 `metaforge-framework` 继承这些基类。
- **FR-029**: 系统 MUST 在 `metaforge-framework` 模块中提供 TestContainers 集成支持——`BaseIntegrationTest` 内置 PostgreSQL 容器化测试环境自动启动与销毁。

#### 公开契约

foundation-core 作为平台基础设施 BC，所有对外能力以契约形式向业务 BC 发布。契约文档分为两类：**编码消费型**（BC 开发者调用/实现时查阅的手册）和**能力声明型**（告知 BC"基座已提供，勿重复实现"的清单）。共计 6 份契约文件（5 Markdown + 1 OpenAPI YAML）。

- **FR-030**: 系统 MUST 提供 `api-contracts.md`，作为 BC 编码时直接消费的核心 API 契约手册，包含：(a) **SPI 扩展点接口**——六大扩展点的接口签名、注册生命周期（发现→加载→排序→调用）、BC 级隔离保证、默认实现与自定义实现的协作规则；(b) **公共 DTO**——`ApiResponse<T>`（code/message/data/traceId）、`PageRequest`（page/size/sort）、`PageResult<T>`（content/total/page/size/totalPages）、基础实体 DTO（含审计字段）的字段定义与构造约定；(c) **库级 API**——CacheManager 缓存读写规范（Key 命名约定为 `<bc-name>:<entity>:<id>`、过期策略参数）、ObjectMapper JSONB 序列化规范（日期格式 `yyyy-MM-dd HH:mm:ss`、空值策略 `NON_NULL`、类型映射）、`PageUtils` 内存分页工具 API、`PageHelper` 转换工具（common DTO ↔ Spring Pageable/Page）用法；(d) **异常基类体系**——异常层次结构（系统异常/校验异常/扩展异常）、各异常类与错误码的映射关系、业务 BC 注册自定义异常类型的机制；(e) **参数校验扩展**——自定义校验注解的开发方式、校验异常与标准错误响应的转换规则；(f) **运行时行为规范**——TraceId 格式（生成算法、HTTP 头 `X-Trace-Id`、日志输出格式、跨线程透传）、结构化日志格式（字段定义、日志级别规则、敏感字段脱敏规则）。
- **FR-031**: 系统 MUST 提供 REST API 契约：`rest-api-contract.md` + `rest-api-contract.yaml`（OpenAPI 3.0），定义统一响应体格式、错误码分类目录及 HTTP 状态码映射。面向 API 消费者（前端/外部系统），确保全平台 REST 接口响应一致。
- **FR-031a**: 系统 MUST 提供 `platform-capabilities.md`，逐项声明 foundation-core 已默认启用且业务 BC 禁止重复实现的平台能力清单，每项简要说明能力范围与"BC 无需/禁止重复"的约束。覆盖能力：虚拟线程（FR-006）、日志脱敏（FR-010）、API 文档 OpenAPI 3.0（FR-014）、国际化 i18n（FR-015）、可观测性指标与健康检查端点（FR-016）、安全基线 XSS/SQL 注入防御/CORS（FR-017）、数据源 HikariCP 连接池 + 事务管理（FR-018）、跨 Schema 写校验（FR-020）、Flyway 数据库迁移统一管理（FR-020a）、测试基座 BaseUnitTest/BaseIntegrationTest + TestContainers（FR-028/FR-029）。
- **FR-032**: 系统 MUST 提供 `configuration-schema.md`，以 Markdown 表格定义所有 foundation-core 相关配置项（属性名、类型、默认值、可选值范围、说明），使用 Spring Boot 原生键名（如 `spring.cache.*`、`spring.datasource.*`、`logging.*`）。
- **FR-033**: （已合并至 FR-030(b)，编号保留为占位，不再有独立内容。）
- **FR-034**: （已合并至 FR-030(c)，编号保留为占位，不再有独立内容。）
- **FR-035**: （已合并至 FR-030(f)，编号保留为占位，不再有独立内容。）
- **FR-036**: 所有契约文档 MUST 存放于 `specs/<feature_dir>/contracts/` 目录。契约变更 MUST 保持向后兼容，破坏性变更须走 MAJOR 版本升级并附迁移指南。`context/contracts/` 目录仅供 `speckit.contract.export` 命令导出，计划和实现阶段不得直接写入该目录。
- **FR-037**: 系统 MUST 提供 `build-system-integration.md`，以 Markdown + 嵌入式 Maven XML 模板形式覆盖：(a) BC POM 标准模板（含 `parent` 引用、最小化框架依赖、禁止 `<dependencyManagement>` 与版本属性覆盖）；(b) BC 注册规则（`metaforge-boot/pom.xml` 中 `<dependency>` 声明格式、默认 `compile` scope）；(c) 依赖声明规则（允许声明的框架依赖白名单、BOM 统一管控、禁止直接依赖的模块黑名单）；(d) 构建校验规则（Maven Enforcer 必须规则与推荐规则配置）；(e) flatten-maven-plugin 配置模板（POM 扁平化输出，`${revision}` 变量解析）。

### Key Entities

- **BOM（Bill of Materials）**: metaforge-parent 中定义的全平台依赖版本清单，包含所有核心依赖的 groupId、artifactId、version 三元组，是依赖仲裁的唯一依据。
- **AutoConfiguration（自动装配类）**: metaforge-server 中的 Spring Boot Auto Configuration 定义，每项能力对应一个独立自动装配类，通过标准 Spring Boot 自动装配机制（`AutoConfiguration.imports`）注册，启动时自动生效。
- **Framework Support（框架工具层）**: metaforge-framework 中封装的第三方框架通用工具与支持类，允许依赖 Spring、JPA/Hibernate、Servlet、Caffeine 等框架库，提供框架感知的工具（如 `PageHelper` 分页转换）、模板与测试基类。不包含自动装配逻辑。
- **PageRequest / PageResult（分页契约 DTO）**: metaforge-common 中定义的纯 Java 分页 DTO。`PageRequest` 包含 page、size、sort 字段；`PageResult<T>` 包含 content、total、page、size、totalPages 字段。`PageUtils` 提供内存级分页（List 截取）。`PageHelper`（位于 metaforge-framework）在 Spring Data `Pageable`/`Page` 与 common DTO 间双向转换。
- **SPI Extension Point（SPI 扩展点）**: metaforge-common 中定义的 Java 接口，声明业务 BC 可定制的扩展行为契约，扩展实现通过 Spring 注册机制加载。
- **TraceId（全链路追踪标识）**: 每个 HTTP 请求的全局唯一标识符，在请求入口生成，通过线程上下文在异步场景下透传，附加于日志与响应头。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 业务 BC 开发者从创建 BC 模块开始到应用日志输出 "Started MetaforgeApplication" 为止，纯操作时间不超过 5 分钟（不含 Maven 依赖下载时间），仅需完成 pom.xml 继承、框架依赖声明、metaforge-boot 注册、应用名配置四步操作（Maven archetype 脚手架生成器推迟到后续 feature）。
- **SC-002**: 所有 REST 接口的响应 100% 符合统一响应体格式，成功与异常路径均含 code、message、data、traceId 四个标准字段。
- **SC-003**: HTTP 请求的 TraceId 在日志中的覆盖率 100%——从请求入口日志到业务处理日志到响应出口日志，全程可追溯。
- **SC-004**: 两个不同的业务 BC 同时实现同一 SPI 扩展点时，扩展行为相互隔离，互不干扰率 100%。
- **SC-005**: common 模块的 compile scope 依赖仅限 JDK 标准库、Jackson（core + databind + annotations）及 SLF4J API，不含 Spring、Servlet API、JDBC、JPA/Hibernate、Caffeine、Flyway 等框架类库（通过 `mvn dependency:list` 验证）。
- **SC-006**: 业务 BC 的单元测试（继承 BaseUnitTest）执行单测试方法平均耗时 < 100ms，集成测试（继承 BaseIntegrationTest）含 TestContainers 启动的首次执行耗时 < 30s。
## Assumptions

- 开发环境 Maven 版本为 3.9+，JDK 版本为 21，Docker 及 Docker Compose 已安装可用
- 业务 BC 开发者熟悉 Spring Boot 基本用法（了解依赖注入、自动装配概念）
- MVP 阶段仅支持 PostgreSQL 数据库，不引入其他数据源类型
- 本 spec 不包含 MCP Server 发布能力，该能力归属于后续的 `agent-consumption` BC
- Maven archetype（BC 项目脚手架生成器）不在本 feature 范围，推迟到后续 feature
- 本 spec 不定义各 BC 的具体 Schema 结构，仅提供通用的数据访问技术封装
- 国际化默认支持中文（zh-CN）和英文（en-US），其他语言按需扩展
- SPI 扩展机制在 MVP 阶段以 Spring Bean 注册方式实现，不引入独立的扩展加载框架
- 安全基线仅覆盖基础防御（XSS/SQL 注入），不引入 Spring Security 或 OAuth2 认证授权体系（该能力不在 foundation-core 职责范围）
- 本 spec 使用 sequential 编号（001），后续 features 依次递增
- MVP/首年预期 BC 模块数量为 3-5 个，Maven reactor 全量构建在此规模下性能可接受
- 递归 CTE 图遍历查询模板及 jOOQ 集成推迟到未来图查询/推理 BC 实现，不在 foundation-core 范围
