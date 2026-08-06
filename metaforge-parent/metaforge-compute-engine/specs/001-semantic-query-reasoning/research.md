# Research Notes: 语义查询与推理引擎

## 1. PostgreSQL 递归 CTE 图遍历方案

**Decision**: 使用 PostgreSQL `WITH RECURSIVE` CTE 实现全部图遍历查询，通过 jOOQ 类型安全 DSL 构建查询。

**Rationale**:
- Global Architecture 已决策 MVP 阶段使用 PostgreSQL 递归 CTE 替代图数据库
- 递归 CTE 在 1000 实体/10000 关系规模下百毫秒级响应完全可行
- jOOQ 提供编译期类型安全的 SQL DSL，支持 `withRecursive()` API，避免 SQL 字符串拼接
- 无需引入 Neo4j、JGraphT 等外部图库，减少运维复杂度与技术栈膨胀
- 预研方案：使用 `WITH RECURSIVE cte AS (种子 UNION ALL 迭代) SELECT * FROM cte` 模式实现 BFS/DFS 遍历

**Alternatives Considered**:
- Neo4j + Cypher：引入图数据库运维成本，MVP 过度工程化
- JGraphT 内存图引擎：数据持久化需额外同步逻辑，与上游表数据一致性问题
- JPA Entity Graphs：无法表达多度递归遍历语义
- 原生 JDBC SQL 拼接：安全风险（SQL 注入），维护性差

**jOOQ 跨 Schema 查询策略**:
- 使用 jOOQ `DSLContext` 配置统一数据源，所有表通过 `DSL.table(name("schema", "table"))` 引用
- 不依赖 jOOQ 代码生成器（上游表结构不可控），采用手动定义表字段的方式
- 递归 CTE 通过 `DSLContext.withRecursive()` 方法构建，变量绑定防止 SQL 注入
- 跨 BC 仅执行 SELECT 查询，符合 Foundation Contract 的 Schema 写校验约束

---

## 2. jOOQ 端口-适配器范式

**Decision**: 领域层定义纯 Java 端口接口（`EntityDataPort`、`RelationDataPort`），基础设施层通过 jOOQ 适配器实现，负责 jOOQ Record 与领域对象的双向转换。

**Rationale**:
- 遵循 DDD 端口-适配器架构：领域层不引入 jOOQ 依赖，保持纯领域模型
- 通过 MapStruct 实现 Record ↔ Domain 对象转换，减少样板代码
- 后续如需要替换 jOOQ 为原生 JDBC 或特定方言优化，仅修改基础设施层适配器
- 与现有 metamodel/metadata/graph BC 的 JPA Repository 模式解耦，本 BC 仅做只读查询不需要 Repository 模式

**Alternatives Considered**:
- JPA + Entity Graphs：JPA Entity 映射需要写权限 Schema，本 BC 无自有表，跨 Schema 只读场景不适合
- Spring Data JDBC：仍需要定义自有聚合根映射，不适合跨 Schema 只读
- MyBatis：需要 XML Mapper 配置，不如 jOOQ 类型安全

---

## 3. AssociationType 传导规则配置设计

**Decision**: 使用 `application.yml` 中的 `metaforge.compute-engine.transitivity-rules` 配置节点定义 AssociationType 传导规则 YAML 列表，通过 `@ConfigurationProperties` 绑定为 Java 配置对象，启动时加载并可运行时热更新（通过 Spring Cloud Config / `@RefreshScope`）。

**Rationale**:
- 显式声明优于隐式约定：每个 AssociationType 的传递性、方向、权重策略、深度上限一目了然
- 配置化修改无需重新编译部署，支持运维灵活调整（如降低某个类型的遍历深度以优化性能）
- 遵循 Foundation Contract 配置规范（application.yml 全局配置），属性前缀 `metaforge.compute-engine.*`
- 与元模型独立维护：元模型定义结构（EntitySchema/RelationSchema），本 BC 配置文件定义传导语义，职责分离

**配置结构设计**:

```yaml
metaforge:
  compute-engine:
    traversal:
      max-depth: 5                # 全局默认遍历深度（范围 1-10）
      timeout-ms: 2000            # 超时熔断阈值
      max-result-count: 500       # 结果数量上限
    transitivity-rules:
      - type: COMPOSITION
        transitive: true
        direction: forward
        weight-strategy: multiply
        max-depth: 5
        description: "整体-部分层级传递，权重连乘表示约束强度或概率"
      - type: DEPENDENCY_INFLUENCE
        transitive: true
        direction: forward
        weight-strategy: multiply
        max-depth: 2
        description: "依赖链传导，权重乘以(0,1]衰减因子"
      - type: PROCESS_SEQUENCE
        transitive: true
        direction: forward
        weight-strategy: add
        max-depth: 5
        description: "步长/耗时/成本累加，计算路径总长"
      - type: ASSOCIATION_REFERENCE
        transitive: false
        direction: directed
        weight-strategy: null
        max-depth: 1
        description: "不可传递（非隐含关系），作为路径桥梁连接其他可传递关系"
      - type: MAPPING_CORRESPONDENCE
        transitive: false
        direction: bidirectional
        weight-strategy: null
        max-depth: 1
        description: "不传递，max策略合并双向查询的置信度"
```

**Alternatives Considered**:
- 枚举硬编码：灵活性不足，不支持运行时调整
- 元模型扩展字段：传导语义不属于元模型结构范畴，引入耦合适得其反
- 关系数据库配置表：增加运维复杂度，配置表变更需版本管理

---

## 4. 图模式匹配实现策略

**Decision**: 在线性路径模式匹配中，将模式字符串解析为路径段序列（`GraphPattern` 值对象），通过 jOOQ 递归 CTE 沿关系边逐段匹配。通配符 `*` 匹配完整 EntitySchema FQN，`?` 匹配完整 RelationSchema FQN。

**Rationale**:
- 模式长度上限 4 段（3 条关系边），可直接展开为固定深度的递归 CTE
- 通配符匹配通过 SQL `LIKE` 或 `=` 条件实现（`*` → 不添加类型过滤条件，`?` → 不添加关系类型过滤条件）
- 递归 CTE 每步匹配一个路径段，通过 `WHERE` 子句过滤当前段的关系类型/实体类型
- 不需要引入复杂的图模式匹配算法（如子图同构、正则路径查询），保持 MVP 简单性

**Alternatives Considered**:
- 应用层图遍历：每次匹配加载全量数据到内存再匹配，不适用于超时约束（2000ms）
- 正则路径查询引擎：复杂度高，MVP 无此需求

---

## 5. 7 维过滤的 SQL 实现

**Decision**: 在 jOOQ CTE 查询的 `WHERE` 子句中通过 `AND` 组合各维度过滤条件，各维度内使用 `OR`/`IN` 实现集合匹配。`matchMode` 决定匹配策略（PREFIX → `LIKE 'prefix%'`，EXACT → `=`，PATTERN → `LIKE`）。

**Rationale**:
- SQL `WHERE` 条件前置过滤，在 CTE 种子查询和递归步中同时生效，被过滤内容自然不参与进一步遍历
- jOOQ 的条件构建方法（`Condition` / `DSL.or()` / `DSL.and()`）支持动态组合过滤条件
- 属性字段 JSONB 精确等值匹配通过 PostgreSQL `@> ?::jsonb` 或 `->> '' = ''` 实现
- `matchMode` 枚举映射为 SQL 操作符，代码清晰可维护

**各维度实现策略**:
| 维度 | SQL 映射策略 |
|------|-------------|
| associationTypes | `relation_type IN (?, ?, ...)` |
| sourceFqns (PREFIX) | `source_entity_fqn LIKE 'prefix%'` |
| sourceFqns (EXACT) | `source_entity_fqn = ?` |
| targetFqns (PREFIX/EXACT) | 同上 |
| relationInstanceFqns (PATTERN) | `fqn LIKE 'pattern'`（通配符 `_` `%`） |
| entityTypes (PREFIX/EXACT) | `entity_schema_fqn LIKE/=` |
| relationTypes (PREFIX/EXACT) | `relation_schema_fqn LIKE/=` |
| propertyFilters | `content @> '{"field": "value"}'::jsonb` |

---

## 6. 多模块 Maven 结构

**Decision**: 遵循现有 metamodel/metadata/graph BC 的三级 Maven 结构：
- 根模块: `metaforge-compute-engine` (pom)
- API 子模块: `metaforge-compute-engine-api` (jar)
- Core 子模块: `metaforge-compute-engine-core` (jar)

**模块依赖链**:
```
metaforge-compute-engine-api → metaforge-framework (foundation-core 平台层)
metaforge-compute-engine-core → metaforge-compute-engine-api + metaforge-metamodel-api + metaforge-metadata-api + metaforge-graph-api + metaforge-framework + jOOQ + MapStruct
metaforge-boot → metaforge-compute-engine-core (启动注册)
```

**Rationale**:
- 下游 BC（agent-consumption）仅依赖 `api` 模块，无法访问 `core` 模块内部实现
- 与现有 Spring Boot 单 JVM 进程模块化方案一致，无需额外服务发现
- 遵循 Foundation Build System Integration 约束

---

## 7. 平台能力复用清单

**Decision**: 最大化复用 foundation-core 提供的平台能力，BC 仅聚焦图遍历算法、推理规则引擎、影响扩散模型等业务领域逻辑。

| Foundation 能力 | 本 BC 接入方式 | 无需重复实现的内容 |
|----------------|---------------|-------------------|
| 统一响应格式 | Controller 返回 `T`，全局自动包装为 `ApiResponse<T>` | 自定义响应包装类 |
| 全局异常处理 | 实现 `ExceptionHandlerSpi` 注册 33000-33999 范围异常 | 自定义 `@RestControllerAdvice` |
| JSONB 序列化 | 使用 `JsonbUtils.toJsonb()` / `fromJsonb()` | `ObjectMapper` 自定义配置 |
| 分页组件 | 检索查询注入 `PageRequest`，返回 `PageResult<T>` | 自定义分页 DTO |
| 虚拟线程 | 零配置，继承全局配置 | `ThreadPoolTaskExecutor` |
| OpenAPI 文档 | Controller 类加 `@Tag` 注解 | 自定义 SpringDoc 配置 |
| 国际化 | 注入 `MessageSource`，添加 `messages_compute-engine_zh_CN.properties` | 自定义 `MessageSource` bean |
| 健康检查 | 实现 `HealthCheckSpi` | 自定义 Actuator 配置 |
| 数据源 | 注入 `DSLContext`（基于统一数据源配置 jOOQ） | 自定义 `DataSource` bean |
| 测试基类 | 继承 `BaseIntegrationTest`（TestContainers 已内置） | 自定义 TestContainers 配置 |

---

## 8. FQN 生成器使用规范

**Decision**: 所有 FQN 的生成、解析、拼接操作必须使用现有的 FQN 生成器工具类（参考 metamodel BC 中的 `FqnGenerator` 或 `FqnParts` 值对象模式）。

**Rationale**:
- FQN 语法格式统一（`bundleCode:version.packageFqn.Segment`），手动拼接容易出错
- 值对象 `FQN` 封装 FQN 解析逻辑，提供 `bundleCode`、`version`、`packageFqn`、`segment` 等只读派生字段
- 避免硬编码分隔符 `.` `:` 导致 FQN 格式不一致

**注意**: 本 BC 不生成新 FQN（计算层不创建实体/关系），仅消费上游生成的 FQN。但查询结果中需要解析 FQN 提取命名空间前缀以支持 PREFIX 匹配，此类操作须通过 FQN 值对象完成。

---

## 9. MCP 工具集设计策略

**Decision**: 本 BC 通过 Spring AI MCP 暴露 `@Tool` 注解方法作为 MCP 工具集，但 MCP Server 的发布由 agent-consumption BC 统一负责（遵循 BC 宪法 Override 1 和 Global Architecture 中 agent-consumption 的职责定义）。

**Rationale**:
- agent-consumption BC 是 MCP Server 的唯一发布方，统一管理白名单过滤与上下文格式化
- 本 BC 通过 `@Tool` 注解定义可被 MCP 暴露的方法契约，但不启动独立 MCP Server
- 实际 MCP 调用链路：Agent → MCP Client → agent-consumption MCP Server → compute-engine Application Service → 查询执行

**注意**: agent-consumption BC 尚未实现（属于 M3 里程碑），MVP 阶段 MCP 工具集可先通过 Application Service 暴露，待 agent-consumption 就绪后切换接入方式。

---

## 10. 递归 CTE 性能优化策略

**Decision**: 采用以下策略优化递归 CTE 性能：
1. **深度截断**：递归 CTE 中 `UNION ALL` 的递归步通过 `WHERE cte.depth < maxDepth` 限制深度
2. **循环检测**：标准 CTE 循环检测，维护 `path` 数组，`WHERE NOT (target_fqn = ANY(cte.path))`
3. **索引利用**：确保 `entity_relation_index` 的 `source_entity_fqn`、`target_entity_fqn` 列有 BTREE 索引
4. **早期过滤**：在 CTE 种子查询中即应用过滤条件，减少递归步数据量
5. **LIMIT 支持**：路径可达性判定时，`LIMIT 1` 在找到首条路径后立即终断查询

**Rationale**:
- PostgreSQL 递归 CTE 在 1000 实体 10000 关系规模下，3 度遍历百毫秒级响应
- 深度约束 + 循环检测确保不会无限递归
- 索引优化是基础性能保障（上游 semantic-relation-network BC 应已提供）

---

## 11. MapStruct 转换器设计

**Decision**: 在 infrastructure 层定义 MapStruct Converter 接口，覆盖：
- jOOQ `Record` → 领域值对象（`EntitySnapshot`、`RelationSnapshot`）
- 领域聚合根 → DTO（`GraphQueryResult`、`PathResult`、`ClosureResult`、`ImpactTraceResult`）
- 配置属性 → 领域值对象（`TransitivityRule`）

**Rationale**:
- MapStruct 编译期生成转换代码，零反射，性能优于手动 `BeanUtils.copyProperties`
- Converter 层隔离底层 Record/JSONB 结构与领域对象，避免基础设施污染领域层
- 现有 BC 已采用此模式（参考 metamodel-core 的 `infrastructure/mapper/` 包）

---

## 12. 错误码分配

**Decision**: 使用错误码范围 **33000-33999**。

**具体分配**:
| 错误码 | 常量名 | HTTP 状态 | 描述 |
|--------|--------|----------|------|
| 33001 | `ENTITY_NOT_FOUND` | 404 | 查询起点实体 FQN 不存在 |
| 33002 | `TRAVERSAL_DEPTH_EXCEEDED` | 422 | 遍历深度超过配置上限 |
| 33003 | `QUERY_TIMEOUT` | 504 | 查询超时中断 |
| 33004 | `RESULT_COUNT_EXCEEDED` | 422 | 结果数量超过上限 |
| 33005 | `INVALID_PATTERN` | 400 | 图模式匹配语法非法 |
| 33006 | `INVALID_FILTER` | 400 | 过滤参数组合非法 |
| 33007 | `NO_LEGAL_CONDUCTION_PATH` | 422 | 无合法传导路径 |
| 33008 | `PATTERN_LENGTH_EXCEEDED` | 400 | 模式长度超过最大段数 |
| 33009 | `BATCH_SIZE_EXCEEDED` | 400 | 批量查询 FQN 数量超限 |
| 33010 | `UPSTREAM_SERVICE_UNAVAILABLE` | 502 | 上游模块不可用 |
| 33011 | `CIRCULAR_REFERENCE_DETECTED` | 422 | 循环引用已检测并截断 |

**Rationale**:
- 现有 BC 已分配：metamodel 30100-30199，metadata 31000-31099，graph 32000-32099
- 33000-33999 为下一个可用范围
- 错误码通过 `ComputeEngineErrorCodes` 常量类管理，SSOT
