# Data Model: metaforge-cli 元认知指导能力

**阶段**: Phase 1（Design & Contracts）| **日期**: 2026-08-01
**关联**: [spec.md](spec.md) §Key Entities / 上游契约 `rest-api.md` v1.1.0

> **说明**: 本产品**不持有任何业务数据主权、无本地持久化**（BC 宪法 I/IV、FR-015）。以下为命令/Skill 层的**逻辑实体模型**（CLI 内存态结构），对应 spec 的 Key Entities，非数据库/存储模型。服务端返回的语义内容为唯一权威，本模型仅用于脚本层的字段映射与校验。

## 实体总览

| 实体 | 类型 | 生命周期 | 说明 |
|---|---|---|---|
| CognitionCapability | 逻辑能力 | 静态（8 项固定） | 需求主体，命令/Skill 是其投影 |
| TemplateDefinition | 运行时获取 | 每次会话拉取 | 服务端注册表动态解析，不持久化 |
| CommandDefinition | 静态定义 | 随交付物发布 | opencode 命令 front-matter + 绑定模板 |
| SkillDefinition | 静态定义 | 随交付物发布 | SKILL.md 自包含定义 |
| CognitionRequest | 请求结构 | 单次调用 | 命令 flag → 结构化请求字段 |
| CognitionResponse | 响应结构 | 单次调用 | `ApiResponse<T>` + 视角章节 |
| DataVersionAnchor | 响应片段 | 单次调用 | 版本锚，用于过期判定 |
| FqnCandidate | 中间产物 | 单次推测流程 | FQN 推测候选，不持久化 |
| Config | 本地配置 | 读取即用 | `~/.config/metaforge/config.yml` + env |

## 实体详述

### CognitionCapability（元认知指导能力）

- **字段**: `id`（8 项固定：platform-discovery / domain-navigation / task-cognition / entity-guidance / sub-task-cognition / free-perspective-composition / impact-awareness / cognition-freshness）、`guidesQuestion`、`guidesBehavior`
- **关系**: 每个能力可对应 0..* 命令投影；能力存在即命令可选（FR-CAP/FR-DLV-001）
- **校验规则**: 8 项能力必须全部可交付（FR-001）；能力清单不随模板数量变化（固定 8 项）

### TemplateDefinition（模板定义，运行时）

- **字段**: `templateId`（string）、`perspectives`（string[]）、`depthTrim`、`agentArchetype`、`scope`（BUNDLE/ENTITY/BOTH）
- **来源**: 服务端模板注册表，经 `cognition templates` 动态解析（FR-002）；**不硬编码**（BC 宪法 II）
- **校验规则**: 未注册 templateId → 34001 中文提示（FR-018）；未知模板引导用户从实际清单选择
- **已知实例**: `bundle-catalog` / `cognition-guidance` / `task-brief` / `step-guide` / `navigate` / `sub-task-brief`（以运行时为准，D2）

### CommandDefinition（命令定义，交付形态）

- **字段**: `commandId`（`metaforge.` 前缀，如 `metaforge.task-brief`）、`templateId`（绑定）、`flagMap`（flag→请求字段映射）、`requiredFlags`（必填校验）、`help`
- **关系**: 是能力的声明式投影；数量不设固定上限（FR-DLV-002）
- **校验规则（FR-014）**: `task-brief` 必填 `--bundles`；`step-guide` 必填 `--entity-fqn`；`subtask` 必填 `--bundles`+`--entry-entity`；`navigate` 必填 `--bundles`；缺失 → 用法提示并中止
- **已知实例**: catalog / navigate / task-brief / step-guide / subtask / guidance（bundle-scope 不提供，R3）

### SkillDefinition（Skill 定义，交付形态）

- **字段**: `skillId`（`metaforge-` 前缀，匹配目录名）、`boundCommand`（绑定的 slash 命令）、`parameterDocs`、`examples`、`outputFormat`
- **校验规则**: 自包含（挂载即用，FR-017）；内部调用对应命令，不直接承载通信细节（FR-DLV-008/009）
- **命名约束**: SKILL.md `name` 须小写字母数字+单连字符且匹配目录名（D9）

### CognitionRequest（认知请求，透传）

- **字段**（camelCase，D1）: `templateId`（路径参数）、`bundleFqns`（string[]，多数模板必填）、`entityFqn`、`entityTypes`、`subjectDomainFqn`、`scopeMode`（INHERITED/PURE）、`cognitionDepth`（L1/L2/L3，默认 L2）、`agentArchetype`（execution/exploration/audit/orchestration，默认 execution）、`maxTokens`（默认 8000）、`expand`（lazy/all，默认 lazy）、`format`（json/prompt，默认 json）、`perspectives`（string[]，自由组合入口）、`contextParameters`（object）、`cursor`、`pageSize`
- **校验规则**: 不得包含自然语言文本（FR-010）；最终发给服务端的必须是确定型 FQN（FR-NL-005）
- **来源**: 命令 flag 转换（FR-DLV-003）或 `--json` 原样覆盖

### CognitionResponse（认知响应，透传）

- **结构**: `code` / `message` / `data` / `traceId`（ApiResponse<T>）；`data.context_meta`（templateId/contextMode/dataVersionAnchors/truncatedPerspectives/skippedPerspectives）+ `data.perspectives`（各视角章节）
- **校验规则（FR-REST-003）**: `code=200` 视为成功，其余进入错误映射（FR-018）；`format=prompt` 时正文为 Markdown
- **处理**: json 原样透传（FR-009）；prompt 直接可注入（FR-008）

### DataVersionAnchor（版本锚）

- **字段**: `bundle`、`publishedVersion`、`queriedAt`
- **形态兼容**: 契约示例为 map（`{bundle: {version, queriedAt}}`），mock 为 array——两种均需解析（R4，D8）
- **用途**: 保留展示（FR-VER-001）；对比前后两次判定过期（FR-VER-002，对比由调用方完成）

### FqnCandidate（FQN 候选）

- **字段**: `fqn`、`name`、`description`、`entitySchemaFqn`、`matchMethod`（exact/name/keywords-aliases/substring）
- **用途**: 唯一命中自动确认；多候选列候选请用户选择；零命中终止并给原因（FR-NL-004）
- **校验规则**: 推测必须基于服务端数据，严禁臆测（FR-NL-002）；不引入模糊检索（FR-NL-003）

### Config（配置）

- **字段**: `server.baseUrl`（默认 `http://localhost:8080`）、`server.timeoutMs`（连接 3000 / 读取 10000）、`default.depth`（L2）、`default.archetype`（execution）、`default.maxTokens`（8000）
- **来源与优先级（Q3/D5）**: 命令 flag > 环境变量（`META_FORGE_*`）> 用户文件 `~/.config/metaforge/config.yml` > 默认值
- **校验规则**: 零配置可用（NFR-004）；凭据不落盘（FR-024）

## 状态与流转

CLI 侧为**无状态**设计，唯一的"流转"是单次请求的内部流水线（不持久化）：

```text
命令调用
  → 参数解析/必填校验（FR-014）       [失败 → 用法提示中止]
  → 配置合并（flag>env>config>默认）  [FR-020]
  → NL→结构化转换 + FQN 推测（如需）  [FR-NL] 唯一→继续 / 多候选→用户选择 / 零命中→终止
  → REST 调用（.metaforge/scripts/）  [FR-DLV-009] 瞬时故障重试 1 次（FR-025）
  → 响应解析（code=200 成功）         [FR-REST-003] 错误→中文映射（FR-018）
  → 输出（json/prompt 原样 + 版本锚） [FR-OUT/FR-VER]
```

## 校验规则汇总（可测试断言）

1. CognitionRequest 所有字段与上游契约对齐（camelCase），无自然语言文本（FR-010/023）
2. 必填校验：task-brief/--bundles、step-guide/--entity-fqn、subtask/--bundles+--entry-entity、navigate/--bundles（FR-014）
3. 错误映射：34001/34002/34003/34004/34005/34006/网络错误 → 中文提示（FR-018）
4. FQN 匹配确定性优先级：精确 > 名称 > keywords/aliases > 子串；零命中终止（FR-012）
5. 版本锚无条件展示；兼容 map/array 形态（FR-VER-001，R4）
