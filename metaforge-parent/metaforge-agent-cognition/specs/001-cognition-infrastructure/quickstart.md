# Quickstart: 认知基础架构层验证指南

**Feature**: `001-cognition-infrastructure` | **Date**: 2026-08-11

> 本文档提供端到端验证场景，证明认知基础架构层的核心功能闭环可正常工作。
> 实现细节（代码、配置、迁移脚本等）见 `tasks.md`（Phase 2 产出）。

---

## 前置条件

1. **Java 21** + **Maven 3.9+** 已安装
2. `metaforge-boot` 模块已配置 `metaforge-agent-cognition-starter` 依赖
3. 上游 4 个 BC（metamodel-governance, metadata-management, semantic-relation-network, semantic-query-engine）的 api 模块已发布且在本地 Maven 仓库可用
4. PostgreSQL 16 已启动（上游 BC 依赖），但本 BC 不直接使用

---

## 场景 1: 零配置启动验证

**目的**: 验证系统在无 `application-agent-cognition.yml` 时使用默认值正常启动（Step 1 收拢地基：`-dimensions`/`-templates` 暂从构建移除，算子/模板空注册容错）。

### 步骤

1. 确保 `metaforge-boot/src/main/resources/` 目录下不存在 `application-agent-cognition.yml`
2. 启动应用: `mvn spring-boot:run -pl metaforge-boot`
3. 检查启动日志中是否包含以下关键日志行：
   ```
   INFO [agent-cognition] TemplateRegistry initialized: 0 templates registered
   INFO [agent-cognition] OperatorRegistry: 0 operators registered (dimensions module not loaded)
   ```

### 预期结果

- 系统正常启动（无错误日志）
- TemplateRegistry 缓存为空（Step 1 中间态，Step 4 重建 `-templates` 后恢复 6 个内置模板）
- 所有配置使用默认值：`cognition-depth=L2`, `format=json`, `max-tokens=8000`, `timeout=10000ms`

---

> **说明**：场景 2-10 依赖内置模板与算子，需待 Step 3/4（重建 `-dimensions`/`-templates`）后生效。Step 1 阶段核心验证为：api/core/starter 独立编译、零配置启动不崩溃、REST/MCP/health 端点可用（空结果）。

---

## 场景 2: DISCOVER 模板 JSON 格式认知查询

**目的**: 验证统一认知入口 `POST /api/v1/cognition/{templateId}` 的模板路由、算子编排、输出组装全链路。

### 步骤

```bash
curl -X POST http://localhost:8080/api/v1/cognition/DISCOVER \
  -H "Content-Type: application/json" \
  -d '{
    "scope": { "bundles": ["order:1.0.0"] },
    "params": { "parent_fqn": "" },
    "format": "JSON",
    "cognitionDepth": "L2"
  }'
```

### 预期结果

- HTTP 200
- 响应体结构：
  - `data.template` = `"DISCOVER"`
  - `data.contextMeta.versionAnchors` 非空，包含声明的 bundle FQN（如 `["order:1.0.0@latest"]`）
  - `data.contextMeta.tokenEstimate` > 0
  - `data.contextMeta.generatedAt` 非空 ISO-8601 时间戳
  - `data.dimensions` 按分类分组（如 `ontological`/`structural`/`relational`）
  - `data.format` = `"JSON"`

---

## 场景 3: 模板不存在错误

### 步骤

```bash
curl -X POST http://localhost:8080/api/v1/cognition/UNKNOWN \
  -H "Content-Type: application/json" \
  -d '{"params": {}}'
```

### 预期结果

- HTTP 404
- `code` = 34001 (`TEMPLATE_NOT_FOUND`)
- `message` 包含 `"模板 'UNKNOWN' 未注册"`

---

## 场景 4: DELEGATE 模板缺少 scope 错误

### 步骤

```bash
curl -X POST http://localhost:8080/api/v1/cognition/DELEGATE \
  -H "Content-Type: application/json" \
  -d '{"params": {}}'
```

### 预期结果

- HTTP 400
- `code` = 34005 (`MISSING_SCOPE`)

---

## 场景 5: Prompt 格式输出

### 步骤

```bash
curl -X POST http://localhost:8080/api/v1/cognition/BRIEF \
  -H "Content-Type: application/json" \
  -d '{
    "scope": { "bundles": ["order:1.0.0"] },
    "params": { "entity_fqn": "order:1.0.0.pkg_order.Order_001" },
    "format": "PROMPT"
  }'
```

### 预期结果

- HTTP 200
- `data.format` = `"PROMPT"`
- `data.content` 为非空 Markdown 字符串（以 `#` 标题开头）
- `data.dimensions` 为空 Map（`{}`）

---

## 场景 6: Scope 越界实体阻断

### 步骤

```bash
curl -X POST http://localhost:8080/api/v1/cognition/BRIEF \
  -H "Content-Type: application/json" \
  -d '{
    "scope": { "bundles": ["order:1.0.0"] },
    "params": { "entity_fqn": "payment:1.0.0.pkg_payment.Payment_001" }
  }'
```

### 预期结果

- HTTP 403
- `code` = 34004 (`ENTITY_OUT_OF_SCOPE`)
- `message` 包含越界的实体 FQN

---

## 场景 7: 无效 format 参数

### 步骤

```bash
curl -X POST http://localhost:8080/api/v1/cognition/DISCOVER \
  -H "Content-Type: application/json" \
  -d '{"params": {}, "format": "xml"}'
```

### 预期结果

- HTTP 400
- `code` = 34010 (`INVALID_FORMAT`)

---

## 场景 8: 外部模板热加载

### 步骤

1. 在外部模板目录（`${META_FORGE_CONFIG}/cognition/templates/`）中创建一个新 YAML 文件
2. 等待配置的热加载间隔（默认 5 秒）或修改 `poll-interval-ms` 为更短值
3. 调用 `POST /api/v1/cognition/{新模板ID}` 验证新模板可被路由

### 预期结果

- 日志输出: `INFO [agent-cognition] Template registered: {新模板ID}`
- 新模板的认知查询正常返回

---

## 场景 9: 认知算子 SPI 扩展验证

### 步骤

1. 在测试模块中创建一个测试算子类：
   ```java
   @Component
   public class TestOntologicalOperator implements CognitionOperator {
       @Override
       public String operatorId() { return "ontological.test-operator"; }
       @Override
       public DimensionCategory category() { return DimensionCategory.ONTOLOGICAL; }
       @Override
       public CognitionResult execute(CognitionQueryContext ctx) {
           return new CognitionResult("ontological.test-operator", DimensionCategory.ONTOLOGICAL, Map.of("test", true), true, null);
       }
   }
   ```
2. 在模板 YAML 中引用 `ontological.test-operator`
3. 重启应用，调用模板验证算子执行被调用

### 预期结果

- OperatorRegistry 日志: `INFO [agent-cognition] Operator registered: ontological.test-operator (ONTOLOGICAL)`
- 模板执行结果中包含测试算子产出的数据

---

## 场景 10: 声明式扩展场景 — 新增模板零代码变更

### 步骤

1. 创建一个新的 YAML 模板文件（如 `custom-analyze-template.yml`），声明算子清单
2. 放入外部模板目录或 classpath
3. 不修改任何 Java 代码，重启应用（或等待热加载）
4. 调用 `POST /api/v1/cognition/CUSTOM_ANALYZE`

### 预期结果

- 新模板成功注册并路由
- 无 Java 代码变更
- 满足 BC 宪法 II"声明式扩展铁律"

---

## 关键校验清单

| # | 验证项 | 关联场景 |
|---|--------|---------|
| 1 | 零配置启动 | S1 |
| 2 | TemplateRegistry 注册 6 个内置模板 | S1 |
| 3 | POST /api/v1/cognition/{templateId} 路由正确 | S2 |
| 4 | ContextMeta 包含版本锚、scope、Token估算、时间戳 | S2 |
| 5 | 无效 templateId → 34001 + 404 | S3 |
| 6 | 缺少 scope → 34005 + 400 | S4 |
| 7 | Prompt 格式输出 Markdown | S5 |
| 8 | Scope 越界阻断 → 34004 + 403 | S6 |
| 9 | 无效 format → 34010 + 400 | S7 |
| 10 | 外部模板热加载正常 | S8 |
| 11 | 认知算子 SPI 加载与执行 | S9 |
| 12 | 声明式新增模板零代码变更 | S10 |
| 13 | MCP Tool `cognition_execute` 可用（需 MCP 客户端测试） | 独立 |
