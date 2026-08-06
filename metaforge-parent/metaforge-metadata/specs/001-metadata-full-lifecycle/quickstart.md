# Quickstart: 元数据全生命周期管理

**Feature**: 001-metadata-full-lifecycle
**Date**: 2026-08-01

## 验证场景总览

本文档提供 5 个端到端验证场景，覆盖元数据全生命周期的核心能力。所有验证在本地单机环境下执行，无需额外中间件。

---

## 前置条件

### 环境要求

- Java 21
- Maven 3.9+
- Docker（用于 TestContainers 集成测试）
- PostgreSQL 16（可通过 docker-compose 启动）

### 启动基础设施

```bash
# 1. 启动 PostgreSQL
cd metaforge-parent
docker-compose up -d

# 2. 构建全量模块（含 metadata BC）
mvn clean install -pl metaforge-boot -am -DskipTests

# 3. 启动应用
mvn spring-boot:run -pl metaforge-boot
```

### 模块编译验证

```bash
# 仅编译 metadata BC（含 api + core）
mvn clean compile -pl metaforge-metadata -am

# 运行 metadata BC 单元测试
mvn test -pl metaforge-metadata/metaforge-metadata-core
```

---

## 场景 1: 草稿创建与结构校验（对应 US-1）

### 验证目标
验证草稿创建、JSON Schema 实时校验、FQN 唯一性约束、草稿删除的完整闭环。

### 步骤

**1.1 创建合法草稿**
```bash
curl -X POST http://localhost:8080/api/v1/metadata/drafts \
  -H "Content-Type: application/json" \
  -d '{
    "fqn": "SalesOrder_001",
    "name": "销售订单",
    "entitySchemaFqn": "order:1.0.0.pkg_order.Order",
    "content": {
      "orderId": "SO-123456",
      "customerName": "张三",
      "status": "active"
    }
  }'
```

**期望输出**: `code=200`（平台统一信封响应，HTTP 恒为 200），返回草稿 DTO，`fqn="SalesOrder_001"`，`baseVersion=null`。

**1.2 校验违反 Schema 约束时被拦截**
```bash
curl -X POST http://localhost:8080/api/v1/metadata/drafts \
  -H "Content-Type: application/json" \
  -d '{
    "fqn": "SalesOrder_002",
    "name": "无效订单",
    "entitySchemaFqn": "order:1.0.0.pkg_order.Order",
    "content": {
      "orderId": "INVALID"
    }
  }'
```

**期望输出**: `code=31003`，消息包含违规字段路径 `/orderId`（JSON Pointer 表示，即文档中的 `$.orderId`）、违规类型 `pattern`。

**1.3 FQN 重复冲突**
```bash
curl -X POST http://localhost:8080/api/v1/metadata/drafts \
  -H "Content-Type: application/json" \
  -d '{
    "fqn": "SalesOrder_001",
    "name": "重复订单",
    "entitySchemaFqn": "order:1.0.0.pkg_order.Order",
    "content": {
      "orderId": "SO-999999"
    }
  }'
```

**期望输出**: `code=31001`，消息包含 "FQN 已存在（主表或草稿表）: SalesOrder_001"。

**1.4 删除草稿**
```bash
curl -X DELETE http://localhost:8080/api/v1/metadata/drafts/SalesOrder_001
```

**期望输出**: `code=200`，data=null。

**1.5 确认主表无变更**
```bash
curl http://localhost:8080/api/v1/metadata/entities/SalesOrder_001
```

**期望输出**: `code=31004`（全过程主表无任何变更）。

---

## 场景 2: 草稿生效与原子事务（对应 US-2）

### 验证目标
验证生效原子事务四步骤（主表写入 + 历史表归档 + 草稿表删除 + 事件发布）的完整性。

### 步骤

**2.1 创建草稿并生效**
```bash
# 创建草稿
curl -X POST http://localhost:8080/api/v1/metadata/drafts \
  -H "Content-Type: application/json" \
  -d '{
    "fqn": "SalesOrder_001",
    "name": "销售订单",
    "entitySchemaFqn": "order:1.0.0.pkg_order.Order",
    "content": {"orderId": "SO-123456", "customerName": "张三"}
  }'

# 执行生效
curl -X POST http://localhost:8080/api/v1/metadata/entities/SalesOrder_001/activate
```

**2.2 验证主表存在且为生效版本**
```bash
curl http://localhost:8080/api/v1/metadata/entities/SalesOrder_001
```

**期望输出**: `code=200`，`currentVersion=1`，`content` 与草稿一致。

**2.3 验证历史表归档**
```bash
curl http://localhost:8080/api/v1/metadata/history/SalesOrder_001/versions
```

**期望输出**: `code=200`，`data.content` 数组长度为 1，`version=1`。

**2.4 验证草稿已清除**
```bash
curl http://localhost:8080/api/v1/metadata/drafts/SalesOrder_001
```

**期望输出**: `code=31005`（草稿已删除）。

**2.5 验证版本号递增（第二次生效）**
```bash
# 从生效版本创建修改草稿
curl -X POST http://localhost:8080/api/v1/metadata/drafts/from-active/SalesOrder_001

# 更新草稿内容
curl -X PUT http://localhost:8080/api/v1/metadata/drafts/SalesOrder_001/content \
  -H "Content-Type: application/json" \
  -d '{"content": {"orderId": "SO-123456", "customerName": "李四", "priority": "high"}}'

# 再次生效
curl -X POST http://localhost:8080/api/v1/metadata/entities/SalesOrder_001/activate
```

**期望输出**: `currentVersion=2`，历史表版本列表长度为 2。

---

## 场景 3: 多维度查询检索（对应 US-3）

### 验证目标
验证 FQN 精准查询、FQN 前缀范围查询、元模型类型查询、管理员全状态查询。

### 步骤

**3.1 准备多条测试数据**

先创建并生效 `SalesOrder_001.OrderItem_001`（子实体）、`SalesOrder_001.OrderItem_002`、`OrderReport_010`。
（`SalesOrder_001` 已在场景 2 生效，处于 ACTIVE 状态。）

**3.2 FQN 前缀范围查询（OR 并集）**
```bash
curl "http://localhost:8080/api/v1/metadata/entities/query/fqn-prefix?prefixes=SalesOrder_,OrderReport_&page=1&size=20"
```

**期望输出**: `code=200`，返回 4 条记录（三条 `SalesOrder_` 前缀：`SalesOrder_001`、`SalesOrder_001.OrderItem_001`、`SalesOrder_001.OrderItem_002` + 一条 `OrderReport_` 前缀：`OrderReport_010`），按 FQN 排序。

**3.3 按元模型类型查询**
```bash
curl "http://localhost:8080/api/v1/metadata/entities/query/entity-schema?entitySchemaFqn=order:1.0.0.pkg_order.Order&page=1&size=20"
```

**期望输出**: `code=200`，返回 4 条生效元数据（与 3.2 相同的 4 条）。

**3.4 下线与下线数据查询**
> 注意：`SalesOrder_001` 存在生效子实体（`SalesOrder_001.OrderItem_001`），按"下线前引用校验"会被拦截（`code=31008`），属预期行为。因此本场景用**叶节点** `SalesOrder_001.OrderItem_002` 验证下线闭环。
```bash
# 父实体下线被引用校验拦截（预期行为）
curl -X POST http://localhost:8080/api/v1/metadata/entities/SalesOrder_001/deactivate
# → code=31008（下线被拦截）

# 叶节点正常下线
curl -X POST http://localhost:8080/api/v1/metadata/entities/SalesOrder_001.OrderItem_002/deactivate
# → code=200（下线成功）

# 验证下线后查询返回不存在
curl http://localhost:8080/api/v1/metadata/entities/SalesOrder_001.OrderItem_002
```

**期望输出**: 叶节点下线后查询返回 `code=31004`，"元数据实体不存在或已下线: SalesOrder_001.OrderItem_002"。

**3.5 管理员全状态查询**
```bash
curl "http://localhost:8080/api/v1/metadata/admin/metadata?statuses=DRAFT,ACTIVE,HISTORY&page=1&size=50"
```

**期望输出**: 返回聚合结果，每条标注 `status` 字段（`DRAFT` / `ACTIVE` / `HISTORY`）。

---

## 场景 4: 历史追溯与差异对比（对应 US-4）

### 验证目标
验证全版本列表查询、单版本详情查询、字段级差异对比。

### 步骤

**4.1 全版本列表查询**
```bash
curl http://localhost:8080/api/v1/metadata/history/SalesOrder_001/versions
```

**期望输出**: 数组长度 ≥ 1，按版本号倒序，每条含版本号、生效时间、操作人。

**4.2 单版本详情查询**
```bash
curl http://localhost:8080/api/v1/metadata/history/SalesOrder_001/versions/1
```

**期望输出**: 返回完整 EntityVersionDto，含 `content` 全量快照、`entitySchemaFqn`、`createdTime`。

**4.3 版本差异对比**
```bash
curl -X POST http://localhost:8080/api/v1/metadata/history/diff \
  -H "Content-Type: application/json" \
  -d '{"fqn": "SalesOrder_001", "versionA": 1, "versionB": 2}'
```

**期望输出**: VersionDiffDto，`diffs` 数组按 `diffType`（`ADDED` / `MODIFIED` / `DELETED`）分类展示字段级变更（含 `path`、`oldValue`、`newValue`）。

---

## 场景 5: 批量导入导出（对应 US-6）

### 验证目标
验证 JSON 格式批量导入、导入成功后仅写草稿表、按 FQN 前缀导出、导出格式与导入兼容。

### 步骤

**5.1 批量导入**
```bash
curl -X POST http://localhost:8080/api/v1/metadata/import \
  -H "Content-Type: application/json" \
  -d '{
    "content": "[{\"fqn\":\"SO_001\",\"name\":\"订单1\",\"entitySchemaFqn\":\"order:1.0.0.pkg_order.Order\",\"content\":{\"orderId\":\"SO-000001\"}},{\"fqn\":\"SO_002\",\"name\":\"订单2\",\"entitySchemaFqn\":\"order:1.0.0.pkg_order.Order\",\"content\":{\"orderId\":\"SO-000002\"}}]",
    "format": "JSON",
    "strategy": "SKIP"
  }'
```

**期望输出**: `code=200`，`totalCount=2`、`successCount=2`、`skipCount=0`、`errorCount=0`。

**5.2 验证仅写草稿表（主表无新增）**
```bash
curl http://localhost:8080/api/v1/metadata/entities/SO_001
```

**期望输出**: `code=31004`（导入成功的数据仅写草稿表，主表无该记录）。

**5.3 生效其中一条**
```bash
curl -X POST http://localhost:8080/api/v1/metadata/entities/SO_001/activate
curl http://localhost:8080/api/v1/metadata/entities/SO_001
```

**期望输出**: `code=200`，`currentVersion=1`。

**5.4 导出（按 FQN 前缀）**
```bash
curl -X POST http://localhost:8080/api/v1/metadata/export \
  -H "Content-Type: application/json" \
  -d '{"type": "FQN_PREFIXES", "fqnPrefixes": ["SO_"], "format": "JSON"}'
```

**期望输出**: `code=200`，`entityCount=1`（仅生效的 SO_001），content 格式与导入文件完全兼容。

---

## 运行集成测试

```bash
# 运行 metadata BC 全量集成测试（需要 Docker）
mvn verify -pl metaforge-metadata/metaforge-metadata-core

# 运行特定测试类
mvn test -pl metaforge-metadata/metaforge-metadata-core \
  -Dtest="MetadataDraftServiceTest,MetadataActivationServiceTest"

# 查看测试报告
open metaforge-metadata/metaforge-metadata-core/target/surefire-reports/index.html
```

## 健康检查

```bash
curl http://localhost:8080/actuator/health
```

metadata BC 的自定义健康检查通过 `MetadataHealthCheck`（实现 `HealthCheckSpi`）注册到 Actuator。

## 性能基准验证

| 检测项 | 目标 | 验证方式 |
|--------|------|---------|
| 草稿创建（含校验） | ≤ 50ms | 压测工具 100 次取 P95 |
| FQN 精准查询 | ≤ 20ms | 压测工具 100 次取 P95 |
| 生效原子操作 | ≤ 100ms | 压测工具 50 次取 P95 |
| FQN 前缀范围查询（百级结果） | ≤ 100ms | 准备 100 条数据后单次查询 |
| 批量导入 500 条 | ≤ 5s | 准备 500 条 JSON 导入文件 |
| 全历史版本列表 | ≤ 100ms | 同一 FQN 10 个版本后单次查询 |
