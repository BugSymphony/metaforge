# Export Contract: 认知模板文件格式规范

**发布方**: `metaforge-agent-cognition-templates`
**消费方**: `metaforge-agent-cognition-core`（TemplateScanner / TemplateYamlParser）
**协议类型**: 静态文件格式规范（File Format Contract）
**版本**: 1.0.0

> 本契约定义认知模板 YAML 文件的完整 Schema、字段约束和命名规范。消费方引擎核心的 `TemplateYamlParser` 按此契约解析模板文件，`TemplateRegistry` 按此契约注册和管理模板。

## 一、文件位置与命名

| 规则 | 说明 |
|------|------|
| 文件路径 | `classpath:cognition/templates/*.yml` |
| 文件名 | `{小写templateId}-template.yml` |
| 扩展名 | 仅 `.yml` |
| 编码 | UTF-8 |
| 文件内 templateId | 与文件名一致（不一致时以文件内声明为准） |

## 二、顶层字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `templateId` | string | **是** | 模板唯一标识，大写字母+数字+下划线 |
| `templateName` | string | **是** | 中文显示名 |
| `description` | string | **是** | 模板功能描述 |
| `version` | string | 否 | 语义化版本号 |
| `stage` | string | 否 | 实现阶段: P0/P1/P2 |
| `enabled` | boolean | 否 | 是否启用，默认 true |
| `operators` | array | **是** | 跨分类扁平算子列表 |
| `inputSchema` | object | **是** | 入参定义，JSON Schema Draft 2020-12 |
| `scopeBehavior` | object | **是** | scope 行为声明 |
| `outputSchema` | object | **是** | 输出结构定义 |
| `contextMeta` | object | **是** | context_meta 生成规则 |

## 三、operators 条目字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `operatorId` | string | **是** | 算子 ID（如 `ontological.bundle-discovery`），必须已注册 |
| `priority` | int | 否 | 优先级，数值越大越优先，默认 0 |
| `required` | boolean | **是** | 是否必须成功 |
| `timeoutMs` | int | 否 | 算子单独超时毫秒数 |
| `archetypes` | string[] | **是** | Agent 原型白名单，`{execution, exploration, audit, orchestration}` 的子集 |
| `description` | string | 否 | 可读说明 |

## 四、scopeBehavior 字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `acceptsScope` | boolean | **是** | 是否接受 scope 入参 |
| `scopeRequired` | boolean | **是** | scope 是否必填 |
| `producesUpdatedScope` | boolean | **是** | 是否产出 updated_scope |
| `scopeFields` | string[] | **是** | 生效的 scope 字段: `{bundles, packages, domain_groups, domains, entity_schemas}` 的子集 |
| `description` | string | 否 | scope 行为说明 |

**约束**: `scopeRequired=true` 时 `acceptsScope` 自动视为 `true`。

## 五、outputSchema 字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | string | **是** | 输出结构类型名 |
| `formats` | string[] | **是** | 支持的输出格式，至少包含 `["json", "prompt"]` |

## 六、contextMeta 字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `includeVersionAnchors` | boolean | **是** | 是否包含 version_anchors |
| `includeScopeApplied` | boolean | **是** | 是否包含 scope_applied |
| `includeTokenEstimate` | boolean | **是** | 是否包含 token_estimate |
| `includeSkippedEntities` | boolean | 否 | 是否包含 skipped_entities |

## 七、校验规则

1. 所有必填字段缺失 → 模板无效，不注册
2. `operators` 为空数组 → 模板无效，不注册
3. `operatorId` 引用不存在 → 模板无效，不注册
4. `archetypes` 不在封闭枚举内 → 模板无效，不注册
5. `archetypes` 为空数组 → 模板无效，不注册
6. `inputSchema.required` 引用 `properties` 中不存在的字段 → 模板无效
7. `templateId` 重复 → 第二个被拒绝注册
8. 文件格式非合法 YAML → 跳过并告警
9. 文件编码非 UTF-8 → 跳过并告警

## 八、已注册模板清单

| templateId | 模板名称 | 场景 | 版本 | stage |
|------------|---------|------|------|-------|
| DISCOVER | 元模型发现 | 探索平台元模型结构 | 1.0.0 | P0 |
| ORIENT | 业务域定位 | 业务域内定位与下钻 | 1.0.0 | P0 |
| BRIEF | 任务/实体全景 | 实体或任务全貌 | 1.0.0 | P0 |
| GUIDE | 单步执行指南 | 步骤级精细指导 | 1.0.0 | P0 |
| FORECAST | 变更影响链路 | 变更波及影响评估 | 1.0.0 | P0 |
| DELEGATE | 子任务上下文委派 | 子 Agent scope 收窄 | 1.0.0 | P0 |
