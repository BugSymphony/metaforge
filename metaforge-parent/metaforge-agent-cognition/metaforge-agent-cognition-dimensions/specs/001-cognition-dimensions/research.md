# Research: 认知算子实现层

**Feature**: 001-cognition-dimensions | **Date**: 2026-08-11

## Decision 1: Maven 模块注册与依赖策略

**Decision**: `-dimensions` 模块仅编译依赖 `metaforge-agent-cognition-api`，通过 `-starter` 聚合到运行时 classpath。

**Rationale**: 遵循 BC 宪法 VII「运行时解耦加载」——引擎核心不编译依赖算子实现层。`-dimensions` 编译时仅需 `-api` 获取 `CognitionOperator`、`CognitionQueryContext`、`CognitionResult`、`DimensionCategory` 及 4 个 Port 接口。运行时由 `-starter` 将所有模块聚合到同一 JVM，`-core` 通过 `@Autowired List<CognitionOperator>` 发现算子 Bean。`-core` 和 `-dimensions` 之间无编译期双向依赖。

**Alternatives considered**:
- `-core` 直接编译依赖 `-dimensions`：违反解耦原则，新增算子需重新编译 `-core`——不符合 BC 宪法 II「声明式扩展铁律」。
- `-dimensions` 独立为完全独立的 JAR 并通过 classpath 扫描加载：增加运维复杂度，与 Starter 聚合模式不兼容。

## Decision 2: 抽象基类承担公共职责

**Decision**: 创建 `AbstractCognitionOperator` 抽象基类，持有 4 个 `@Autowired` Port 字段（`MetamodelReadPort`/`MetadataReadPort`/`GraphReadPort`/`ComputeEngineReadPort`），提供 `applyScope(cognitionResult, scope)` scope 裁剪方法、`buildLazyNode(data, hasChildren, nextCall)` 惰性节点构造方法和 `wrapFailure(operatorId, category, error)` 异常模板方法。所有 25 个算子继承此基类。

**Rationale**: 4 个 Port 字段是所有算子的共同依赖，通过基类统一注入避免每个算子重复声明。scope 裁剪和异常处理是 BC 宪法 V 和 VI 的强制性要求，封装到基类确保一致性，防止算子实现遗漏。

**Alternatives considered**:
- 每个算子独立注入 Port 字段：代码重复，scope 裁剪逻辑可能被遗漏。
- 通过工具类/静态方法提供公共能力：无法直接访问 `@Autowired` Port 字段。
- 通过接口默认方法提供：Java 接口不支持实例字段，Port 注入无法实现。

## Decision 3: Port 调用失败统一处理

**Decision**: 基类提供 `executeWithPort(Supplier)` 模板方法，自动捕获 Port 调用异常并转换为 `CognitionResult.failure(operatorId, category, "UPSTREAM_UNAVAILABLE")`。每个 `timeoutMs` 通过 `CompletableFuture.supplyAsync().orTimeout()` 实现超时控制。

**Rationale**: 遵循 BC 宪法 VI「失败不扩散」——算子失败返回失败对象而非抛异常。超时控制遵循 FR-010。Virtual Thread（Java 21）使超时控制实现简洁。Port 上游不可用统一错误码 `UPSTREAM_UNAVAILABLE`(34011) 是上游契约约定。

**Alternatives considered**:
- 在每个算子中手工 try-catch：代码重复，错误码可能不统一。
- 使用 AOP 切面统一处理：引入额外复杂性，算子内部 Port 调用模式多样（单个查询 vs 多步组合）。

## Decision 4: Lazy 模式实现策略

**Decision**: `buildLazyNode()` 方法返回标准 `Map<String, Object>` 结构：`{ data: ..., has_children: boolean, suggested_next_call: "ontological.xxx" }`。调用方（模板/OutputAssembler）根据 `has_children` 决定是否在响应中标记为可展开节点。

**Rationale**: 遵循 BC 宪法 VIII「渐进式探索」。标准 Map 结构不与特定 DTO 绑定，消费端（引擎核心的 OutputAssembler）可直接序列化为 JSON。惰性模式是 ONTOLOGICAL 分类算子的通用模式。

**Alternatives considered**:
- 定义专门的 LazyNode DTO：增加 `-api` 模块的 DTO 膨胀，且 LazyNode 是算子实现层模式而非 API 契约——不应放在 `-api` 中。
- 使用 Optional/Stream 惰性求值：Java Stream 惰性不适合携带 `has_children` 元信息，且需额外包装层。

## Decision 5: 单元测试策略

**Decision**: 每分类至少 1 个集成测试方法（Mock 4 个 Port 接口），验证算子正确调用 Port 方法、scope 裁剪、失败场景。使用 JUnit 5 + Mockito，Spring 上下文仅加载被测算子 Bean（`@SpringBootTest(classes = {OperatorUnderTest.class})`）。

**Rationale**: 算子逻辑主要是 Port 调用的编排——核心验证点是"算子是否以正确参数调用了正确的 Port 方法"以及"结果是否正确转换"。Mock Port 可消除上游 BC 依赖的不确定性。每个分类至少 1 个集成测试覆盖该分类算子的典型链路。

**Alternatives considered**:
- 纯单元测试（不启动 Spring 容器）：算子依赖 `@Autowired` Port 注入，纯 Mock 需手动构造依赖图，得不偿失。
- 端到端测试（真实 PostgreSQL + 上游 BC）：测试链路过长，算子本身的逻辑验证被上游数据准备淹没，运行缓慢不可控。
