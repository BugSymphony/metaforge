<!--
================================================================================
  Sync Impact Report
================================================================================
  BC Constitution Version Change: 0.0.0 (占位骨架) → 1.0.0
  Parent Global Constitution Version: 1.0.0
  Ratification Date: 2026-08-01

  Principle Summary:
    ✅ Added: I. 纯消费端边界坚守 (MUST)
    ✅ Added: II. 能力优先与模板驱动 (MUST)
    ✅ Added: III. 结构化透传铁则 (MUST)
    ✅ Added: IV. 无状态幂等 (MUST)
    ✅ Added: V. 零解析开销输出 (SHOULD)
    ✅ Added: VI. 认知新鲜度保障 (SHOULD)
    ✅ Added: VII. 可观测与可诊断 (SHOULD)
    ✅ Added: VIII. 配置可治理 (SHOULD)
    ✅ Added: Custom Section - 交付形态与能力投影细则
    ✅ Added: Custom Section - 开发态环境治理细则（.metaforge/）
    ✅ Added: Custom Section - MVP 边界与合规治理

  Override Entries:
    ✅ Added: VI. 合约化双协议标准接口 (SHOULD) — MVP 仅实现 REST 通道，MCP 委托发布后置
    ✅ Added: VIII. Agent 友好型输出 (SHOULD) — 细化为 json/prompt 双格式零解析透传契约

  Deferred TODOs:
    TODO(MCP_DELEGATION): MCP 工具委托发布（cognition_execute 代理）MVP 不实现，
      留待后续迭代按 mcp-tools.md 契约实现，届时必须遵循全局双协议标准。
    TODO(AUTHORIZATION): 导入授权/白名单过滤（SVC-023）MVP 不实现，
      未来接入时必须严格遵循全局 MUST II/III。

  Rationale: metaforge-cli（agent-consumption BC）首个正式宪法。依据 PRD
  （docs/requirements.md）固化纯消费端定位、能力优先与模板驱动、结构化透传、
  无状态幂等等 8 项 BC 专属原则，并针对全局 SHOULD 级原则 VI/VIII 声明两处
  覆盖项（MCP 后置、双格式输出细化）。全部 MUST 级原则继承全局宪法，无违规覆盖。
================================================================================
-->

# agent-consumption Bounded Context Constitution
<!-- BC-level governance constitution. Inherits all rules from the global system constitution as read-only baseline. -->
<!-- OVERRIDE RULE: Only SHOULD/MAY level principles from the parent global constitution can be overridden. MUST level principles CANNOT be modified or removed. -->

**Parent Version**: 1.0.0
<!-- Human-readable traceability marker only. No version alignment validation during AI merge. Records the global constitution version referenced when this BC constitution was created/last updated. -->

---

## BC-Specific Principles
<!-- Exclusive core principles for this BC only, not inherited from the global constitution. Follow MUST/SHOULD/MAY level specification. Principle names must not duplicate global constitution principles. -->

### I. 纯消费端边界坚守 (MUST)

metaforge-cli 是 MetaForge 元认知服务的消费端，仅为 Agent 提供消费元认知服务的
能力。本 BC 不持有任何业务数据主权，不实现服务端侧能力（元建模、元数据管理、
语义关系网络、查询推理）。本 BC 不得直接访问服务端存储，不得绕过服务端接口
构造或改写语义内容；一切语义以服务端 `ApiResponse<T>` 返回为准，本 BC 仅负责
请求构造、传输、响应解析与呈现。

### II. 能力优先与模板驱动 (MUST)

本 BC 以元认知指导能力为需求主体，能力清单固定为 8 项：平台发现、领域导航、
任务认知、实体即时指导、子任务认知（收窄）、自由视角组合查询、影响感知、
认知新鲜度。命令与 Skill 是能力的交付形态，能力与命令解耦——能力存在即命令
可选，不要求"每能力必有命令"。命令清单与服务端模板注册表
（`cognition-templates.yml`）保持声明式投影关系：禁止硬编码模板清单，应从
服务端解析可用模板 ID（如 `TemplateRegistryService.listTemplateIds`），
服务端新增或变更模板时消费端能力须自动跟随，不设固定数量上限。

### III. 结构化透传铁则 (MUST)

所有 REST 调用统一走 `POST /api/v1/cognition/{templateId}`，请求/响应字段必须
与服务端 `CognitionRequest` / `ApiResponse<T>` 完全对齐，原样透传、不做语义
改写。字段命名以服务端实际实现为准（camelCase：`bundleFqns`、`entityFqn`、
`cognitionDepth`、`agentArchetype`、`scopeMode`、`maxTokens` 等）。本 BC 不得
将自然语言文本发送给服务端；NL→结构化参数的转换完全在本 BC 完成，最终发给
服务端的一定是确定型结构化参数。

### IV. 无状态幂等 (MUST)

所有命令与 Skill 必须无状态、可重复执行，不保存任何会话/任务上下文
（沿用服务端无状态设计）。相同参数连续执行的结果必须语义一致，仅允许时间戳、
版本锚等动态字段存在差异。

### V. 零解析开销输出 (SHOULD)

输出必须可直接注入 Agent 上下文，无需大模型二次解析。支持 json（结构化，
默认）与 prompt（Markdown 语义说明）双格式，两种格式语义内容完全一致。
默认模式原样输出服务端返回的 json/prompt，不主动改写语义内容，保证零信息损耗。

### VI. 认知新鲜度保障 (SHOULD)

每次查询输出必须保留/展示 `context_meta.data_version_anchors`（各 Bundle 已
发布版本号 + 查询时间戳）。系统应支持对比前后两次查询的版本锚，某 Bundle
版本变化时提示"认知可能已过期，建议重新获取"。

### VII. 可观测与可诊断 (SHOULD)

本 BC 必须支持透传/接收 `X-Trace-Id` 并支持 `--verbose` 输出原始请求/响应，
便于链路排障。错误处理必须将服务端错误码（34001~34006 及网络错误）映射为
人类可读的简体中文提示，不得将原始错误堆栈直接暴露给 Agent。

### VIII. 配置可治理 (SHOULD)

服务端 base-url、连接/读取超时、默认深度、默认原型、默认 Token 预算必须可配置
（配置文件与环境变量）。配置覆盖优先级固定为：命令 flag > 环境变量 > 配置文件
> 默认值。未配置任何项时采用默认值即可运行（零配置可用）。

---

## 交付形态与能力投影细则
<!-- 细化命令/Skill 命名、必填校验、分页透传与模板注册表投影规则 -->

### 命令与 Skill 命名规范

- 命令统一使用 `metaforge.` 前缀 + 点号命名（如 `metaforge.catalog`、
  `metaforge.task-brief`、`metaforge.step-guide`、`metaforge.subtask`、
  `metaforge.guidance`），对应 Skill 命名与命令一致。
- 命令/Skill 的 front-matter 格式与现有 `.opencode/commands/*.md`
  （speckit 系列）保持一致（`description`、`handoffs` 等字段）。
- Skill 必须自包含：含参数说明、调用示例、输出格式说明，挂载后无需二次开发；
  Skill 内部调用对应 slash 命令，不接受自然语言直达服务端。

### 必填参数校验

`task-brief` 必须要求 `--bundles`；`step-guide` 必须要求 `--entity-fqn`；
`subtask` 必须要求 `--bundles` + `--entry-entity`；`navigate` 必须要求
`--bundles`。缺失时输出用法提示并中止执行。

### 分页透传

列表型输出（catalog / navigate）必须支持 `--page-size` 与游标续翻
（解析 `next_cursor`），透传 `pageSize` / `cursor` 字段。

### 能力↔命令投影约束

命令是模板注册表的声明式投影，禁止手工维护"命令→模板"固定映射表；模板 ID
以 `cognition templates` 输出的服务端实际注册清单为准。

---

## 开发态环境治理细则（.metaforge/）
<!-- 细化开发态环境目录、脚本路由与环境变量规范 -->

### 目录结构

项目根必须维护 `.metaforge/` 目录（与 `.specify/` 共存、职责分离），其下包含
`scripts/` 子目录存放 shell 脚本；不使用 `context.json` 等状态文件。

### 脚本路由规范

脚本入口必须采用"单入口 + 命名空间子命令"路由方式，风格与
`.specify/scripts/bash/speckit-pro.sh` 对齐，至少提供以下子命令：

- `env root`：向上搜索 `.metaforge/` 标记定位项目根，支持 `META_FORGE_ROOT`
  环境变量覆盖。
- `env summary`：输出项目根、服务端地址等 key=value 环境摘要。
- `cognition execute <templateId>`：调用
  `POST {server-url}/api/v1/cognition/{templateId}`，支持全部
  `CognitionRequest` 字段 flag 透传。
- `cognition templates`：列出服务端实际注册的模板 ID。
- `health`：调用 `GET {server-url}/actuator/health` 检查服务端健康状态。

### 环境变量治理

服务端地址通过 `META_FORGE_SERVER_URL` 配置（默认 `http://localhost:8080`）；
连接/读取超时通过 `META_FORGE_CONNECT_MS`、`META_FORGE_TIMEOUT_MS` 配置。
`env summary` 输出环境摘要，便于开发排障。

---

## MVP 边界与合规治理
<!-- 声明 MVP 范围外项与全局 MUST 级合规对齐要求 -->

### MVP 范围外项

- MCP 工具委托发布（`cognition_execute` 代理）MVP 阶段不实现，留待后续迭代
  按 `mcp-tools.md` 契约实现。
- 导入授权/白名单权限过滤（SVC-023）MVP 阶段不在本 BC 实现，不做本地权限拦截。

### 全局 MUST 合规对齐

- 全局 MUST II（显式导入边界管控）、MUST III（全链路权限过滤）由上游服务端
  （agent-cognition BC）强制执行。本 BC 在 MVP 阶段仅透传请求，不得绕过或弱化
  上游已执行的导入边界与权限过滤；未来接入 SVC-023 时，本地授权实现必须以全局
  MUST II/III 为基线，不得制定冲突的本地规则。
- 本 BC 所有原则不得与全局 MUST 级原则（I 元模型唯一权威性、II 显式导入边界、
  III 全链路权限过滤、IV 版本统一收敛、IX 纯元数据边界、X 文档中文规范）冲突。
  本 BC 不持有业务数据、不进行本地持久化，天然满足全局 MUST IX。

---

## BC Overrides
<!-- Selective override of parent global constitution SHOULD/MAY level principles only. Each entry must reference the exact parent principle name + explicit override rationale. MUST level principles are forbidden to be overridden here. -->

### Override 1: VI. 合约化双协议标准接口 (SHOULD)
- **Original Parent Rule**: 全局宪法 VI 要求所有对外能力通过 REST + MCP 双协议
  标准化接口发布，内部实现不对外暴露。
- **Override Content**: MVP 阶段本 BC 仅实现 REST 消费通道
  （`POST /api/v1/cognition/{templateId}`）作为能力交付的唯一协议通道；MCP 工具
  委托发布（`cognition_execute` 代理）延后至后续迭代，待实现时必须按
  `mcp-tools.md` 契约发布并遵循全局双协议标准。
- **Rationale**: 服务端契约 `mcp-tools.md` 已声明 `cognition_execute` 由
  metaforge-consumer 代理发布，但本 BC 的 MVP 定位为 opencode CLI 消费端
  （slash command + Skill 交付），Agent 平台经 opencode 直接挂载调用，暂无 MCP
  消费诉求；故将 MCP 委托发布整体后置，REST 为 MVP 唯一协议通道。

### Override 2: VIII. Agent 友好型输出 (SHOULD)
- **Original Parent Rule**: 全局宪法 VIII 要求所有查询与推理结果输出为低理解成本
  的结构化格式，可直接注入 Agent 上下文，无需大模型二次解析。
- **Override Content**: 本 BC 在满足全局 VIII 的基础上细化输出契约：json
  （结构化，默认）与 prompt（Markdown 语义说明）双格式，语义内容完全一致；默认
  模式原样透传服务端输出，不主动改写语义内容，保证零信息损耗。
- **Rationale**: 本 BC 作为 Agent 消费通道，其输出直接决定 Agent 的认知输入
  质量。全局 VIII 已确立"结构化、可直接注入"基调，本 BC 将其细化为可验证的
  双格式透传契约，明确"零解析、零改写"交付要求，属对该 SHOULD 原则的细化而
  非弱化。

---

**BC Constitution Version**: 1.0.0 | **Created**: 2026-08-01 | **Last Amended**: 2026-08-01
<!-- BC constitution has independent semantic versioning, decoupled from global constitution version. -->
