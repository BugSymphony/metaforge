# 快速验证指南: 元模型治理核心能力 MVP

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Data Model**: [data-model.md](./data-model.md)

本文档提供端到端验证场景，证明核心功能工作正常。具体实现代码见 `tasks.md` 及源码。

---

## 前置条件

1. **环境**: JDK 21, Maven 3.9+, PostgreSQL 16, Docker (TestContainers)
2. **构建**: `mvn clean install -pl metaforge-boot -am -DskipTests`
3. **启动**: `mvn spring-boot:run -pl metaforge-boot`
4. **SWAGGER**: `http://localhost:8080/swagger-ui.html`（选择 `metamodel` 分组）
5. **Flyway**: 启动后自动执行 `V1__metamodel_governance_ddl.sql` 和 `V2__metamodel_governance_init.sql`，预置 `metaforge:1.0.0`

---

## 场景 1: 完整 Bundle 生命周期（User Story 1 + 2）

### 步骤

```bash
# 1. 创建 Bundle
curl -X POST http://localhost:8080/api/v1/metamodel/bundles \
  -H "Content-Type: application/json" \
  -d '{
    "fqn": "order",
    "name": "订单领域",
    "description": "覆盖电商订单的完整生命周期建模，含订单创建、支付、履约、售后。",
    "owner": "zhangsan"
  }'
# 预期: 201, 返回 BundleDto (status=DRAFT 的 v0.0.1 草稿版本)

# 2. 在草稿版本中创建 Package
curl -X POST http://localhost:8080/api/v1/metamodel/packages \
  -H "Content-Type: application/json" \
  -d '{
    "bundleVersionFqn": "order:0.0.1",
    "parentPackageFqn": null,
    "segment": "pkg_order",
    "description": "订单领域命名空间"
  }'
# 预期: 201, Package FQN = order:0.0.1.pkg_order

# 3. 创建 AttributeTemplate
curl -X POST http://localhost:8080/api/v1/metamodel/attribute-templates \
  -H "Content-Type: application/json" \
  -d '{
    "bundleVersionFqn": "order:0.0.1",
    "segment": "AuditFields",
    "name": "审计字段模板",
    "attributeDefinitions": [
      {"name": "createdBy", "type": "string", "required": true, "description": "创建人"},
      {"name": "createdAt", "type": "string", "required": true, "description": "创建时间", "constraints": {"format": "date-time"}}
    ]
  }'
# 预期: 201, AttributeTemplate FQN = order:0.0.1.AuditFields

# 4. 创建 EntitySchema（挂载模板 + 原生属性）
curl -X POST http://localhost:8080/api/v1/metamodel/entity-schemas \
  -H "Content-Type: application/json" \
  -d '{
    "packageFqn": "order:0.0.1.pkg_order",
    "segment": "Order",
    "name": "订单实体",
    "description": "描述电商订单的核心概念。适用场景：订单创建、支付、履约。",
    "nativeAttributes": [
      {"name": "orderAmount", "type": "number", "required": true, "description": "订单金额", "constraints": {"minimum": 0}}
    ],
    "mountedTemplateFqns": ["order:0.0.1.AuditFields"]
  }'
# 预期: 201, EntitySchema FQN = order:0.0.1.pkg_order.Order

# 5. 再创建 Item EntitySchema
curl -X POST http://localhost:8080/api/v1/metamodel/entity-schemas \
  -H "Content-Type: application/json" \
  -d '{
    "packageFqn": "order:0.0.1.pkg_order",
    "segment": "Item",
    "name": "订单项",
    "description": "订单内单个商品项。适用场景：商品下单。",
    "nativeAttributes": [
      {"name": "quantity", "type": "integer", "required": true, "description": "数量", "constraints": {"minimum": 1}}
    ],
    "mountedTemplateFqns": ["order:0.0.1.AuditFields"]
  }'
# 预期: 201

# 6. 创建 RelationSchema
curl -X POST http://localhost:8080/api/v1/metamodel/relation-schemas \
  -H "Content-Type: application/json" \
  -d '{
    "packageFqn": "order:0.0.1.pkg_order",
    "segment": "Order_contains_Item",
    "name": "订单包含商品",
    "description": "订单与订单项的组成关系",
    "sourceFqn": "order:0.0.1.pkg_order.Order",
    "targetFqn": "order:0.0.1.pkg_order.Item",
    "associationType": "组成",
    "cardinalitySource": "1",
    "cardinalityTarget": "1..*"
  }'
# 预期: 201, RelationSchema FQN = order:0.0.1.pkg_order.Order_contains_Item

# 7. 配置导出清单
curl -X PUT http://localhost:8080/api/v1/metamodel/versions/order:0.0.1/export-manifest \
  -H "Content-Type: application/json" \
  -d '{"packageFqns": ["order:0.0.1.pkg_order"]}'
# 预期: 200

# 8. 发布版本
curl -X POST http://localhost:8080/api/v1/metamodel/versions/order:0.0.1/publish
# 预期: 200, status → PUBLISHED, json_schema 已生成

# 9. 验证 json_schema 已固化
curl http://localhost:8080/api/v1/metamodel/entity-schemas/order:0.0.1.pkg_order.Order
# 预期: jsonSchema 字段非空, 包含 orderAmount + createdBy + createdAt 三个属性
```

### 验证要点

- [x] EntitySchema 对外的 json_schema 已平铺合并（模板属性 + 原生属性）
- [x] name 字段（"订单实体"）独立于 FQN 短名（"Order"）
- [x] embedding 字段在 DTO 中返回（值为 null，MVP 阶段占位）
- [x] enabled 字段从 status 正确推导（DRAFT = false, PUBLISHED = true）

---

## 场景 2: 属性名冲突校验（Edge Case）

### 步骤

```bash
# 1. 创建草稿版本
curl -X POST http://localhost:8080/api/v1/metamodel/bundles/order/versions \
  -H "Content-Type: application/json" \
  -d '{"upgradeLevel": "MINOR"}'
# → order:0.1.0

# 2. 创建数据库字段模板（含同名 createdBy）
curl -X POST http://localhost:8080/api/v1/metamodel/attribute-templates \
  -H "Content-Type: application/json" \
  -d '{
    "bundleVersionFqn": "order:0.1.0",
    "segment": "DbFields",
    "name": "数据库字段模板",
    "attributeDefinitions": [
      {"name": "createdBy", "type": "string", "required": true, "description": "数据库创建人"}
    ]
  }'
# 预期: 201

# 3. 尝试挂载两个同名模板到 EntitySchema
curl -X POST http://localhost:8080/api/v1/metamodel/entity-schemas \
  -H "Content-Type: application/json" \
  -d '{
    "packageFqn": "order:0.1.0.pkg_order",
    "segment": "ConflictTest",
    "name": "冲突测试实体",
    "description": "测试属性名冲突校验。适用场景：测试。",
    "mountedTemplateFqns": ["order:0.0.1.AuditFields", "order:0.1.0.DbFields"]
  }'
# 预期: 400, 错误码 30106, 提示 "createdBy" 冲突
```

### 验证要点

- [x] 写入阶段检测到属性名冲突并拦截
- [x] 错误信息包含冲突字段名和冲突来源

---

## 场景 3: FQN 统一生成器（FR-FQN-01~04）

### Java 单元测试验证

```java
// FqnGeneratorTest 中的测试用例
@Test
void testGenerateBundleFqn() {
    assertEquals("order", FqnGenerator.bundle("order"));
}

@Test
void testGenerateBundleVersionFqn() {
    assertEquals("order:1.0.0",
        FqnGenerator.bundleVersion("order", "1.0.0"));
}

@Test
void testGenerateEntitySchemaFqn() {
    assertEquals("order:1.0.0.pkg_order.Order",
        FqnGenerator.entitySchema("order:1.0.0.pkg_order", "Order"));
}

@Test
void testParseFqn() {
    FqnParts parts = FqnGenerator.parse("order:1.0.0.pkg_order.Order");
    assertEquals("order", parts.bundleCode());
    assertEquals("1.0.0", parts.version());
    assertEquals(List.of("pkg_order"), parts.segments());
    assertEquals("Order", parts.shortName());
    assertEquals("order:1.0.0.pkg_order", parts.parentFqn());
}

@Test
void testStripTypePrefix() {
    String stripped = FqnGenerator.stripTypePrefix("entity:order:1.0.0.pkg_order.Order");
    assertEquals("order:1.0.0.pkg_order.Order", stripped);
}

@Test
void testToFilePath() {
    assertEquals("order/1.0.0/pkg_order/Order.json",
        FqnGenerator.toFilePath("order:1.0.0.pkg_order.Order"));
}
```

### 验证要点

- [x] 所有 6 种实体类型均可生成 FQN
- [x] 解析方法可提取 bundle-code、version、segments、shortName、parentFqn
- [x] 类型前缀可剥离
- [x] 文件系统映射正确

---

## 场景 4: 发布校验 — 升级等级不匹配（User Story 2.4）

### 步骤

```bash
# 基于 v0.0.1 创建草稿 v0.1.0（声明 MINOR）
curl -X POST http://localhost:8080/api/v1/metamodel/bundles/order/versions \
  -H "Content-Type: application/json" \
  -d '{"upgradeLevel": "MINOR"}'
# → order:0.1.0

# 删除草稿中的一个 EntitySchema
curl -X DELETE http://localhost:8080/api/v1/metamodel/entity-schemas/order:0.1.0.pkg_order.Order

# 尝试发布
curl -X POST http://localhost:8080/api/v1/metamodel/versions/order:0.1.0/publish
# 预期: 422, 错误码 30104, 提示 "声明 MINOR 升级但包含元素删除，不匹配"
```

### 验证要点

- [x] 升级等级与变更内容匹配校验正确工作
- [x] 校验失败阻止发布

---

## 场景 5: FQN 版本省略解析（Edge Case）

### 步骤

```bash
# 使用省略版本 FQN 查询（省略版本时按最新已发布版本解析）
curl -X POST http://localhost:8080/api/v1/metamodel/tools/resolve-fqn \
  -H "Content-Type: application/json" \
  -d '{"fqn": "order.pkg_order.Order"}'
# 预期: resolvedFqn = "order:0.0.1.pkg_order.Order"（最新已发布版本）
```

### 验证要点

- [x] 版本省略语法正确解析为最新已发布版本
- [x] 非数字 segment 正确识别为 Package 路径

---

## 运行集成测试

```bash
# 完整集成测试（需 Docker 运行 TestContainers）
mvn test -pl metaforge-metamodel-core -Dtest="*IntegrationTest"

# 单独运行 FQN 生成器单元测试
mvn test -pl metaforge-metamodel-core -Dtest="FqnGeneratorTest"

# 契约测试
mvn test -pl metaforge-metamodel-core -Dtest="*ContractTest"
```

### 预期测试覆盖率

- `FqnGenerator`: 100% 方法覆盖（纯函数，全参数组合测试）
- `AttributeMergeService`: 覆盖正常合并、同名冲突、空模板、空原生属性
- `CircularDependencyDetector`: 覆盖无环、单环、多环、长链、空图
- `UpgradeLevelValidator`: 覆盖全部 10 种变更类型 × 3 级等级组合
