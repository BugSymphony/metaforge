# 技术调研：语义关系实例全生命周期管理

**Feature**: 001-relation-instance-lifecycle | **Date**: 2026-08-01 | **Status**: Complete

## 调研概览

本文档针对 `metaforge-graph` BC 实现过程中的关键技术决策点进行调研，确保所有技术选型与 foundation-core 平台规范一致，并符合 BC 级宪法的强制要求。

---

## 1. 持久化层技术选型

### 1.1 JPA + PostgreSQL JSONB 动态字段存储

- **Decision**: Spring Data JPA + Hibernate 6 + PostgreSQL JSONB 原生支持
- **Rationale**: 
  - foundation-core 已预配置统一数据源（HikariCP），BC 直接复用
  - Hibernate 6 原生支持 JSONB 字段映射，无需额外 TypeDef
  - `content` 字段（关系属性内容）以 JSONB 列存储，动态 Schema 结构由上游 RelationSchema 定义
  - MVP 不需要对象关系图映射（Graph ORM）
- **Alternatives considered**:
  - JDBC Template：需手动结果集映射，代码量大，弃用
  - MyBatis：引入额外依赖，与 foundation 规范不一致，弃用

### 1.2 JSONB 序列化策略

- **Decision**: 统一使用 `JsonbUtils`（foundation-core 提供）进行 JSONB ↔ Java 对象双向转换
- **Rationale**: 
  - foundation-core 已预置 `JsonbUtils.toJsonb()` / `JsonbUtils.fromJsonb()` / `JsonbUtils.fromJsonbList()`
  - 日期格式 `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`，NON_NULL 策略
  - JPA AttributeConverter 中调用 `JsonbUtils` 完成 JPO 字段与 JSONB 列的映射
- **Alternatives considered**:
  - 自定义 Jackson ObjectMapper：违反平台能力复用规范，弃用
  - Hibernate UserType：Hibernate 6 已原生支持 JSONB，不再需要自定义 UserType

### 1.3 JPA AttributeConverter 统一封装

```java
// 用于 JPA @Convert 注解的 JSONB 转换基类
@Converter
public abstract class JsonbConverter<T> implements AttributeConverter<T, String> {
    private final TypeReference<T> typeRef;
    
    @Override
    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) return null;
        return JsonbUtils.toJsonb(attribute);
    }
    
    @Override
    public T convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return JsonbUtils.fromJsonb(dbData, typeRef);
    }
}
```

---

## 2. JSON Schema 结构校验

### 2.1 校验库选择

- **Decision**: `com.networknt:json-schema-validator`（version 1.4.x）
- **Rationale**:
  - 支持 JSON Schema Draft 4/6/7/2019-09/2020-12
  - 性能优异（Jackson 底层，零反射开销），符合草稿创建 ≤50ms 的性能基线
  - 结构化错误信息输出（含字段路径、违规类型、Schema 引用），满足 FR-027 要求
  - 轻量级，无额外依赖链
- **Alternatives considered**:
  - `everit-org/json-schema`：维护已停滞，不推荐
  - `com.github.fge/json-schema-validator`：Jackson 1.x 依赖，已过时
  - Spring Boot 自带：Spring Boot 不内置 JSON Schema 校验器

### 2.2 Schema 来源与缓存策略

- **Decision**: 通过 `metaforge-metamodel-api` 的 `ElementDefinitionService.getRelationSchema(fqn)` 获取 RelationSchema JSON Schema 后，使用 Caffeine 本地缓存（TTL 30 分钟）
- **Rationale**:
  - RelationSchema 发布后不可变，缓存安全无一致性风险
  - 每次校验重新获取 Schema 会显著增加上游调用量，违反性能基线
  - 缓存 key 格式：`metaforge-graph:schema:{relationSchemaFqn}`
- **注意事项**: 已发布版本不可变，缓存无需失效；缓存仅在应用重启后清空

---

## 3. MapStruct 对象转换

### 3.1 版本与配置

- **Decision**: MapStruct 1.5.5.Final + Lombok 兼容配置
- **Rationale**:
  - foundation-core 白名单允许 MapStruct 依赖
  - 减少手动编码的对象转换样板代码
  - Maven compiler plugin 配置 `annotationProcessorPaths` 确保 Lombok 先于 MapStruct 处理
- **配置要点**:
  ```xml
  <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <configuration>
          <annotationProcessorPaths>
              <path>
                  <groupId>org.projectlombok</groupId>
                  <artifactId>lombok</artifactId>
              </path>
              <path>
                  <groupId>org.mapstruct</groupId>
                  <artifactId>mapstruct-processor</artifactId>
                  <version>1.5.5.Final</version>
              </path>
          </annotationProcessorPaths>
      </configuration>
  </plugin>
  ```

### 3.2 转换器位置与范围

- **Decision**: 所有 MapStruct 转换器接口统一存放在 `metaforge-graph-core` 的 `infrastructure/converter/` 包下
- **转换矩阵**:
  | 源类型 | 目标类型 | 场景 |
  |--------|----------|------|
  | `CreateDraftRequest` (api dto) | `RelationInstanceDraft` (domain) | 应用层 → 领域层 |
  | `RelationInstance` (domain) | `RelationInstanceJpo` (jpa) | 仓储适配器持久化 |
  | `RelationInstanceJpo` (jpa) | `RelationInstance` (domain) | 仓储适配器还原 |
  | `RelationInstance` (domain) | `RelationInstanceDto` (api dto) | 领域层 → DTO 响应 |
  | `RelationInstanceDraft` (domain) | `RelationInstanceDraftJpo` (jpa) | 仓储适配器持久化 |
  | `RelationInstanceDraftJpo` (jpa) | `RelationInstanceDraft` (domain) | 仓储适配器还原 |
  | `RelationVersion` (domain) | `RelationVersionJpo` (jpa) | 仓储适配器持久化 |
  | `RelationVersionJpo` (jpa) | `RelationVersion` (domain) | 仓储适配器还原 |
- **Alternatives considered**:
  - 手动编写转换代码：代码量大，易出错，维护成本高
  - BeanUtils 反射拷贝：性能低，无类型安全，隐式字段拷贝风险

---

## 4. 关系 FQN 生成器设计

### 4.1 FQN 格式规范

- **Decision**: 统一格式 `{源实体FQN}#{关系类型FQN}#{目标实体FQN}`
- **Rationale**:
  - `#` 分隔符：与实体 FQN 中的 `.` 分隔符明确区分，避免歧义
  - 三元组结构：天然支持从 FQN 反向解析三要素
  - 所有新增路径（手动创建、自动构建、批量导入）强制使用统一生成器
- **实现**:
  ```java
  public final class FqnGenerator {
      private FqnGenerator() {}
      
      public static FQN generate(EntityFQN source, RelationTypeFQN type, EntityFQN target) {
          return FQN.of(source.value() + "#" + type.value() + "#" + target.value());
      }
      
      public static FqnComponents parse(FQN fqn) {
          String[] parts = fqn.value().split("#", 3);
          if (parts.length != 3) throw new FqnParseException("FQN 格式非法: " + fqn);
          return new FqnComponents(FQN.of(parts[0]), FQN.of(parts[1]), FQN.of(parts[2]));
      }
      
      public record FqnComponents(FQN sourceEntityFqn, FQN relationTypeFqn, FQN targetEntityFqn) {}
  }
  ```

---

## 5. 双向索引存储与查询

### 5.1 索引表设计

- **Decision**: 独立 `entity_relation_index` 表存储双向引用索引
- **Schema**:
  ```sql
  CREATE TABLE semantic_relation_network.entity_relation_index (
      id              BIGSERIAL PRIMARY KEY,
      entity_fqn      VARCHAR(512) NOT NULL,     -- 实体 FQN
      direction       VARCHAR(8)   NOT NULL,     -- 'OUTBOUND' | 'INBOUND'
      relation_fqn    VARCHAR(1536) NOT NULL,    -- 关联的关系 FQN
      created_time    TIMESTAMP    NOT NULL DEFAULT NOW(),
      CONSTRAINT uq_entity_direction_relation UNIQUE (entity_fqn, direction, relation_fqn)
  );
  CREATE INDEX idx_ei_entity_direction ON semantic_relation_network.entity_relation_index(entity_fqn, direction);
  ```
- **Rationale**:
  - 出入边查询性能目标 ≤50ms（百级结果集），依赖高效索引扫描
  - 索引与主表正式数据在同一事务内更新（FR-022），保证强一致性
  - UNIQUE 约束防止重复索引记录

### 5.2 图遍历策略

- **Decision**: MVP 使用邻接表 + PostgreSQL 递归 CTE，不倒排图数据库
- **Rationale**:
  - MVP 500 实体规模下递归 CTE 遍历性能满足指标
  - 不引入 Neo4j 等图数据库以减少运维复杂度
  - 后续若需深度图计算，由 `semantic-query-engine` BC 负责
- **CTE 模板**（由 foundation-core `RecursiveCteTemplate` 提供或参考实现）:
  ```sql
  WITH RECURSIVE relation_path AS (
      -- 起始节点
      SELECT r.relation_fqn, r.source_fqn, r.target_fqn, 1 AS depth
      FROM semantic_relation_network.relation_instance r
      WHERE r.source_fqn = :startEntityFqn
      UNION ALL
      -- 递归扩展
      SELECT r.relation_fqn, r.source_fqn, r.target_fqn, rp.depth + 1
      FROM semantic_relation_network.relation_instance r
      JOIN relation_path rp ON r.source_fqn = rp.target_fqn
      WHERE rp.depth < :maxDepth
  )
  SELECT * FROM relation_path;
  ```

---

## 6. 事件驱动架构实现

### 6.1 发布端设计

- **Decision**: 
  - 领域层定义 `RelationEventPublisher` 接口（发布端口）
  - 基础设施层 `SpringRelationEventPublisher` 使用 `ApplicationEventPublisher` 实现
  - 事件类 `RelationChangeEvent` 定义在 api 模块（作为对外契约）
- **发布时机**: 原子事务成功提交后发布（`@TransactionalEventListener(phase = AFTER_COMMIT)` 或事务成功后手动 publish）
- **Rationale**: 事务回滚不发布事件，杜绝脏通知（FR-016b）

### 6.2 消费端设计

- **Decision**: 
  - 监听器实现在 `metaforge-graph-core` 的 `interfaces/event/` 包下
  - 实现上游 `metaforge-metadata-api` 的 `MetadataChangeListener` 接口
  - Spring Bean 注册，由发布方自动发现并分发
- **幂等处理**: 以实体 FQN + 版本号作为幂等键，通过本地幂等标记表或业务逻辑判断去重（FR-044a）
- **异常隔离**: 消费方异常不影响已提交事务，不重试

### 6.3 事件传递模型

- **Decision**: MVP 使用 Spring ApplicationEvent 同步事件（调用方线程内执行）
- **Rationale**:
  - BC 宪法 MVP 边界明确：不引入消息队列
  - 抽象 `RelationEventPublisher` 接口预留后续 MQ 替换能力
  - 跨 BC 通信已通过 upstream-contract 明确定义事件格式

---

## 7. 上游 BC 对接

### 7.1 metamodel-governance 消费

- **Consumed API**: `ElementDefinitionService.getRelationSchema(fqn)` — 获取已发布 RelationSchema 的 JSON Schema 与基数约束
- **消费路径**: 
  1. 领域层定义 `RelationSchemaRepository` 端口（domain/repository/）
  2. 基础设施层 `MetamodelGatewayAdapter` 实现该端口
  3. 适配器内部注入 `metaforge-metamodel-api` 的 `ElementDefinitionService` Bean
- **缓存策略**: Caffeine 本地缓存，TTL 30 分钟，key 为 relationSchemaFqn

### 7.2 metadata-management 消费

- **Consumed API**:
  - `MetadataQueryService.getByFqn(fqn)` — 校验端点实体存在性与生效状态
  - 事件：`MetadataChangeListener.onMetadataChange(MetadataChangeEvent)` — 响应实体变更
- **消费路径**:
  1. 领域层定义 `MetadataEntityGateway` 端口（domain/repository/）
  2. 基础设施层 `MetadataGatewayAdapter` 实现该端口
  3. 适配器内部注入 `metaforge-metadata-api` 的 `MetadataQueryService` Bean
- **事件监听**: `interfaces/event/MetadataChangeEventListener` 实现 `MetadataChangeListener`

---

## 8. PostgreSQL `pg_trgm` 文本搜索

### 8.1 扩展启用

- **Decision**: Flyway 迁移脚本中执行 `CREATE EXTENSION IF NOT EXISTS pg_trgm;`
- **Rationale**:
  - 满足 FR-050 的 name/description 子串匹配（`ILIKE '%keyword%'`）性能要求
  - GIN 索引类型 `gin_trgm_ops` 加速 `ILIKE` 查询
  - PostgreSQL 16 内置，无需额外安装
- **索引创建**:
  ```sql
  CREATE INDEX idx_ri_name_trgm ON semantic_relation_network.relation_instance 
      USING gin (name gin_trgm_ops);
  CREATE INDEX idx_ri_description_trgm ON semantic_relation_network.relation_instance 
      USING gin (description gin_trgm_ops);
  ```

### 8.2 查询安全

- **Decision**: 使用 JPA 参数化查询（`?1` / `:keyword` 形式），禁止字符串拼接
- **Rationale**: 防止 SQL 注入（FR 边缘案例），参数化查询由 JDBC PreparedStatement 原生保障

---

## 9. 异常码与 SPI 扩展

### 9.1 错误码分配

- **Decision**: BC 错误码范围 **32000-32099**（遵循 foundation-core 约定的 BC 业务错误范围 30000-49999）
- **错误码规划**:
  | 错误码 | 常量 | HTTP | 描述 |
  |--------|------|------|------|
  | 32001 | `FQN_CONFLICT` | 409 | 关系 FQN 已存在（主表或草稿表） |
  | 32002 | `SCHEMA_NOT_PUBLISHED` | 422 | 绑定的 RelationSchema 版本未发布 |
  | 32003 | `SCHEMA_VALIDATION_FAILED` | 422 | JSON Schema 结构校验失败 |
  | 32004 | `RELATION_NOT_FOUND` | 404 | 指定 FQN 的生效关系不存在 |
  | 32005 | `DRAFT_NOT_FOUND` | 404 | 草稿不存在 |
  | 32006 | `VERSION_NOT_FOUND` | 404 | 历史版本不存在 |
  | 32007 | `ACTIVATION_FAILED` | 500 | 生效原子事务执行失败 |
  | 32008 | `DEPENDENCY_BLOCKED` | 409 | 下线被拦截（存在下游强依赖） |
  | 32009 | `ENDPOINT_NOT_ACTIVE` | 422 | 源端或目标端实体未生效 |
  | 32010 | `CARDINALITY_VIOLATION` | 422 | 基数约束违反 |
  | 32011 | `DUPLICATE_DRAFT` | 409 | 同一 FQN 已存在草稿 |
  | 32012 | `FQN_PARSE_ERROR` | 400 | FQN 解析失败 |
  | 32013 | `CROSS_DOMAIN_REJECTED` | 403 | 跨域关系未经授权 |
  | 32014 | `IMPORT_PARSE_FAILED` | 400 | 导入文件解析失败 |
  | 32015 | `ENDPOINT_TYPE_MISMATCH` | 422 | 端点实体类型与 Schema 定义不匹配 |

### 9.2 自定义异常处理 SPI

- **Decision**: 实现 `ExceptionHandlerSpi`，注册为 Spring `@Component`，`@Order(100)`
- **Rationale**: 
  - foundation-core 通过 SPI 链式调用来处理自定义异常
  - 禁止自定义 `@RestControllerAdvice` 与全局异常切面
- **实现框架**:
  ```java
  @Component
  @Order(100)
  public class GraphExceptionHandlerSpi implements ExceptionHandlerSpi {
      @Override
      public ApiResponse<?> handle(Exception e) {
          if (e instanceof GraphBizException bizEx) {
              return ApiResponse.error(bizEx.getCode(), bizEx.getMessage());
          }
          return null; // 未匹配则交由下一个处理链
      }
  }
  ```

---

## 10. MCP 工具定义与实现路径

### 10.1 协议实现

- **Decision**: 通过 Spring AI 的 MCP Server 支持，在 `metaforge-graph-core` 的 `interfaces/mcp/` 下定义 MCP 工具提供者
- **Rationale**: 
  - 全局架构约束：MCP 统一发布由 `agent-consumption` BC 负责
  - 本 BC 的 MCP 工具定义作为内部能力声明，最终由 `agent-consumption` 聚合发布
  - 内部工具提供者以 Spring `@Component` 形式注册，提供标准化的工具接口签名
- **工具范围**: 关系拓扑查询、关系实例查询（供 Agent 上下文获取）
- **Alternatives considered**:
  - 本 BC 独立发布 MCP Server：违反全局架构设计（MCP 由 agent-consumption 统一发布），弃用
  - 不定义 MCP 能力：无法满足 BC 能力自治原则，下游无法标准化消费

### 10.2 工具提供者抽象

```java
// 内部 MCP 工具提供者接口
public interface GraphMcpToolProvider {
    String getName();
    String getDescription();
    ToolCallback getCallback();
}
```

---

## 11. 配置属性设计

### 11.1 统一前缀

- **Decision**: 所有 BC 专属配置属性使用 `metaforge.graph.*` 前缀
- **属性清单**:
  ```yaml
  metaforge:
    graph:
      schema-cache:
        ttl-seconds: 1800        # RelationSchema 缓存 TTL（秒）
        max-size: 200            # 缓存最大条目数
      validation:
        max-content-size: 10485760     # content 字段最大字节数（10MB）
      import:
        batch-size: 100           # 批量导入每批次大小
  ```

---

## 总结

所有关键技术决策均与 foundation-core 平台规范、BC 级宪法、全局宪法保持一致。MVP 阶段遵循最小化原则，不引入消息队列、图数据库、外部缓存中间件。核心性能目标（草稿创建 ≤50ms、FQN 查询 ≤20ms、生效事务 ≤100ms）通过 PostgreSQL 索引优化、Caffeine 本地缓存、JSON Schema 高效校验器组合实现。
