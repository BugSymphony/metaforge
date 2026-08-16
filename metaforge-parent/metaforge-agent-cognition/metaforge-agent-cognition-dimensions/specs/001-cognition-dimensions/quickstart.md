# Quickstart: 认知算子实现层

**Feature**: 001-cognition-dimensions | **Date**: 2026-08-11

## 前置条件

- Java 21 + Maven 3.9+
- `metaforge-agent-cognition-api` 模块已编译（`mvn install -pl metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-api`）
- 4 个 Port 接口已由 `-core` 的 `infrastructure/adapter` 提供运行时实现

## 模块注册

### 1. 注册到父 POM

在 `metaforge-parent/metaforge-agent-cognition/pom.xml` 的 `<modules>` 中添加：

```xml
<module>metaforge-agent-cognition-dimensions</module>
```

### 2. 创建模块 POM

`metaforge-agent-cognition-dimensions/pom.xml`：

```xml
<parent>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition</artifactId>
    <version>${revision}</version>
</parent>
<artifactId>metaforge-agent-cognition-dimensions</artifactId>
<dependencies>
    <dependency>
        <groupId>com.metaforge</groupId>
        <artifactId>metaforge-agent-cognition-api</artifactId>
    </dependency>
</dependencies>
```

### 3. 集成到 Starter

在 `metaforge-agent-cognition-starter/pom.xml` 的 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-agent-cognition-dimensions</artifactId>
</dependency>
```

## 验证场景

### 场景 1：算子编译与注册

```bash
# 编译整个 cognition 模块
mvn compile -pl metaforge-parent/metaforge-agent-cognition -am

# 验证：启动应用，检查 OperatorRegistry 日志是否包含 25 个算子
# 预期日志输出：OperatorRegistry registered 25 operators across 8 categories
```

### 场景 2：本体论全链路（单元测试）

```bash
mvn test -pl metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-dimensions \
  -Dtest="OntologicalOperatorsTest"
```

**预期结果**: `ontological.bundle-discovery` → 返回 Bundle 列表（lazy 节点）；`ontological.entity-profile` → 按 FQN 返回实体画像。

### 场景 3：结构论算子（单元测试）

```bash
mvn test -pl metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-dimensions \
  -Dtest="StructuralOperatorsTest"
```

**预期结果**: `structural.decomposition` FORWARD 方向返回子树；`structural.belonging` BACKWARD 方向返回父链。

### 场景 4：关系论算子（单元测试）

```bash
mvn test -pl metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-dimensions \
  -Dtest="RelationalOperatorsTest"
```

**预期结果**: `relational.direct-link` 返回按 AssociationType 分组的 1 度关联；`relational.impact-trace` 返回正向波及链 + 反向溯源链。

### 场景 5：流程论算子（单元测试）

```bash
mvn test -pl metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-dimensions \
  -Dtest="ProceduralOperatorsTest"
```

**预期结果**: `procedural.flow-blueprint` 返回最长路径序列含 ENTRY/DECISION/EXIT。

### 场景 6：约束论 + 能力论算子（单元测试）

```bash
mvn test -pl metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-dimensions \
  -Dtest="DeonticOperatorsTest,CapabilityOperatorsTest"
```

**预期结果**: `deontic.level-classifier` 返回 MANDATORY/RECOMMENDED 分类；`capability.call-method` 识别 REST/MCP/CLI/LocalMethod。

### 场景 7：认知论 + 治理算子（单元测试）

```bash
mvn test -pl metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-dimensions \
  -Dtest="EpistemicOperatorsTest,GovernanceOperatorsTest"
```

**预期结果**: `epistemic.freshness-check` 返回 version_anchors；`governance.scope-narrowing` 返回三层收窄结果。

### 场景 8：所有测试

```bash
mvn test -pl metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-dimensions
```

**预期结果**: 全部 25 个算子的单元测试通过，覆盖率 ≥ 80%。

## 常见问题排查

| 问题 | 原因 | 解决 |
|------|------|------|
| 启动时算子在 OperatorRegistry 中不可见 | `-dimensions` 模块未注册到 `-starter` 或未在 classpath | 确认 `-starter/pom.xml` 含 `-dimensions` 依赖 |
| 算子 category 校验失败 | 类上 category 字段与 category() 返回值不一致 | 检查 category 字段与 category() 返回值是否一致 |
| Port 注入为 null | 运行时 Port adapter Bean 不存在 | 确保 `-core` 模块在 classpath，Port 适配器已注册为 Spring Bean |
