# 验证指南：语义关系实例全生命周期管理

**Feature**: 001-relation-instance-lifecycle | **Date**: 2026-08-01

## 概述

本文档提供 `metaforge-graph` BC 功能的端到端验证场景，覆盖草稿管理、版本生效、多维查询、历史追溯、批量导入导出、自动构建、事件通知与拓扑校验等核心能力。所有场景可独立运行和验证。

## 先决条件

1. **基础环境**:
   - Java 21 + Maven 3.9+
   - PostgreSQL 16（或 Docker 启动的 PostgreSQL 实例）
   - 元模型治理 BC 已发布至少一条 RelationSchema（如 `order:1.0.0.COMPOSITION`）
   - metadata-management BC 中有两个生效实体（如源实体 `Order_001`、目标实体 `OrderItem_003`）

2. **启动命令**:
   ```bash
   # 从仓库根目录启动完整应用（含所有 BC）
   cd $REPO_ROOT
   mvn spring-boot:run -pl metaforge-boot
   ```

3. **测试工具**: curl / Postman / IntelliJ HTTP Client / Spring Boot Test

## 验证场景

### 场景 1：草稿创建与编辑（US-1）

**目标**: 验证基于已发布 RelationSchema 创建关系草稿的全流程。

```bash
# Step 1: 创建关系草稿
curl -X POST http://localhost:8080/api/v1/graph/drafts \
  -H "Content-Type: application/json" \
  -d '{
    "sourceEntityFqn": "Order_001",
    "relationTypeFqn": "order:1.0.0.COMPOSITION",
    "targetEntityFqn": "OrderItem_003",
    "name": "订单项组成关系",
    "description": "订单与订单项的组成关联",
    "content": { "quantity": 1 }
  }'
```

**预期结果**: HTTP 200，返回草稿 DTO，`fqn` 遵循 `Order_001#order:1.0.0.COMPOSITION#OrderItem_003` 格式，`baseVersion` 为 null。

```bash
# Step 2: 更新草稿内容
curl -X PUT http://localhost:8080/api/v1/graph/drafts/Order_001%23order:1.0.0.COMPOSITION%23OrderItem_003/content \
  -H "Content-Type: application/json" \
  -d '{
    "content": { "quantity": 2, "unitPrice": 99.99 }
  }'
```

**预期结果**: HTTP 200，返回更新后的草稿 DTO，content 中 `quantity` 更新为 2 且新增 `unitPrice`。

```bash
# Step 3: 创建重复草稿（应拦截）
curl -X POST http://localhost:8080/api/v1/graph/drafts \
  -H "Content-Type: application/json" \
  -d '{ ... same as Step 1 ... }'
```

**预期结果**: HTTP 409，错误码 `32011`（DUPLICATE_DRAFT）。

```bash
# Step 4: 基于生效版本创建草稿
curl -X POST http://localhost:8080/api/v1/graph/drafts/from-active \
  -H "Content-Type: application/json" \
  -d '{
    "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003"
  }'
```

**预期结果**: HTTP 200，草稿内容初始化为生效版本的全量副本，`baseVersion` 记录原版本号。

---

### 场景 2：草稿生效（US-1）

**目标**: 验证草稿生效的四步原子事务。

```bash
# 前置条件：已有校验通过的草稿（场景 1 Step 1 创建）
curl -X POST http://localhost:8080/api/v1/graph/relations/activate \
  -H "Content-Type: application/json" \
  -d '{
    "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003"
  }'
```

**预期结果**: HTTP 200，返回 `RelationInstanceDto`（含 `currentVersion: 1`）。验证：
1. 主表 `relation_instance` 中有该 FQN 记录
2. 草稿表 `relation_instance_draft` 中该 FQN 已删除
3. 历史表 `relation_version` 中有 v1 的快照记录
4. 索引表 `entity_relation_index` 中有 `Order_001` 的 OUTBOUND 和 `OrderItem_003` 的 INBOUND 记录

```bash
# 验证索引
curl -X GET "http://localhost:8080/api/v1/graph/relations/outbound?entityFqn=Order_001"
curl -X GET "http://localhost:8080/api/v1/graph/relations/inbound?entityFqn=OrderItem_003"
```

**预期结果**: 均返回包含刚生效关系的列表。

---

### 场景 3：多维查询（US-3）

**目标**: 验证 FQN 精准查询、出入边查询、多维过滤查询。

```bash
# FQN 精准查询
curl -X GET "http://localhost:8080/api/v1/graph/relations/Order_001%23order:1.0.0.COMPOSITION%23OrderItem_003"
```

**预期结果**: HTTP 200，返回完整属性（name、description、content、currentVersion 等），响应时间 ≤20ms。

```bash
# 实体出边查询（按关系类型过滤）
curl -X GET "http://localhost:8080/api/v1/graph/relations/outbound?entityFqn=Order_001&relationType=COMPOSITION"
```

**预期结果**: HTTP 200，返回 COMPOSITION 类型的出边关系列表，响应时间 ≤50ms。

```bash
# 多维过滤查询
curl -X POST http://localhost:8080/api/v1/graph/relations/filter \
  -H "Content-Type: application/json" \
  -d '{
    "relationTypes": ["COMPOSITION"],
    "nameKeyword": "组成",
    "pageRequest": { "page": 1, "size": 20 }
  }'
```

**预期结果**: HTTP 200，返回符合 COMPOSITION 类型且 name 包含"组成"的关系分页列表，响应时间 ≤100ms。

```bash
# 空结果不报错——多维过滤无匹配
curl -X POST http://localhost:8080/api/v1/graph/relations/filter \
  -H "Content-Type: application/json" \
  -d '{
    "relationTypes": ["NONEXISTENT"],
    "pageRequest": { "page": 1, "size": 20 }
  }'
```

**预期结果**: HTTP 200，data.content 为空数组，total 为 0。

---

### 场景 4：关系下线与依赖校验（US-4）

**目标**: 验证下线操作前的依赖校验与下线后状态。

```bash
# Step 1: 校验下线前置条件
curl -X POST http://localhost:8080/api/v1/graph/relations/check-deprecation \
  -H "Content-Type: application/json" \
  -d '{
    "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003"
  }'
```

**预期结果**: HTTP 200，返回 `{ canDeprecate: true, blockingRelations: [] }`（假设无强依赖）。

```bash
# Step 2: 执行下线
curl -X POST http://localhost:8080/api/v1/graph/relations/deprecate \
  -H "Content-Type: application/json" \
  -d '{
    "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003"
  }'
```

**预期结果**: HTTP 200，data 为 null。验证：
1. 主表中该 FQN 已不存在
2. 历史表中仍保留所有历史版本
3. 索引表中该关系的记录已删除

```bash
# Step 3: 查询已下线关系——应返回"不存在"
curl -X GET "http://localhost:8080/api/v1/graph/relations/Order_001%23order:1.0.0.COMPOSITION%23OrderItem_003"
```

**预期结果**: HTTP 404，错误码 `32004`（RELATION_NOT_FOUND）。

---

### 场景 5：历史版本追溯（US-4）

**目标**: 验证历史版本列表查询、单版本详情、版本差异对比。

```bash
# Step 1: 查询历史版本列表
curl -X GET "http://localhost:8080/api/v1/graph/versions/Order_001%23order:1.0.0.COMPOSITION%23OrderItem_003"
```

**预期结果**: HTTP 200，返回版本列表（倒序），不需要 content。响应时间 ≤100ms。

```bash
# Step 2: 重新生效并再次修改生效（产生多版本）
# 执行 reactivate + 多次 activate，产生 v1（原）、v2（再生效）、v3（再修改生效）

# Step 3: 差异对比
curl -X POST http://localhost:8080/api/v1/graph/versions/diff \
  -H "Content-Type: application/json" \
  -d '{
    "fqn": "Order_001#order:1.0.0.COMPOSITION#OrderItem_003",
    "versionA": 1,
    "versionB": 3
  }'
```

**预期结果**: HTTP 200，返回字段级差异按 ADDED/MODIFIED/DELETED 分类。

---

### 场景 6：事件发布验证（US-6）

**目标**: 验证关系变更事件的发布。

**验证方式**: 通过集成测试断言事件发布。

```java
// 集成测试伪代码
@SpringBootTest
class RelationChangeEventTest extends BaseIntegrationTest {

    @Autowired
    private RelationActivationService activationService;

    @MockBean
    private RelationChangeListener testListener;  // 下游模拟监听器

    @Test
    void shouldPublishActivatedEventOnDraftActivation() {
        // given: 创建草稿并执行生效
        activationService.activate(draftFqn);

        // then: 事务成功后事件被发布
        verify(testListener, timeout(1000))
            .onRelationChange(argThat(event ->
                event.getChangeType() == ChangeType.ACTIVATED &&
                event.getFqn().equals(expectedFqn) &&
                event.getVersion() > 0
            ));
    }

    @Test
    void shouldNotPublishEventOnTransactionRollback() {
        // given: 端点实体已下线，导致生效校验失败

        // when: 执行生效
        // then: 事务回滚，无事件发布
        verify(testListener, never()).onRelationChange(any());
    }
}
```

**预期结果**:
- 生效成功 → 发布 ACTIVATED 事件（含 FQN、版本号、RelationSchema FQN、端点 FQN、时间戳）
- 下线成功 → 发布 DEPRECATED 事件
- 事务回滚 → 无事件发布

---

### 场景 7：批量导入导出（US-5）

**目标**: 验证 YAML/JSON 格式批量导入与导出。

```bash
# Step 1: 批量导入（JSON 格式）
curl -X POST http://localhost:8080/api/v1/graph/import \
  -H "Content-Type: application/json" \
  -d '{
    "content": "[{\"sourceEntityFqn\":\"Order_001\",\"relationTypeFqn\":\"order:1.0.0.COMPOSITION\",\"targetEntityFqn\":\"OrderItem_004\",\"name\":\"条目4组成\",\"content\":{\"qty\":1}}]",
    "format": "JSON",
    "strategy": "SKIP"
  }'
```

**预期结果**: HTTP 200，返回 `ImportResultDto`：
- 导入成功的关系进入草稿表（非主表）
- FQN 重复且采用 SKIP 策略时计入跳过清单
- 校验失败的数据计入失败清单（含失败原因）

```bash
# Step 2: 验证导入数据未进入主表
curl -X GET "http://localhost:8080/api/v1/graph/relations/Order_001%23order:1.0.0.COMPOSITION%23OrderItem_004"
# 预期: HTTP 404（草稿未生效，对外不可见）

# Step 3: 导出为 YAML
curl -X POST http://localhost:8080/api/v1/graph/export \
  -H "Content-Type: application/json" \
  -d '{
    "fqnPrefixes": ["Order_001#"],
    "format": "YAML"
  }'
```

**预期结果**: HTTP 200，返回包含所有 `Order_001#` 前缀生效关系的 YAML 内容。

---

### 场景 8：拓扑完整性校验（US-3 扩展）

**目标**: 验证批量拓扑完整性校验报告。

```bash
curl -X POST http://localhost:8080/api/v1/graph/topology/validate \
  -H "Content-Type: application/json" \
  -d '{
    "fqnPrefix": "Order_001#",
    "relationType": "COMPOSITION"
  }'
```

**预期结果**: HTTP 200，返回 `TopologyValidationReport`：
- `totalChecked` 显示校验数量
- `issues` 清单列出悬空边、无效端点、基数异常、元模型不匹配四类问题
- 若全部正常则 `issuesFound` 为 0

---

### 场景 9：边界与异常验证

```bash
# 端点未生效时创建草稿——应拦截
# 预期: HTTP 422，错误码 32009（ENDPOINT_NOT_ACTIVE）

# Schema 版本未发布时创建草稿——应拦截
# 预期: HTTP 422，错误码 32002（SCHEMA_NOT_PUBLISHED）

# 基数约束违反时生效——应拦截
# 预期: HTTP 422，错误码 32010（CARDINALITY_VIOLATION）

# 存在强依赖时下线——应拦截
# 预期: HTTP 409，错误码 32008（DEPENDENCY_BLOCKED）

# 分页超出有效范围（page 过大）
# 预期: HTTP 200，返回空列表

# name/description 关键词包含 SQL 特殊字符
# 预期: HTTP 200，参数化查询防注入，正常返回结果（可能为空）
```

---

## 性能验证

所有验证场景中性能应满足以下基线：

| 操作 | 目标 | 验证点 |
|------|------|--------|
| 草稿创建（含结构校验） | ≤50ms | 响应时间 |
| FQN 精准查询 | ≤20ms | 响应时间 |
| 出入边查询（百级） | ≤50ms | 响应时间 |
| 多维过滤查询（3 维度） | ≤100ms | 响应时间 |
| 草稿生效（四步事务） | ≤100ms | 响应时间 |
| 历史版本列表 | ≤100ms | 响应时间 |
| 批量导入 500 条 | ≤6s | 响应时间 |

---

## 集成测试执行

```bash
# 运行本 BC 所有集成测试
cd $BC_PATH/metaforge-graph-core
mvn test -Pintegration-test

# 运行特定测试类
mvn test -Dtest=RelationDraftServiceImplTest

# 运行契约适配测试（验证上游 BC 对接）
mvn test -Dtest=MetamodelGatewayAdapterTest
mvn test -Dtest=MetadataGatewayAdapterTest
```
