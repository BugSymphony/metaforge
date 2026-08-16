# Quickstart: 认知模板配置层

**Feature**: cognition-templates | **Phase**: 1 | **Date**: 2026-08-11

## 验证场景

本快速指南提供模板配置部署后的端到端验证场景。所有验证依赖引擎核心 (`metaforge-agent-cognition-core`) 已正确配置并启动。

### 前置条件

1. 模板模块已构建并通过 starter 引入运行时 classpath
2. PostgreSQL 数据库已初始化（引擎核心依赖）
3. `metaforge-boot` 应用正常启动

### 场景 1: 模板扫描与注册验证

**目标**: 验证 6 个内置模板全部被 TemplateScanner 发现并注册到 TemplateRegistry。

**步骤**:
1. 启动 `metaforge-boot` 应用
2. 查看启动日志，确认无模板解析错误

**预期结果**:
- 日志显示 6 个模板文件被扫描发现：`discover-template.yml`, `orient-template.yml`, `brief-template.yml`, `guide-template.yml`, `forecast-template.yml`, `delegate-template.yml`
- 所有模板校验通过，无告警日志
- TemplateRegistry 包含 6 个条目，templateId 分别为 `DISCOVER`, `ORIENT`, `BRIEF`, `GUIDE`, `FORECAST`, `DELEGATE`

### 场景 2: DISCOVER 模板 — 全平台 Bundle 发现

**目标**: 验证 DISCOVER 模板的算子编排正确，返回全平台 Bundle 列表。

**请求**:
```
POST /api/v1/cognition/DISCOVER
Content-Type: application/json

{
  "params": {
    "parent_fqn": null
  },
  "format": "json"
}
```

**预期结果** (关键字段):
- HTTP 200
- `contextMeta.template` = `"DISCOVER"`
- `dimensions` 包含 `ontological`, `structural` 分类结果
- 不含 `scope_applied`（scope 未提供时 scopeRequired=false 正常）

### 场景 3: DELEGATE 模板 — scope 必填校验

**目标**: 验证 DELEGATE 的 `scopeRequired: true` 约束生效。

**请求**:
```
POST /api/v1/cognition/DELEGATE
Content-Type: application/json

{
  "params": {
    "entity_fqn": "Step_CheckInventory"
  }
}
```

**预期结果**:
- HTTP 400（或对应错误状态）
- 错误码 34005 (MISSING_SCOPE)

### 场景 4: BRIEF 模板 — 全貌查询

**目标**: 验证 BRIEF 模板 8 个算子按 priority 编排，返回 5 类结果。

**请求**:
```
POST /api/v1/cognition/BRIEF
Content-Type: application/json

{
  "params": {
    "entity_fqn": "Task_InventoryCheck"
  },
  "scope": {
    "bundles": ["order:1.0.0"],
    "domains": ["Domain_Inventory"]
  },
  "format": "json"
}
```

**预期结果**:
- `dimensions` 包含 `entity_profile` + `flow_blueprint` + `constraint_set` + `capability_catalog` + `relationship_graph`
- `contextMeta.includeSkippedEntities` = `true`
- scope 外关联实体被标注在 `skipped_entities` 中

### 场景 5: FORECAST 模板 — 影响链路

**目标**: 验证 FORECAST 的关系图遍历算子正确。

**请求**:
```
POST /api/v1/cognition/FORECAST
Content-Type: application/json

{
  "params": {
    "entity_fqn": "Rule_InventoryAboveZero",
    "direction": "both",
    "max_depth": 3
  },
  "format": "json"
}
```

**预期结果**:
- 返回 `forward_impact` + `backward_dependency` + `impact_paths`
- 遇到 scope 外 Bundle 实体时截断标注

### 场景 6: 模板校验失败隔离

**目标**: 验证非法模板不影响已注册模板。在 classpath 中临时放入一个非法模板文件（如 operators 为空的 `invalid-template.yml`），重启验证。

**预期结果**:
- 6 个合法模板正常注册并可消费
- 非法模板仅产生告警日志，不污染注册表
- DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE 全部可路由

### 场景 7: archetype 过滤

**目标**: 验证 archetype 白名单生效。

**请求**:
```
POST /api/v1/cognition/DISCOVER
Content-Type: application/json

{
  "archetype": "audit",
  "params": {
    "parent_fqn": null
  }
}
```

**预期结果**:
- 仅执行 `archetypes` 包含 `audit` 的算子（DISCOVER 的 `ontological.bundle-discovery` 声明了 `audit`，正常执行返回 Bundle 层）

## 构建命令

```bash
# 从仓库根目录构建
mvn clean package -pl metaforge-parent/metaforge-agent-cognition/metaforge-agent-cognition-templates

# 构建整个 cognition 模块（含 templates）
mvn clean package -pl metaforge-parent/metaforge-agent-cognition -am

# 启动应用
mvn spring-boot:run -pl metaforge-boot
```
