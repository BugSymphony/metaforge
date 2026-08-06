# 技术调研: 元模型治理核心能力 MVP

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-08-01

## 1. FQN 统一生成器设计

### Decision

采用 `FqnGenerator` 接口 + `FqnGeneratorImpl` 实现分离模式，提供纯函数式 FQN 生成与解析能力。接口与实现均位于领域层 `domain/service/`，`FqnGeneratorImpl` 为无状态 Spring Bean（可 Singleton 注入），方法本身保持纯函数式（无副作用、无状态），不依赖数据库或外部服务。

FQN 生成器不对外暴露为独立 REST/MCP 服务——生成逻辑作为 BC 内部领域服务使用，解析能力通过已有的 `resolveFqn` MCP 工具和 REST 查询接口向下游消费方提供。

各 BC（metamodel、metadata、relation-network 等）在各自包路径内独立实现自己的 FQN 生成器，包路径天然形成命名空间隔离，避免跨 BC 冲突。公共接口提取至 `metaforge-common` 为后续优化项，不在当前 MVP 范围。

### Rationale

- FQN 操作是纯字符串变换，不需要任何外部状态或副作用
- 接口与实现分离预留未来将公共接口提取至 `metaforge-common` 的扩展路径
- 无状态 Bean 通过 Spring DI 注入，在实体构造、校验链路、序列化等位置通过 `@Autowired` 或构造器注入使用
- 所有 6 种实体类型（Bundle、BundleVersion、Package、EntitySchema、RelationSchema、AttributeTemplate）共享同一组解析逻辑
- FQN 生成器仅做纯字符串变换，不承担业务格式校验职责；格式合规校验统一在上层写入校验和发布校验环节完成

### Key APIs

```java
public interface FqnGenerator {
    // 生成
    String bundle(String code);
    String bundleVersion(String code, String version);
    String package_(String parentFqn, String segment);
    String entitySchema(String packageFqn, String segment);
    String relationSchema(String packageFqn, String segment);
    String attributeTemplate(String bundleVersionFqn, String segment);

    // 解析
    FqnParts parse(String fqn);
    String toParentFqn(String fqn);
    String toShortName(String fqn);
    String toBundleCode(String fqn);
    String toVersion(String fqn);
    String toFilePath(String fqn);

    // 类型前缀处理（仅用于 API 层 FQN → 纯净 FQN 转换）
    String stripTypePrefix(String typedFqn);
    EntityType detectType(String typedFqn);
}

@Service
public class FqnGeneratorImpl implements FqnGenerator {
    // 纯函数式实现，无状态，线程安全
}
```

### FqnParts Record

```java
public record FqnParts(
    String bundleCode,
    String version,       // null for Bundle
    List<String> segments, // Package路径segments
    String shortName,
    String parentFqn
) {}
```

### Alternatives Considered

- **纯静态工具类**: 简化调用，但无扩展性——后续提取公共接口到 `metaforge-common` 时需大规模重构调用方代码。接口+实现分离模式在调用方式上成本接近（构造器注入），但预留了提取路径。
- **实体自身方法**: 每个实体类型实现 `toFqn()` / `fromParts()`，导致逻辑分散，FQN 文法变更时需修改 6 处
- **对外暴露为 REST/MCP 服务**: FQN 生成是上游写操作内部调用，下游仅需解析能力（已有 `resolveFqn` MCP 工具覆盖），生成能力对外无明确消费场景，暴露会增加不必要的耦合面
- **FQN 生成器内置格式校验**: 生成器作为底层工具，保持纯字符串变换职责更清晰。格式校验统一在读/写/发布链路的上层校验环节完成，避免校验逻辑分散

---

## 2. 属性平铺合并策略

### Decision

发布时通过 `AttributeMergeService` 领域服务完成属性平铺合并。流程：
1. 加载 EntitySchema/RelationSchema 的原生属性定义（JSONB）
2. 遍历 `mountedTemplateFqns` 列表，加载每个 AttributeTemplate 的属性定义集合（JSONB）
3. 按挂载顺序平铺展开所有属性，同名检测 → 冲突时报错
4. 合并后的属性集合转换为扁平 JSON Schema Draft 2020-12 格式
5. 写入目标实体的 `json_schema` 字段

### Rationale

- 草稿态保留组合式结构（模板引用 + 原生属性），编辑灵活
- 发布时一次性能完成全量平铺合并，生成的 Schema 可直接用于下游校验
- 模板加载顺序决定属性优先级（先挂载的模板属性先写入，后续同名报错）

### Implementation

```java
// AttributeMergeService
public JsonNode mergeAttributes(
    List<NativeAttribute> nativeAttrs,
    List<AttributeTemplate> templates
) {
    Map<String, JsonNode> merged = new LinkedHashMap<>();
    // 按模板挂载顺序遍历
    for (AttributeTemplate tpl : templates) {
        for (var entry : tpl.getAttributeDefinitions()) {
            if (merged.containsKey(entry.key)) {
                throw new AttributeNameConflictException(entry.key);
            }
            merged.put(entry.key, entry.value);
        }
    }
    // 然后是原生属性
    for (NativeAttribute attr : nativeAttrs) {
        if (merged.containsKey(attr.getName())) {
            throw new AttributeNameConflictException(attr.getName());
        }
        merged.put(attr.getName(), attr.toJsonNode());
    }
    // 生成 JSON Schema
    return JsonSchemaCompiler.compile(merged);
}
```

### Alternatives Considered

- **属性模板组嵌套引用**: 增加递归展开复杂度，MVP 阶段不需要
- **优先级覆盖而非报错**: 与 spec FR-004 冲突，必须拦截而非覆盖

---

## 3. 循环依赖检测算法

### Decision

基于邻接表的 Kahn 拓扑排序算法 + 深度优先搜索（DFS）组合检测。`BundleDependency` 表存储有向边（source_version_fqn → target_version_fqn）。

### Rationale

- Kahn 算法在 DAG 无环时线性时间 O(V+E)，检测到环时可通过 DFS 回溯输出完整环路
- MVP 规模（≤5 Bundle）下性能完全足够
- 不需要递归 CTE，邻接表在内存中即可完成

### Algorithm

```java
public List<List<String>> detectCycles(Map<String, List<String>> graph) {
    // 1. Kahn 拓扑排序检测有无环
    Map<String, Integer> inDegree = computeInDegree(graph);
    Queue<String> queue = new ArrayDeque<>();
    for (var entry : inDegree.entrySet()) {
        if (entry.getValue() == 0) queue.add(entry.getKey());
    }
    int visited = 0;
    while (!queue.isEmpty()) {
        String node = queue.poll();
        visited++;
        for (String neighbor : graph.getOrDefault(node, List.of())) {
            inDegree.merge(neighbor, -1, Integer::sum);
            if (inDegree.get(neighbor) == 0) queue.add(neighbor);
        }
    }
    // 2. 有剩余节点 → 存在环，DFS 回溯找路径
    if (visited < graph.size()) {
        return findAllCycles(graph, inDegree);
    }
    return List.of();
}
```

### Alternatives Considered

- **PostgreSQL 递归 CTE 检测**: 需要将依赖图写入数据库查询，MVP 规模下内存算法更简单直观
- **Floyd-Warshall**: O(V³) 复杂度不必要，仅需检测有无环而非全对最短路径

---

## 4. 升级等级匹配校验算法

### Decision

发布时对比源版本与草稿版本的实体变更差异，按规则判定是否匹配声明的升级等级。

### Rules Mapping

| 变更类型 | PATCH | MINOR | MAJOR |
|---------|-------|-------|-------|
| description 修改 | ✓ | ✓ | ✓ |
| name 修改 | ✓ | ✓ | ✓ |
| 新增 EntitySchema / RelationSchema | ✗ | ✓ | ✓ |
| 新增 AttributeTemplate | ✗ | ✓ | ✓ |
| 删除 EntitySchema / RelationSchema | ✗ | ✗ | ✓ |
| 删除 AttributeTemplate | ✗ | ✗ | ✓ |
| 修改关联类型（AssociationType） | ✗ | ✗ | ✓ |
| 修改必填属性 | ✗ | ✗ | ✓ |
| 新增可选属性 | ✗ | ✓ | ✓ |
| 枚举值删除 | ✗ | ✗ | ✓ |

### Implementation Strategy

```java
public enum UpgradeLevel { PATCH, MINOR, MAJOR }

public record ChangeReport(
    boolean hasElementAddition,
    boolean hasElementDeletion,
    boolean hasRelationTypeChange,
    boolean hasRequiredAttributeAddition,
    boolean hasEnumValueDeletion,
    boolean hasBreakingChange
) {}

// 对比源版本与草稿版本生成 ChangeReport
public ChangeReport diff(BundleVersion source, BundleVersion draft);

// 匹配校验
public boolean isCompatible(UpgradeLevel declared, ChangeReport report) {
    return switch (declared) {
        case PATCH -> !report.hasElementAddition() && !report.hasElementDeletion()
                       && !report.hasBreakingChange();
        case MINOR -> !report.hasElementDeletion() && !report.hasBreakingChange();
        case MAJOR -> true;
    };
}
```

---

## 5. Flyway 数据库迁移策略

### Decision

遵循 foundation-core 的 Flyway 统一管理规范。迁移脚本按 `V<n>__<bc-name>_<type>.sql` 命名，提交到 `metaforge-boot/src/main/resources/db/migration/`。

### Migration Scripts

| 版本 | 文件 | 内容 |
|------|------|------|
| V1 | `V1__metamodel_governance_ddl.sql` | 创建 `metamodel_governance` Schema 及所有实体表（Bundle、BundleVersion、Package、EntitySchema、RelationSchema、AttributeTemplate、BundleDependency、ExportManifest） |
| V2 | `V2__metamodel_governance_init.sql` | 预置 `metaforge` Bundle v1.0.0 数据（含 `agent` 和 `common` 包的基础元模型元素） |

### Key DDL Design Notes

- FQN 字段定义为 `VARCHAR(512) UNIQUE NOT NULL`
- JSONB 列（`native_attributes`、`mounted_template_fqns`、`json_schema`、`embedding`）使用 PostgreSQL `JSONB` 类型
- `embedding` 字段为 `JSONB DEFAULT NULL`，MVP 阶段不实现向量逻辑
- 状态字段 `status` 使用 `VARCHAR(20)` 枚举约束（DRAFT / PUBLISHED）
- 所有表包含 `id BIGSERIAL PRIMARY KEY`、`created_time TIMESTAMP`、`updated_time TIMESTAMP`

### Alternatives Considered

- **各 BC 独立管理 Flyway 迁移**: 违反 foundation-core 的 Flyway 统一管理规范（FR-020a），不可行
- **Liquibase**: foundation-core 已明确 Flyway 为唯一迁移工具，不引入 Liquibase

---

## 6. JSONB vs 关系型存储边界

### Decision

以下字段使用 PostgreSQL JSONB 存储：
- `native_attributes` (EntitySchema / RelationSchema) — 原生属性定义
- `mounted_template_fqns` (EntitySchema / RelationSchema) — 挂载属性模板组 FQN 列表
- `attribute_definitions` (AttributeTemplate) — 属性模板定义集合
- `json_schema` (EntitySchema / RelationSchema) — 发布时生成的扁平 JSON Schema
- `embedding` (Bundle / Package / EntitySchema / RelationSchema) — 向量占位字段

以下字段使用标准关系型列：
- FQN（VARCHAR 512，UNIQUE INDEX）
- name（VARCHAR 255）
- description（TEXT）
- 关联类型（VARCHAR 50）
- 状态（VARCHAR 20）

### Rationale

- 属性定义天然是动态结构，JSONB 避免 EAV 模式或大量 JOIN
- `json_schema` 字段对外直接输出，无需二次转换
- FQN / name / description 等字段需要精确索引查询，使用标准列类型
- 符合 foundation-core 的 JSONB 序列化工具（`JsonbUtils`）支持

---

## 7. FQN 前缀集合查询实现

### Decision

EntitySchema 和 RelationSchema 的过滤查询（FR-QRY-01~02）精简为 FQN 前缀集合查询（`List<String> fqnPrefixes`），通过 Spring Data JPA Specification 动态组合 `LIKE 'prefix%'` 条件，多个前缀 OR 拼接。利用 FQN 字段的 B-tree 索引完成高效前缀扫描。

### Rationale

- FQN 前缀本身已编码 Bundle/版本/Package 的完整层级信息，无需单独的 `bundleFqn` / `bundleVersionFqn` / `packageFqn` 维度
- 例如 `order:` 等价于按 Bundle 过滤，`order:1.0.0.` 等价于按版本过滤，`order:1.0.0.pkg_order.` 等价于按 Package 过滤
- 集合查询满足 agent-context BC 的核心场景：按多个 Package 前缀批量筛选元模型元素
- B-tree 索引的前缀扫描能力天然支持 `LIKE 'prefix%'`，≤1000 实体规模下性能足够
- `PageRequest` / `PageResult` 复用 foundation-core 分页组件

### Alternatives Considered

- **四维度独立参数**: 增加 API 表面积和维护成本，本质上都是 FQN 前缀的不同表达形式
- **全文索引 (GIN)**: FQN 字段不需要分词搜索，B-tree 前缀扫描完全足够
