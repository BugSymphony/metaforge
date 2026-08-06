# Research: metaforge-cli 元认知指导能力

**阶段**: Phase 0（Outline & Research）| **日期**: 2026-08-01
**关联**: [plan.md](plan.md) / [spec.md](spec.md) / 上游契约 `context/upstream-contracts/server-agent-cognition/rest-api.md` v1.1.0

## 研究范围

Spec 无遗留 [NEEDS CLARIFICATION]（已在 clarify 阶段全部消解：认证=无认证、性能=引用服务端预算+CLI 开销<5%、配置=用户级文件+env、重试=瞬时故障 1 次）。本文件记录**实现前必须定案的技术决策**，来源为上游契约、spec、BC 宪法与仓库既有惯例。

## 决策清单

### D1: 字段命名约定（R1 风险应对）

- **Decision**: 请求/响应字段按服务端**实际实现 camelCase**（`bundleFqns`、`entityFqn`、`cognitionDepth`、`agentArchetype`、`scopeMode`、`maxTokens`、`contextParameters`、`pageSize`、`cursor`、`nextCursor`、`dataVersionAnchors`）发送与解析。
- **Rationale**: 契约文档 `rest-api.md` 示例为 snake_case，但 spec FR-REST-002 与 BC 宪法 III 明确"以服务端实际实现为准"，且既有 `test/cognition-agent-test.sh` 与 quickstart 均按 camelCase 验证。
- **Alternatives considered**: 按契约文档 snake_case 发送——被拒：与实际实现不符，必然 400 错误；同时做双命名兼容——被拒：MVP 增加复杂度，交给上游统一契约后收敛。
- **风险标注**: 契约文档与实现命名不一致（R1），需与上游 `metaforge-agent-cognition` 核对统一；实现时以真实响应为权威。

### D2: 模板清单来源（FR-CAP-002 模板驱动）

- **Decision**: 命令/Skill 绑定模板 ID 不硬编码；脚本提供 `cognition templates` 子命令，从服务端注册表动态解析可用模板 ID。
- **Rationale**: BC 宪法 II 要求"命令是模板注册表的声明式投影，禁止硬编码模板清单"；服务端新增/变更模板时消费端能力自动跟随。
- **Alternatives considered**: 本地维护模板→命令映射表——被拒：违反模板驱动原则，模板变化需人工同步。
- **注意**: 契约文档内置模板表列出 5 个模板（缺 `sub-task-brief`），而 spec 依据服务端实际注册为 6 个（含 `sub-task-brief`）。以 `cognition templates` 运行时输出为准；`sub-task-brief` 按 spec 保留默认命令 `metaforge.subtask`。`bundle-scope`（bundleScope）服务端未注册，不提供独立命令，能力经 `cognition-guidance` 组合 `schema_inventory` + `instance_catalog` 视角达成（R3）。

### D3: FQN 推测数据源与流水线（FR-NL-001~005）

- **Decision**: FQN 推测仅基于服务端认知接口返回数据，采用确定型流水线：
  1. 识别目标类型（Bundle / 主题域 / 实体）；
  2. Bundle → `bundle-catalog`（bundle_directory+domain_navigation）；主题域/任务 → `navigate`（domain_navigation）；实体 → `cognition-guidance` 组合 `schema_inventory` + `instance_catalog`；
  3. 匹配优先级：精确 FQN > 名称精确 > keywords/aliases > 子串；
  4. 唯一命中→自动确认；多候选→列候选请用户选择；零命中→终止并给原因与平台现有清单；兜底用 34004 候选列表。
- **Rationale**: 服务端宪法"不接受自然语言"；严禁凭空臆测 FQN（FR-NL-002/003/004）。
- **Alternatives considered**: 引入模糊检索/相似度匹配——被拒：FR-NL-003 明确不引入，保证可解释、可预测、可审计。

### D4: 通信层封装（FR-DLV-009 / FR-DEV-001）

- **Decision**: 全部 REST 调用（`POST /api/v1/cognition/{templateId}`、`GET /actuator/health`）由 `.metaforge/scripts/metaforge-pro.sh`（单入口 + 命名空间子命令）承载；命令/Skill 文件仅调用脚本入口，不出现 REST URL/HTTP 方法/curl。
- **Rationale**: REST 细节（端点、请求体、超时、trace 头）集中一处维护；命令文件保持声明式、可读、不向大模型暴露凭据与接口细节。
- **Alternatives considered**: 命令文件内嵌 curl——被拒：违反 FR-DLV-009，泄露接口细节；用独立编程语言实现——被拒：与仓库脚本惯例不符，引入运行时依赖。

### D5: 配置加载与覆盖（FR-CFG / Q3）

- **Decision**: 配置源为 用户级文件 `~/.config/metaforge/config.yml` + 环境变量 `META_FORGE_*` + 命令 flag；覆盖优先级 flag > env > config > 默认。默认值：base-url `http://localhost:8080`、depth L2、archetype execution、max-tokens 8000、连接超时 3s、读取超时 10s、page-size 20、format json、expand lazy。
- **Rationale**: 零配置可用（NFR-004/FR-020）；环境变量命名统一 `META_FORGE_SERVER_URL`、`META_FORGE_CONNECT_MS`、`META_FORGE_TIMEOUT_MS`（FR-DEV-004），与 spec FR-020（Q3）一致。
- **Alternatives considered**: 仅环境变量——被拒：可配置项多时管理不便；项目级配置文件——被拒：与"开发态环境仅承载脚本"职责分离冲突。

### D6: 认证（Q1 澄清）

- **Decision**: MVP 无传输层认证，直连服务端；任何场景不得硬编码凭据（FR-024）。不预留认证头逻辑（YAGNI），未来需要时经环境变量引入。
- **Rationale**: 用户澄清明确选择"无认证"；MVP 范围外不实现授权白名单（SVC-023），授权由上游强制执行。
- **Alternatives considered**: env 令牌（B 选项）、配置文件凭据（C）、TLS+令牌（D）——均被用户以 A 否决。

### D7: 瞬时故障重试（FR-025 / Q4）

- **Decision**: 对网络错误、上游不可用（34006）、单视角超时（34005）自动重试 1 次；重试后仍失败给中文提示。无指数退避、无并发重试。
- **Rationale**: 用户澄清选择"有限自动重试 1 次"；无状态幂等保证重试安全（FR-015）；不无限阻塞 Agent。
- **Alternatives considered**: 不重试（A）、可配置重试次数（C）、指数退避（D）——均被用户以 B 否决。

### D8: 输出处理（FR-OUT / FR-VER）

- **Decision**: 默认 `format=json` 原样透传；`--format prompt` 输出 Markdown 供直接注入。`context_meta.data_version_anchors` 无条件保留展示；`truncated_perspectives`/`skipped_perspectives` 转提示展示（FR-019）。版本锚形态兼容 map（契约示例）与 array（mock）两种（R4）。
- **Rationale**: 零解析开销（FR-OUT-003）、零改写（FR-OUT-002）、新鲜度保障（FR-VER-001/002）。
- **Alternatives considered**: CLI 侧重组/美化输出——被拒：违反原样透传零信息损耗原则。

### D9: opencode 命令/Skill 装载格式（NFR-005）

- **Decision**: 命令文件 `.opencode/commands/metaforge.*.md`，front-matter 含 `description`、`handoffs`（与 speckit 系列一致）；正文含 `$ARGUMENTS` 占位符，脚本经 `!`bash`` 注入或直接调用说明。Skill 文件 `.opencode/skills/metaforge-*/SKILL.md`，front-matter 含 `name`/`description`（+`metadata`），`name` 须匹配目录名且小写字母数字+单连字符。
- **Rationale**: opencode 官方装载规范；与既有 `.opencode/commands/speckit.*.md` 保持格式一致（NFR-005）。
- **Alternatives considered**: opencode.json 配置内嵌命令——被拒：与仓库 markdown 文件惯例不一致，可读性差。

## 风险摘要

| 风险 | 等级 | 处置 |
|---|---|---|
| R1 契约 snake_case vs 实现 camelCase | 高 | 按实现 camelCase 发送；与上游核对统一（D1） |
| R2 业务语义上料未完全就绪 | 高 | 验收用例用 mock `docs/cognition2/mock/order-bundle-m1.json` 验证链路 |
| R3 模板注册表差异（契约 5 vs 实际 6） | 中 | 命令为模板声明式投影，动态解析；`sub-task-brief` 按 spec 保留（D2） |
| R4 data_version_anchors 形态 map/array 不一致 | 中 | 兼容两种形态解析（D8） |
| R6 opencode 版本差异导致 Skill 挂载格式差异 | 低 | 与 speckit 命令保持同构格式（D9） |
| R7 能力与命令解耦不彻底 | 中 | 以 spec §4.1 能力清单为验收主体，命令仅作交付形态验证 |
