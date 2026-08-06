---

description: "Task list for metaforge-cli 元认知指导能力（Agent 消费接入）"
---

# Tasks: metaforge-cli 元认知指导能力（Agent 消费接入）

**Input**: Design documents from `/specs/001-metaforge-cli-consumption/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/script-cli.md, contracts/opencode-delivery.md, quickstart.md

**Tests**: 仓库既有 `test/cognition-agent-test.sh`（REPO_ROOT 下）作为集成验证载体，每个用户故事对应一组可独立验证的验收场景（见各 Phase Independent Test），不采用 TDD（测试非前置）。

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1-US7, per spec.md)
- Include exact file paths in descriptions

## Path Conventions

- **REPO_ROOT**: `/data/ext/source-8/metaforge`（opencode 仓库根，`.metaforge/`、`.opencode/` 所在）
- **BC_PATH**: `/data/ext/source-8/metaforge/metaforge-cli`（本 BC 治理资产：context/docs/specs）
- 本任务所有路径均为 REPO_ROOT 相对路径（脚本/命令/Skill 交付物位于 REPO_ROOT，遵循仓库既有惯例）
- 上游契约（只读对接）: `metaforge-cli/context/upstream-contracts/server-agent-cognition/rest-api.md`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 项目初始化与基础结构（`.metaforge/` 开发态环境骨架 + opencode 交付目录）

- [X] T001 创建 `.metaforge/scripts/`、`.metaforge/scripts/lib/`、`.metaforge/scripts/modules/` 目录结构（FR-DEV-001）
- [X] T002 创建 `.opencode/commands/`、`.opencode/skills/` 目录（确认与 speckit 系列共存，NFR-005）
- [X] T003 [P] 创建 `metaforge-pro.sh` 单入口脚本骨架（namespace 路由：env / cognition / health，风格对齐 `.specify/scripts/bash/speckit-pro.sh`，FR-DEV-002/005）

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 全部用户故事共用的基础能力——配置、REST 通信、错误映射、模板动态解析、FQN 推测、开发态环境命令。MUST 完成后方可开始任何用户故事。

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 实现 `lib/config.sh`：配置合并（flag > env > config > 默认），默认值（base-url `http://localhost:8080`、depth L2、archetype execution、max-tokens 8000、connect 3000ms、read 10000ms、page-size 20、format json、expand lazy），读取 `~/.config/metaforge/config.yml` 与 `META_FORGE_SERVER_URL`/`META_FORGE_CONNECT_MS`/`META_FORGE_TIMEOUT_MS`（FR-CFG-001/002/003、Q3、D5）
- [X] T005 [P] 实现 `lib/http.sh`：REST 通信唯一承载——curl 封装 `POST {base-url}/api/v1/cognition/{templateId}`、camelCase 请求体构造、连接/读取超时、`X-Trace-Id` 透传、瞬时故障自动重试 1 次（网络错误/34005/34006）、`ApiResponse<T>` 解析（code=200 成功）、`data_version_anchors` map/array 双形态兼容（FR-REST-001~005、FR-025、FR-DLV-009、D1/D8/R4）
- [X] T006 [P] 实现 `lib/errors.sh`：错误码 34001~34006 + 网络错误 → 简体中文提示映射；`--verbose` 输出原始请求/响应与 traceId（FR-ERR-001、FR-018/021、NFR-006/007）
- [X] T007 [P] 实现 `lib/fqn-resolve.sh`：FQN 推测流水线——目标类型识别（Bundle/主题域/实体）→ 调用 `bundle-catalog`/`navigate`/`cognition-guidance(schema_inventory+instance_catalog)` 获取候选 → 确定型匹配（精确 FQN > 名称 > keywords/aliases > 子串）→ 唯一命中确认/多候选列候选/零命中终止并给原因与平台清单；严禁臆测，不引入模糊检索（FR-NL-001~005、FR-011/012、D3）
- [X] T008 [P] 实现 `modules/env.sh`：`env root`（向上搜索 `.metaforge/` 标记，`META_FORGE_ROOT` 覆盖）与 `env summary`（key=value 环境摘要）（FR-DEV-002、FR-DEV-004）
- [X] T009 [P] 实现 `modules/health.sh`：`health` 子命令——`GET {base-url}/actuator/health`，输出 HEALTH OK / FAIL + 中文原因（FR-DEV-002）
- [X] T010 实现 `modules/cognition.sh` 的 `cognition templates` 子命令：动态解析服务端实际注册模板 ID（不硬编码模板清单，FR-CAP-002、D2/R3），`cognition execute` 的子命令路由骨架与 flag→`CognitionRequest` 字段映射表（camelCase，FR-DEV-002/003、FR-DLV-003、D1）

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Agent 获取任务认知简报 (Priority: P1) 🎯 MVP

**Goal**: Agent 以自然语言描述目标任务，CLI 完成 NL→结构化转换 + FQN 推测，获取 task-brief 认知简报并原样透传（json/prompt），交付 `metaforge.task-brief` 命令与 `metaforge-task-brief` Skill。

**Independent Test**: 调用 `metaforge.task-brief`（或 `cognition resolve` + `cognition execute task-brief`），以自然语言"订单处理任务"描述，返回含约束/流程/能力/决策分支的结构化简报，全程未向服务端发送自然语言文本；多候选列候选、零命中终止并给原因；服务不可达给中文提示。

### Implementation for User Story 1

- [X] T011 [P] [US1] 创建 `.opencode/commands/metaforge.task-brief.md`：front-matter（`description` 简体中文 + `handoffs`）与正文（用途/参数/执行方式经 `metaforge-pro.sh cognition execute task-brief`/输出说明），不含任何 REST URL/curl（FR-DLV-001/009、NFR-005）
- [X] T012 [P] [US1] 创建 `.opencode/skills/metaforge-task-brief/SKILL.md`：自包含（用途/参数/调用示例/输出格式），`name` 匹配目录名（FR-DLV-007/008、D9）
- [X] T013 [US1] 实现 `modules/cognition.sh` 的 `cognition execute task-brief`：`--bundles` 必填校验（缺失→用法提示退出码 2）、`--depth/--archetype/--max-tokens/--format` 透传、json 原样输出 + prompt Markdown 输出（FR-014/008/009、FR-REST-002、D1/D8）
- [X] T014 [US1] 实现 `modules/cognition.sh` 的 `cognition resolve <描述>` 子命令：接入 `lib/fqn-resolve.sh`，支持 Bundle 推测与自然语言→`--bundles` 转换后继续执行 task-brief（FR-NL、FR-DLV-004）
- [X] T015 [US1] 在 `test/cognition-agent-test.sh` 增加任务认知场景：NL 描述→简报返回、多候选列候选、零命中终止、服务不可达中文提示（复用 mock `docs/cognition2/mock/order-bundle-m1.json`）

**Checkpoint**: User Story 1 独立可验证（MVP 闭环：NL→FQN→task-brief→简报）

---

## Phase 4: User Story 2 - Agent 执行中获取实体即时指导 (Priority: P1)

**Goal**: Agent 指定实体 FQN，获取 step-guide 实体级 6 视角指导（约束级别/能力协议/决策分支/影响层/adjacentContext），交付 `metaforge.step-guide` 命令与 Skill。

**Independent Test**: 调用 `metaforge.step-guide --entity-fqn <fqn>` 返回实体级指导；缺失 `--entity-fqn` 给用法提示中止；归属失败（34004）终止并展示候选列表。

### Implementation for User Story 2

- [X] T016 [P] [US2] 创建 `.opencode/commands/metaforge.step-guide.md`（front-matter + 正文，经脚本调用，无 REST 细节，FR-DLV-001/009）
- [X] T017 [P] [US2] 创建 `.opencode/skills/metaforge-step-guide/SKILL.md`（自包含，FR-DLV-007/008）
- [X] T018 [US2] 实现 `modules/cognition.sh` 的 `cognition execute step-guide`：`--entity-fqn` 必填校验、34004 归属失败候选展示（`data.candidates`）、实体级视角透传与 Bundle 级视角 skip 提示（FR-014/019、FR-ERR、D1）
- [X] T019 [US2] 在 `test/cognition-agent-test.sh` 增加实体指导场景：有效实体返回 6 视角、缺参中止、归属失败候选展示

**Checkpoint**: User Story 1 AND 2 独立可用

---

## Phase 5: User Story 3 - 探索型 Agent 发现平台与领域 (Priority: P2)

**Goal**: Agent 请求平台清单（catalog）与逐层下钻（navigate），支持分页续翻，交付 `metaforge.catalog`/`metaforge.navigate` 命令与 Skill。

**Independent Test**: `metaforge.catalog` 返回 Bundle 列表 + 版本锚 + 主题域概要；`metaforge.navigate --bundles <fqn>` 逐层下钻返回层级树 + `has_more` + `entryStepFqn`；超单页时 `--cursor` 续翻完整遍历；缺 `--bundles` 中止。

### Implementation for User Story 3

- [X] T020 [P] [US3] 创建 `.opencode/commands/metaforge.catalog.md` + `.opencode/skills/metaforge-catalog/SKILL.md`（平台发现，FR-DLV-001/007/009）
- [X] T021 [P] [US3] 创建 `.opencode/commands/metaforge.navigate.md` + `.opencode/skills/metaforge-navigate/SKILL.md`（领域导航，FR-DLV-001/007/009）
- [X] T022 [US3] 实现 `cognition execute bundle-catalog`：`--page-size`/`--cursor` 分页透传与 `next_cursor` 解析续翻（FR-DLV-006、FR-016）
- [X] T023 [US3] 实现 `cognition execute navigate`：`--bundles` 必填校验、`--subject-domain`/`--expand`/`--page-size`/`--cursor` 透传、`has_more`/`entryStepFqn` 保留（FR-014/016）
- [X] T024 [US3] 在 `test/cognition-agent-test.sh` 增加平台发现/导航场景：catalog 列表、navigate 下钻、分页续翻、缺参中止

**Checkpoint**: User Story 3 独立可用

---

## Phase 6: User Story 4 - 编排型父 Agent 委派子任务认知 (Priority: P2)

**Goal**: 父 Agent 指定入口实体与收窄模式，获取 sub-task-brief 收窄简报（INHERITED 三层收窄 / PURE 纯净），交付 `metaforge.subtask` 命令与 Skill。

**Independent Test**: `metaforge.subtask --bundles <fqn> --entry-entity <fqn> --scope-mode INHERITED` 返回收窄约束/能力/决策 + `narrowedSchemaFqns`；`PURE` 仅返回 entity_profile；缺 `--bundles`/`--entry-entity` 中止。

### Implementation for User Story 4

- [X] T025 [P] [US4] 创建 `.opencode/commands/metaforge.subtask.md` + `.opencode/skills/metaforge-subtask/SKILL.md`（FR-DLV-001/007/009）
- [X] T026 [US4] 实现 `cognition execute sub-task-brief`：`--bundles`+`--entry-entity` 必填校验、`--scope-mode`（INHERITED/PURE）透传、`narrowedSchemaFqns` 保留（FR-005/014、FR-DLV-004）
- [X] T027 [US4] 在 `test/cognition-agent-test.sh` 增加子任务收窄场景：INHERITED 三层收窄、PURE 纯净、缺参中止

**Checkpoint**: User Story 4 独立可用

---

## Phase 7: User Story 5 - Agent 定制自由视角组合查询 (Priority: P2)

**Goal**: Agent 经 `perspectives` 参数按需组合任意视角子集，获取定制认知，交付 `metaforge.guidance` 命令与 Skill；承接 `bundle-scope` 能力（schema_inventory + instance_catalog 组合）。

**Independent Test**: `metaforge.guidance --perspectives schema_inventory,instance_catalog --bundles <fqn>` 返回恰好包含所请求视角章节的结果；含 `impact_trace` 时返回 forward/backward 分层。

### Implementation for User Story 5

- [X] T028 [P] [US5] 创建 `.opencode/commands/metaforge.guidance.md` + `.opencode/skills/metaforge-guidance/SKILL.md`（FR-DLV-001/007/009）
- [X] T029 [US5] 实现 `cognition execute cognition-guidance`：`--perspectives` 视角子集透传、`--bundles`/`--entity-fqn` 透传、视角章节原样返回（FR-CAP-003、FR-003、D2/R3 bundle-scope 子场景）
- [X] T030 [US5] 在 `test/cognition-agent-test.sh` 增加自由组合场景：指定视角子集返回对应章节、schema_inventory+instance_catalog 组合、含 impact_trace 视角

**Checkpoint**: User Story 5 独立可用

---

## Phase 8: User Story 6 - Agent 影响感知评估变更风险 (Priority: P3)

**Goal**: 查询含 `impact_trace` 视角时交付正向影响/反向依赖/影响路径详情并按影响程度分层，支撑变更风险评估。

**Independent Test**: 经 `step-guide` 或 `guidance` 发起的含 `impact_trace` 查询返回 forward_impact / backward_dependency / impact_paths 分层详情。

### Implementation for User Story 6

- [X] T031 [US6] 实现 `lib/http.sh`/`modules/cognition.sh` 的 impact_trace 输出处理：forward/backward 分层呈现、`impact_paths` 保留（FR-CAP-006、FR-006）
- [X] T032 [US6] 在 `test/cognition-agent-test.sh` 增加影响感知场景：含 impact_trace 查询返回分层影响（复用 US2/US5 查询路径，仅输出校验）

**Checkpoint**: User Story 6 独立可用

---

## Phase 9: User Story 7 - 领域知识工程师验证认知新鲜度 (Priority: P3)

**Goal**: 每次输出保留版本锚；对比前后两次查询的版本锚，某 Bundle 版本变化时提示"认知可能已过期"，一致时不误报。

**Independent Test**: 任意成功查询输出含 `data_version_anchors`；用两次不同版本锚对比，版本变化提示过期、版本一致不误报（对比由调用方完成，本产品不保存状态）。

### Implementation for User Story 7

- [X] T033 [US7] 实现版本锚输出与过期对比：每次输出无条件保留 `data_version_anchors`（map/array 双形态，R4/D8）；提供对比判定（两次查询版本锚变化→"认知可能已过期，建议重新获取"，一致→无提示）的脚本函数（FR-VER-001/002、FR-007）
- [X] T034 [US7] 在 `test/cognition-agent-test.sh` 增加新鲜度场景：输出含版本锚、版本变化提示过期、版本一致不误报

**Checkpoint**: 全部用户故事独立可用

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: 跨用户故事的收尾与一致性保障

- [X] T035 [P] 全部命令文件 front-matter 与 speckit 系列格式一致性复核；`--help` 用法说明补全（NFR-004/005）
- [X] T036 [P] 契约一致性复核：`metaforge-pro.sh` flag 映射与 `contracts/script-cli.md` 一致；命令/Skill 与 `contracts/opencode-delivery.md` 一致；请求字段 camelCase（R1 标注）
- [X] T037 运行 `quickstart.md` 验证（V1-V10 全部场景），修复失败项
- [X] T038 SC-011 性能验证：task-brief 端到端 ≤500ms、step-guide ≤150ms、CLI 开销占比 <5%（真实/模拟服务端测量）

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-9)**: All depend on Foundational phase completion
  - US1/US2（P1）优先；US3/US4/US5（P2）与 US6/US7（P3）可顺序或并行
- **Polish (Phase 10)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 任务认知 (P1)**: 依赖 Phase 2（config/http/errors/fqn-resolve/templates）；FQN 推测为 US1 核心，其余故事复用
- **US2 实体即时指导 (P1)**: 依赖 Phase 2；复用 http/errors，独立可测
- **US3 平台发现/导航 (P2)**: 依赖 Phase 2；复用 http/分页；独立可测
- **US4 子任务收窄 (P2)**: 依赖 Phase 2；复用 http；独立可测
- **US5 自由视角组合 (P2)**: 依赖 Phase 2；复用 http/perspectives 透传；独立可测
- **US6 影响感知 (P3)**: 依赖 US2/US5 的 impact_trace 查询路径；仅输出处理增量
- **US7 认知新鲜度 (P3)**: 依赖 Phase 2（http 已保留版本锚）；仅对比增量

### Within Each User Story

- 命令/Skill 文件（[P] 并行）→ 脚本实现（cognition execute 路径）→ 集成验证（test/cognition-agent-test.sh）
- 实现在前，验证在后（非 TDD）

### Parallel Opportunities

- Setup: T003 独立（[P]）
- Foundational: T004-T009 互不依赖可并行（[P]）
- US1: T011/T012 并行；US2: T016/T017 并行；US3: T020/T021 并行；US4: T025 单独；US5: T028 单独
- 不同用户故事可并行推进（依赖 Phase 2 完成后）

---

## Parallel Example: User Story 1 (MVP)

```bash
# Launch commands/skills files together:
Task: "Create .opencode/commands/metaforge.task-brief.md"
Task: "Create .opencode/skills/metaforge-task-brief/SKILL.md"

# After cognition.sh execute task-brief implementation:
Task: "Implement cognition execute task-brief in modules/cognition.sh"
Task: "Implement cognition resolve subcommand in modules/cognition.sh"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. 完成 Phase 1: Setup（.metaforge/ 骨架 + 单入口）
2. 完成 Phase 2: Foundational（config/http/errors/fqn-resolve/templates/env/health — CRITICAL，阻塞所有故事）
3. 完成 Phase 3: User Story 1（task-brief 命令 + Skill + cognition execute/resolve）
4. **STOP and VALIDATE**: 独立验证 US1（NL→FQN→简报闭环）
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → 基础就绪（env/health/templates 可用）
2. US1（P1）→ 验证 → MVP Demo（任务认知）
3. US2（P1）→ 验证 → Demo（实体即时指导）
4. US3/US4/US5（P2）→ 逐个验证 → Demo
5. US6/US7（P3）→ 验证 → 全能力闭环

### Parallel Team Strategy

1. Team 完成 Setup + Foundational
2. Foundational 完成后：A=US1、B=US2、C=US3（P2 早期），US4/US5 随后，US6/US7 收尾
3. 各故事独立实现、独立验证

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to user story for traceability
- 上游依赖：仅 `metaforge-cli/context/upstream-contracts/server-agent-cognition/rest-api.md`（只读对接）；无 foundation-contracts → 无 foundation 适配任务
- 严格禁止：命令/Skill 文件中出现 REST URL/HTTP 方法/curl（FR-DLV-009）；REST 调用必须经 `.metaforge/scripts/`（FR-DEV-001）
- 无状态幂等：不保存会话/任务上下文；凭据不落盘（FR-024）
- 每个故事可独立验证；验收以 spec §8 验收标准与 quickstart V1-V10 为准
- Commit after each task or logical group
