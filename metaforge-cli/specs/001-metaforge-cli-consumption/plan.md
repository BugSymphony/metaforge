# Implementation Plan: metaforge-cli 元认知指导能力（Agent 消费接入）

**Branch**: `001-metaforge-cli-consumption` | **Date**: 2026-08-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-metaforge-cli-consumption/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

为 Agent（opencode）提供消费 MetaForge 元认知服务的能力：以 **8 项元认知指导能力**（平台发现/领域导航/任务认知/实体即时指导/子任务认知/自由视角组合/影响感知/认知新鲜度）为需求主体，命令与 Skill 是服务端模板注册表的**声明式投影**。CLI 将 Agent 的自然语言意图转换为结构化 `CognitionRequest`（camelCase），经 `POST /api/v1/cognition/{templateId}`（封装于 `.metaforge/scripts/` 脚本，命令/Skill 不出现任何 REST 细节）获取认知简报，以 json/prompt 双格式原样透传，并交付版本锚、FQN 推测（基于服务端数据，严禁臆测）、错误码中文映射。MVP 不做 MCP 委托发布与授权白名单。

技术路线：**shell 脚本（单入口 + 命名空间子命令）+ opencode 声明式命令（front-matter）+ 自包含 Skill**，与现有 `.specify/`/`.opencode/commands/`（speckit 系列）惯例对齐。核心设计约束来自上游契约 `rest-api.md` v1.1.0（camelCase 实际实现）与 BC 宪法 I~VIII。

## Technical Context

**Language/Version**: Bash（POSIX sh 兼容），风格与 `.specify/scripts/bash/speckit-pro.sh` 对齐；命令/Skill 为 Markdown + YAML front-matter

**Primary Dependencies**: curl（HTTP 调用）、jq（JSON 解析）、sed/grep（文本处理）、opencode（命令/Skill 装载平台，非代码依赖）

**Storage**: 无本地持久化（无状态幂等）；仅用户级配置文件 `~/.config/metaforge/config.yml`（Q3 澄清）+ `META_FORGE_*` 环境变量

**Testing**: shell 集成测试（`test/cognition-agent-test.sh` 风格）；验证数据用 mock（`docs/cognition2/mock/order-bundle-m1.json`）

**Target Platform**: opencode CLI（Agent 消费端），Linux/macOS shell 环境

**Project Type**: opencode AI slash commands + Skills + shell 脚本（CLI 消费端工具）

**Performance Goals**: 端到端 task-brief ≤500ms、step-guide ≤150ms（引用服务端预算）；CLI 侧开销占端到端 <5%（SC-011）

**Constraints**: 命令/Skill 文件不得出现 REST URL/HTTP 方法/curl（FR-DLV-009/FR-022）；请求字段 camelCase（R1）；零配置可用（默认 base-url `http://localhost:8080`）；无状态幂等；用户可见提示简体中文（NFR-007）；瞬时故障自动重试 1 次（FR-025）

**Scale/Scope**: 8 能力 / 6 模板 / 14 视角；单 Agent 消费；无并发规模要求（受服务端约束）

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

*✅ Re-checked after Phase 1 (2026-08-01): 设计产物（research/data-model/contracts/quickstart）已复核，以下全部结论保持 PASS，无设计引入的违规。*

**全局宪法（Level 1，MUST 不可覆盖）**
- ✅ **I 元模型唯一权威性**：消费端仅透传，不构造/改写语义内容，语义以服务端 `ApiResponse<T>` 为准（FR-023）→ PASS
- ✅ **II 显式导入边界管控 / III 全链路权限过滤**：MVP 无授权白名单，由上游服务端强制执行，本 BC 仅透传不绕过不弱化（spec Assumptions + BC 宪法 MVP 边界章节）→ PASS（依赖上游已执行）
- ✅ **IV 版本统一收敛**：版本锚 `data_version_anchors` 保留/展示，支撑过期判定（FR-007）→ PASS
- ✅ **IX 纯元数据边界坚守**：本 BC 不持有业务数据、无本地持久化（FR-015、FR-024）→ PASS
- ✅ **X 文档中文规范**：spec/plan/命令提示均为简体中文，术语保留英文（NFR-007）→ PASS

**BC 宪法（Level 3/4）**
- ✅ **I 纯消费端边界坚守**：请求构造/传输/响应解析与呈现，不实现服务端侧能力 → PASS
- ✅ **II 能力优先与模板驱动**：从服务端 `cognition templates` 动态解析，命令清单为声明式投影，不硬编码（FR-002/FR-013）→ PASS
- ✅ **III 结构化透传铁则**：camelCase 对齐，NL→结构化在本 BC 完成，不向服务端发自然语言（FR-010/FR-023）→ PASS
- ✅ **IV 无状态幂等**：不保存会话/任务上下文（FR-015）→ PASS
- ✅ **V 零解析开销输出**：json/prompt 双格式原样透传（FR-008/FR-009）→ PASS
- ✅ **VI 认知新鲜度保障**：版本锚展示 + 过期提示（FR-007）→ PASS
- ✅ **VII 可观测与可诊断**：X-Trace-Id 透传、--verbose、错误码中文映射（FR-018/FR-021）→ PASS
- ✅ **VIII 配置可治理**：配置覆盖优先级 flag > env > config > 默认；零配置可用（FR-020，Q3 澄清）→ PASS

无违规项。无需 Complexity Tracking 表。

## Project Structure

### Documentation (this feature)

```text
specs/001-metaforge-cli-consumption/
├── plan.md                   # This file (/speckit.plan command output)
├── research.md               # Phase 0 output
├── data-model.md             # Phase 1 output
├── quickstart.md             # Phase 1 output
├── contracts/                # Phase 1 output（本 BC 导出的命令/Skill 契约）
└── tasks.md                  # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (BC root)

```text
REPO_ROOT = /data/ext/source-8/metaforge
BC_PATH   = /data/ext/source-8/metaforge/metaforge-cli   # 本 BC（agent-consumption）物理目录
```

```text
# 开发态环境（与 .specify/ 共存、职责分离，FR-DEV-001/005）
.metaforge/
└── scripts/
    ├── metaforge-pro.sh       # 单入口 + 命名空间子命令（风格对齐 speckit-pro.sh）
    ├── lib/
    │   ├── config.sh          # 配置读取/合并/覆盖优先级（Q3）
    │   ├── http.sh            # REST 通信唯一承载：curl 封装、超时、X-Trace-Id、重试 1 次（FR-025）
    │   ├── errors.sh          # 34001~34006 + 网络错误 → 中文提示映射（FR-018）
    │   └── fqn-resolve.sh     # FQN 推测流水线：目标识别→候选获取→匹配→确认/终止（FR-011/012）
    └── modules/
        ├── env.sh             # env root / env summary
        ├── cognition.sh       # cognition execute / templates / resolve
        └── health.sh          # health（GET /actuator/health）

# opencode 交付形态（命令，FR-DLV-001/009；与 speckit 系列同目录）
.opencode/commands/
├── metaforge.catalog.md       # 平台发现（bundle-catalog）
├── metaforge.navigate.md      # 领域导航（navigate）
├── metaforge.task-brief.md    # 任务认知（task-brief）
├── metaforge.step-guide.md    # 实体即时指导（step-guide）
├── metaforge.subtask.md       # 子任务认知（sub-task-brief）
└── metaforge.guidance.md      # 自由视角组合（cognition-guidance）

# opencode 交付形态（Skill，FR-DLV-007/008；自包含 SKILL.md）
.opencode/skills/
├── metaforge-catalog/SKILL.md
├── metaforge-navigate/SKILL.md
├── metaforge-task-brief/SKILL.md
├── metaforge-step-guide/SKILL.md
├── metaforge-subtask/SKILL.md
└── metaforge-guidance/SKILL.md

# 测试
test/
└── cognition-agent-test.sh    # 集成验证（沿用现有脚本，扩展 metaforge 场景）

# 用户级配置（Q3 澄清，FR-CFG-001/002）
~/.config/metaforge/config.yml   # server.base-url / timeout / default.*
```

**Structure Decision**: 
- Selected structure type: 轻量脚本工具 BC（Option 5 变体）——单入口脚本 + 命名空间子命令 + 声明式命令/Skill 文件
- BC relative path to REPO_ROOT: `metaforge-cli/`（context/docs/specs 治理资产）+ `REPO_ROOT/.metaforge/` + `REPO_ROOT/.opencode/`（运行交付物，遵循仓库既有 opencode 惯例）
- Real directory layout: 见上方源码树；`.metaforge/scripts/` 为全部 REST 调用唯一承载处（FR-DEV-001/FR-DLV-009）
- Selection rationale: 本 BC 是 opencode 消费端工具，无服务端运行时；仓库既有惯例为 `.specify/scripts/bash/*.sh`（单入口）与 `.opencode/commands/*.md`（front-matter），metaforge 沿用同构模式以最大化兼容与可维护性（NFR-005）
- Internal architecture note: 无 DDD 分层；按"配置 → HTTP → 错误 → FQN 推测"的模块职责拆分 lib 脚本，命令/Skill 仅作声明式投影
- Cross-BC dependency status: 依赖 1 个上游业务 BC（`server-agent-cognition`，导入契约 `rest-api.md` v1.1.0）；无导出业务契约（命令/Skill 契约见 `contracts/`，由本 BC 维护）

**BC Boundary Confirmation**: 
- ✅ 本 BC 核心逻辑（脚本）封装于 `$BC_PATH` 与 REPO_ROOT 的 `.metaforge/`（BC 专属开发态环境，PRD 明令与 `.specify/` 共存），不直接引用其他 BC 内部实现代码
- ✅ 无对外业务接口导出；命令/Skill 定义为本 BC 交付形态，契约沉淀于 `FEATURE_DIR/contracts/` 并由本 BC 维护
- ✅ 上游依赖仅使用 `$BC_PATH/context/upstream-contracts/server-agent-cognition/rest-api.md`，无跨 BC 直接代码调用
- ✅ 跨 BC 交互严格遵循上游契约（camelCase 实际实现），MVP 按实现发送
- Foundation compliance: 无 foundation-contracts 导入，不适用

## Complexity Tracking

无（Constitution Check / Foundation Check 均无违规项）。

> **注**：`rest-api.md` 契约文档字段为 snake_case（`bundle_fqns` 等），而服务端实际实现为 camelCase（`bundleFqns`），spec 与 BC 宪法 III 明确 MVP 按实际实现 camelCase 发送——此为**上游契约与实现不一致**（R1），本 BC 以 spec 明确要求为准，并在 `quickstart.md` 与 `research.md` 风险段记录，需与上游核对统一，不构成本 BC 设计违规。
