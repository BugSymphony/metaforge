# opencode AI 命令手动测试文档（metaforge.\* 认知闭环）

**阶段**: Phase 1（Design & Contracts）| **日期**: 2026-08-05
**关联**: [quickstart.md](../metaforge-cli/specs/001-metaforge-cli-consumption/quickstart.md) / [spec.md](../metaforge-cli/specs/001-metaforge-cli-consumption/spec.md) / 数据底座 [cognition-demo-seed.sql](./cognition-demo-seed.sql)

> 本文是**在 opencode 中手动执行 AI 命令**的验证指南：以「平台发现 → 领域导航 → 任务认知 → 实体指导 → 子任务收窄 → 自由视角组合」为闭环主线，业务（order/erp）与 Agent（metaforge）双视角交叉验证 6 个 metaforge 命令。每个场景同时给出**自然语言唤起方式**（Agent 日常用法，体现"指导 Agent 行为与认知"）与**显式参数方式**（供精确验证）。数据全部来自 `test/cognition-demo-seed.sql`（四层：元模型/元数据/关系/认知服务端索引）。

---

## 0. 前置条件

1. **服务端可用**：`metaforge-boot` 单进程部署，`POST /api/v1/cognition/{templateId}` 与 `GET /actuator/health` 可访问（默认 `http://localhost:8080`）。
2. **四层数据已上料**（本测试闭环的数据底座）：
   ```bash
   PGPASSWORD=metaforge psql -h localhost -U metaforge -d metaforge -f test/cognition-demo-seed.sql
   ```
   该脚本幂等，覆盖：
   - **元模型**：metaforge / order / erp 三 Bundle；entity_schema 27、relation_schema 31
   - **元数据**：M1 实例 75+（8 步流程、6 规则、4 能力、2 决策、2 风险、3 任务、2 Agent、3 角色、2 权限、common L1-L5 四域、protocol 3 类）
   - **关系**：relation_instance 69（PROCESS_SEQUENCE 18 / ASSOCIATION_REFERENCE 35 / COMPOSITION 39 / DEPENDENCY_INFLUENCE 15 / MAPPING_CORRESPONDENCE 2）
   - **认知服务端**：entity_relation_index 218 条（compute-engine 图遍历依赖）
3. **命令可用**：`.opencode/commands/metaforge.*.md` 已就位；命令仅调用 `.metaforge/scripts/metaforge-pro.sh`，不直接接触 REST。
4. **在 opencode 中执行**：以下每个场景中的 `/metaforge.xxx ...` 即为你在 opencode 输入框直接敲入的 slash 命令（含参数）。

### 关键 FQN 速查（数据底座提供）

| 用途 | FQN |
|---|---|
| 业务 Bundle | `order:1.0.0` / `erp:1.0.0` |
| Agent 基础 Bundle | `metaforge:1.0.0` |
| 流程入口步骤 | `order:1.0.0.Step_ReceiveOrder`（ENTRY） |
| 主锚点步骤 | `order:1.0.0.Step_CheckInventory`（PROCESSING） |
| 决策分支步骤 | `order:1.0.0.Step_CheckPayment`（DECISION，出边 2） |
| 退出步骤 | `order:1.0.0.Step_CompleteOrder`（EXIT） |
| 履约任务 / 库存任务 | `metaforge:1.0.0.agent.Task_OrderFulfillment` / `Task_InventoryCheck` |
| Agent | `metaforge:1.0.0.agent.Agent_OrderBot` / `Agent_InventoryBot` |
| 主题域 | `metaforge:1.0.0.common.Domain_Order` / `Domain_Inventory` / `Domain_Payment` |

---

## 0.1 自然语言调用（Agent 行为与认知驱动）⭐ 核心用法

> metaforge 命令的**首要交互方式是自然语言**：Agent（opencode 中的 AI）读取命令文件的
> `description` 判断"何时用"，把用户意图映射为结构化参数后调用 CLI。以下给出每个命令的
> NL 唤起方式、Agent 应执行的动作、以及认知输出如何**反哺 Agent 行为**（形成闭环）。

### NL 唤起 → 命令映射

| 用户自然语言（你直接输入） | Agent 判定使用 | Agent 行为（映射到 CLI） |
|---|---|---|
| 「平台上都有哪些业务库？我想选一个接入」 | `metaforge.catalog` | 无需 FQN，直接调 `bundle-catalog`；据返回的 Bundle 清单决定下一步（进入哪个库） |
| 「我想看看订单履约领域里都有什么」 | `metaforge.navigate` | 从 catalog 结果或上下文推导 `--bundles order:1.0.0`，逐层下钻 |
| 「我要执行订单履约任务，先了解它的规则、流程和可用能力」 | `metaforge.task-brief` | 把「订单履约」映射为 `--bundles order:1.0.0`（或 resolve 推测）→ 取简报 |
| 「履约流程里『库存校验』这一步该怎么做？有什么约束和能力？」 | `metaforge.step-guide` | 把「库存校验」映射为 `--entity-fqn order:1.0.0.Step_CheckInventory` → 取实体指导 |
| 「我要把库存校验委派给子 Agent，给我一份只跟它相关的收窄简报」 | `metaforge.subtask` | `--entry-entity Task_InventoryCheck --scope-mode INHERITED` → 收窄简报 |
| 「帮我盘点一下 order 库的 Schema 和实例」 / 「评估改『库存校验』会影响什么」 | `metaforge.guidance` | `--perspectives schema_inventory,instance_catalog` / `--entity-fqn Step_CheckInventory --perspectives impact_trace` |

### 认知输出 → Agent 行为闭环（指导 Agent 下一步）

1. **任务认知简报**（task-brief）返回 `constraint_set`（MANDATORY/RECOMMENDED/REFERENCE）、
   `flow_blueprint`（流程步骤）、`capability_catalog`（可用能力）、`decision_matrix`（决策分支）——
   Agent 据此**规划执行路径**：按哪个分支走、调用哪个能力、必须满足哪些约束。
2. **实体即时指导**（step-guide）返回实体级 `constraint_set`（约束级别）、`capability_catalog`
   （能力协议）、`impact_trace`（影响层）——Agent 在**执行中单步决策**时规避越界、按需调用能力。
3. **子任务收窄**（subtask）返回收窄后的约束/能力/决策——父 Agent 据此**判断委派是否可行**，
   子 Agent 据此**知道自己该知道什么**（职责隔离）。
4. **影响感知**（guidance + impact_trace）返回 `forwardImpact / backwardDependency / impactPaths`——
   Agent 在**变更前评估波及范围**，决定是否规避或回滚。
5. **版本锚**（每次输出携带）——Agent 在**复用旧认知前**对比两次查询，判断是否过期。

> 下面每个场景均先给**自然语言唤起方式**，再给**显式参数方式**（两者等价，前者是 Agent 日常用法）。

---

## 场景 1：平台发现 → 领域导航（探索型闭环）

**目的**：验证 `metaforge.catalog`（平台发现）→ `metaforge.navigate`（领域导航）的入口闭环，确认模板注册表投影与版本锚。

**自然语言唤起**（Agent 行为：判定意图 → 映射到 catalog/navigate）：
```text
平台上都有哪些业务库？我想选一个接入。
/metaforge.catalog 平台上有哪些可用的库？
/metaforge.navigate 我想看看订单履约领域里都有什么
```
**预期**：Agent 依据命令文件 `description` 选用 `metaforge.catalog` / `metaforge.navigate`，把「订单履约」映射为 `--bundles order:1.0.0` 后执行，返回清单/层级树并据结果决定下钻目标。

**显式参数方式**（同义，供精确验证）：
```text
/metaforge.catalog
```
**预期**：
- 输出 `code=200`；`bundle_directory.bundles` 含 3 个 Bundle（MetaForge 语义基座 / 企业资源计划 / 订单领域）
- `contextMeta.dataVersionAnchors` 含 `metaforge:1.0.0` 版本锚
- 分页：追加 `--page-size 10` 仍正常返回

```text
/metaforge.navigate --bundles metaforge:1.0.0 --subject-domain metaforge:1.0.0.common.Domain_Order
```
**预期**：
- 输出 `code=200`，含 `domain_navigation` 章节与版本锚
- ⚠️ 当前服务端 `domain_navigation` 为 BUNDLE scope 懒加载，`--subject-domain` 锚点尚未返回子节点（服务端实现缺口，非数据问题）——**子节点下钻建议改用 `--entity-fqn` 锚点查询或跳过此项**，不影响其余命令闭环。

**验证点**：模板清单（V2）与 catalog Bundle 清单（V3）对齐；版本锚存在；NL 意图 → 命令映射正确。

---

## 场景 2：任务认知简报（业务视角，execution 原型）

**目的**：验证 `metaforge.task-brief` 对业务 Bundle `order:1.0.0` 返回 6 视角简报，且 json/prompt 双格式语义一致。

**自然语言唤起**（Agent 行为：识别「任务认知」意图 → 映射 Bundle → 取简报并据简报规划执行）：
```text
我要执行订单履约任务，先了解它的规则、流程和可用能力。
/metaforge.task-brief 我想了解订单履约任务的规则与流程
```
**预期**：Agent 把「订单履约任务」映射为 `--bundles order:1.0.0 --archetype execution`，返回简报；
随后 Agent 依据简报中的 `constraint_set`（必守约束）、`capability_catalog`（可用能力）、
`decision_matrix`（分支选择）**规划下一步执行动作**（如：先确认订单→库存校验→支付校验分派）。

**显式参数方式**（同义，供精确验证）：
```text
/metaforge.task-brief --bundles order:1.0.0 --depth L2 --archetype execution --max-tokens 8000 --format json
```
**预期**：
- `code=200`；`perspectives` 含 `entity_profile / constraint_set / capability_catalog / flow_blueprint / decision_matrix / prerequisite_chain`
- `flow_blueprint` 的 Bundle 侧步骤由 `metaforge:1.0.0.agent.ExecutionStep` 实例构成（8 步含 ENTRY/EXIT）
- `contextMeta.dataVersionAnchors` 含 `order` 版本锚

```text
/metaforge.task-brief --bundles order:1.0.0 --format prompt
```
**预期**：输出 Markdown，`## 认知查询结果` 开头，可直接注入 LLM 上下文。

**必填校验**：敲 `/metaforge.task-brief`（无 `--bundles`）→ 输出用法提示（`必须指定 --bundles`）并中止，退出码 2。

**验证点**：10 视角简报中至少 6 视角返回；双格式语义一致；必填校验生效（FR-014）；认知输出可反哺 Agent 行为。

---

## 场景 3：实体即时指导（执行中单步决策）

**目的**：验证 `metaforge.step-guide` 对主锚点步骤 `Step_CheckInventory` 返回实体级 6 视角，含约束级别 / 能力协议 / 决策分支 / 影响层 / 邻接上下文。

**自然语言唤起**（Agent 行为：识别「执行中需要实体指导」意图 → 把实体名称映射为 FQN → 取指导并据此单步决策）：
```text
履约流程里「库存校验」这一步该怎么做？有什么约束和能力？
/metaforge.step-guide 库存校验这一步我该怎么执行？
```
**预期**：Agent 把「库存校验」映射为 `--entity-fqn order:1.0.0.Step_CheckInventory`，返回实体级指导；
随后 Agent 依据 `constraint_set`（必须满足 `Rule_InventoryAboveZero` 等约束）、
`capability_catalog`（可调用 `Cap_InventoryAPI`）、`decision_matrix`（是否分支）**执行该步骤并规避越界**。

**显式参数方式**（同义，供精确验证）：
```text
/metaforge.step-guide --entity-fqn order:1.0.0.Step_CheckInventory --format json
```
**预期**：
- `code=200`；`perspectives` 含 `entity_profile / constraint_set / capability_catalog / decision_matrix / impact_trace / relationship_graph`
- `entity_profile` 返回 `库存校验` + `step_type=PROCESSING` + `schemaAttributes`（ExecutionStep 属性）
- `impact_trace` 含 `forwardImpact / backwardDependency / impactPaths`
- `relationship_graph` 按 AssociationType 分组（PROCESS_SEQUENCE 前后步骤等）
- stderr 含「影响感知（impact_trace）」分层摘要

```text
/metaforge.step-guide --entity-fqn order:1.0.0.Step_CheckInventory --format prompt
```
**预期**：Markdown 输出，语义与 json 一致。

**错误闭环**：
- 缺 `--entity-fqn` → `必须指定 --entity-fqn` 中止（退出码 2）
- 幽灵实体 `ghost:1.0.0.Task_X` → `归属校验失败` 中文提示（34004）+ 候选列表，退出码 1

**验证点**：实体级 6 视角齐备；约束级别 MANDATORY/RECOMMENDED/REFERENCE 分级可见；34004 归属失败候选展示（FR-ERR/FR-014）；认知输出支撑单步决策。

---

## 场景 4：子任务认知收窄（编排型父 Agent 委派）

**目的**：验证 `metaforge.subtask` 的 INHERITED 三层收窄与 PURE 纯净两种模式。

**自然语言唤起**（Agent 行为：识别「编排委派」意图 → 映射入口实体与收窄模式 → 收窄简报供子 Agent 使用）：
```text
我要把库存校验委派给子 Agent，给我一份只跟它相关的收窄简报。
/metaforge.subtask 给库存校验子 Agent 一份收窄认知简报
```
**预期**：Agent 把「库存校验子任务」映射为 `--entry-entity metaforge:1.0.0.agent.Task_InventoryCheck
--scope-mode INHERITED`，返回收窄简报；父 Agent 据此**判断委派可行性**，子 Agent 据此**只接触该知道的内容**。

**显式参数方式**（同义，供精确验证）：
```text
/metaforge.subtask --bundles metaforge:1.0.0 --entry-entity metaforge:1.0.0.agent.Task_OrderFulfillment --scope-mode INHERITED --format json
```
**预期**：
- `code=200`；`perspectives` 含 `entity_profile / constraint_set / capability_catalog / decision_matrix / prerequisite_chain / relationship_graph`
- 收窄范围围绕履约任务（含其归属主题域 `Domain_Order` 与入口步骤）

```text
/metaforge.subtask --bundles metaforge:1.0.0 --entry-entity metaforge:1.0.0.agent.Task_InventoryCheck --scope-mode PURE --format json
```
**预期**：
- `code=200`；`perspectives` **仅** `entity_profile`（纯净模式，无继承/扩散内容）

**必填校验**：敲 `/metaforge.subtask`（缺 `--bundles` / `--entry-entity`）→ 用法提示中止（退出码 2）。

**验证点**：INHERITED 多视角收窄 vs PURE 仅 entity_profile（FR-005/FR-014）；无状态幂等（重复执行结果语义一致）。

---

## 场景 5：自由视角组合（定制化认知 + 影响感知）

**目的**：验证 `metaforge.guidance` 恰好返回所请求视角章节，并承接 bundle-scope 能力（schema_inventory + instance_catalog）与影响感知（impact_trace）。

**自然语言唤起**（Agent 行为：识别「定制化认知」意图 → 选择视角子集 → 聚合认知供评估/盘点）：
```text
帮我盘点一下 order 库的 Schema 和实例。
/metaforge.guidance 我想看看订单库有哪些 Schema 和业务实例
```
**预期**：Agent 把「Schema 和实例」映射为 `--perspectives schema_inventory,instance_catalog`，返回恰好两章；
Agent 据此盘点元模型结构清单与实例规模。

```text
评估一下改「库存校验」会影响哪些实体和步骤。
/metaforge.guidance 我改库存校验会不会影响支付校验和锁库存？
```
**预期**：Agent 映射为 `--entity-fqn order:1.0.0.Step_CheckInventory --perspectives impact_trace,relationship_graph`，
返回 forward/backward 影响分层；Agent 据此**变更风险评估**（如影响 `Step_CheckPayment`/`Step_ReserveStock`）。

**显式参数方式**（同义，供精确验证）：
```text
/metaforge.guidance --bundles order:1.0.0 --perspectives schema_inventory,instance_catalog --format json
```
**预期**：
- `code=200`；`perspectives` **恰好**为 `schema_inventory` + `instance_catalog` 两章
- `schema_inventory.schemas` ≥ 2（Order/Item）；`instance_catalog.entities` 9 个业务实例

```text
/metaforge.guidance --bundles order:1.0.0 --entity-fqn order:1.0.0.Step_CheckInventory --perspectives impact_trace,relationship_graph --format json
```
**预期**：
- `code=200`；含 `impact_trace`（forward/backward/impactPaths）与 `relationship_graph`（按 AssociationType 分组）
- stderr 显示影响感知分层摘要

**验证点**：视角子集精确命中（FR-003）；影响感知 forward/backward/impact_paths 分层（FR-006/FR-003）；承接 bundle-scope（R3）。

---

## 场景 6：FQN 推测（cognition resolve，FR-NL）——自然语言 → 结构化

**目的**：验证自然语言 → FQN 推测的唯一/多候选/零命中三态，这是**自然语言驱动命令的核心前置**：
Agent 先据用户 NL 推测出确定型 FQN，再继续执行认知查询（全程不向服务端发送自然语言）。

**自然语言唤起（推荐用法）**：
```text
/metaforge.task-brief 订单履约主任务
/metaforge.step-guide 库存校验
/metaforge.subtask 给库存校验子 Agent 一份简报
```
**Agent 行为**：`task-brief`/`step-guide`/`subtask` 命令文件声明"可直接传入自然语言描述"；
Agent 把 NL 交给 `cognition resolve` 推测 FQN，唯一命中自动确认继续执行，多候选列候选请用户选择，
零命中终止并给原因与平台清单。

**显式 resolve 三态验证**：
```text
/metaforge.catalog --verify        # 先确认模板与数据就绪
```
```text
cognition resolve 订单履约主任务     # 唯一命中 → 输出 FQN（如 metaforge:1.0.0.agent.Task_OrderFulfillment）
cognition resolve 订单              # 多候选 → 列出 fqn/name/description 请用户选择（退出码 3）
cognition resolve 不存在的任务xxx    # 零命中 → 终止 + 原因 + 平台已发布清单（退出码 1）
```
（注：上述 `cognition resolve` 为脚本子命令，在 opencode 中由 Agent 依据 NL 内部调用，不直接敲入。）

**预期**：
- 若服务端上料完整 → 唯一命中自动确认，输出 FQN 并继续执行简报
- ⚠️ 当前服务端 `bundle-catalog`/`navigate` 要求 `bundleFqns` 非空（34003），resolve 基于空目录的推测路径暂不可用（R2 已知风险，需 mock 数据或服务端放开空目录查询）——**此场景的 resolve 部分标记 SKIP，不构成数据问题**；Agent 应回退为"从上下文/catalog 结果显式推导 FQN"。

**验证点**：唯一命中自动确认 / 多候选列候选 / 零命中终止并给原因与平台清单（FR-NL-001~005）；全程不向服务端发送 NL（FR-010）。

---

## 场景 7：错误处理与认知新鲜度（FR-ERR / FR-VER）

**自然语言唤起（错误闭环）**：
```text
/metaforge.task-brief 帮我看看退款任务的规则     # Agent 推测"退款"→ 平台无此 Bundle → 中文提示或列候选
/metaforge.step-guide 幽灵步骤怎么执行           # Agent 无法映射 → 缺 --entity-fqn 用法提示（rc 2）
/metaforge.task-brief --bundles not-a-bundle:9.9.9
```
**预期**：Agent 对无法唯一映射的 NL 给出中文提示（多候选列候选 / 零命中终止 / 必填校验），不臆测 FQN（FR-NL-002）。

**显式错误验证**：
```text
/metaforge.task-brief --bundles not-a-bundle:9.9.9
```
**预期**：服务端错误码（34003 或 34002）→ 简体中文提示（不暴露堆栈），退出码 1。

```text
/metaforge.guidance --perspectives impact_trace
```
**预期**：缺 `--bundles`/`--entity-fqn` 时按模板语义返回或提示；任意成功查询均携带 `dataVersionAnchors`（版本锚）。

**新鲜度对比（FR-VER-002，由调用方完成）**：
- 连续两次相同查询 → 版本锚一致（`queriedAt` 除外），语义一致（幂等）
- 将两次输出的 `dataVersionAnchors` 对比：某 Bundle 版本变化 → 提示「认知可能已过期，建议重新获取」

**验证点**：34001/34002/34003/34004 中文映射（FR-018）；版本锚无条件展示 + map/array 双形态（FR-VER-001，R4）；幂等（FR-015）。

---

## 场景 8：自然语言驱动的完整闭环（Agent 行为走查 ⭐）

> 模拟一位**订单履约编排 Agent** 的完整工作流：全程用自然语言驱动，每一步的认知输出都指导下一步行为。

**Step 1 平台选型（catalog）**
```text
我要接入订单履约业务，平台上有哪些可用的库？
```
**Agent 行为**：判定「平台发现」→ 调用 `metaforge.catalog` → 得到 3 个 Bundle 与版本锚 → **选择 `order:1.0.0`**。

**Step 2 任务认知（task-brief）**
```text
我要执行订单履约任务，先了解它的规则、流程和可用能力。
```
**Agent 行为**：判定「任务认知」→ `metaforge.task-brief --bundles order:1.0.0` → 简报含
`constraint_set`（48h 发货/库存>0/大额审批等 MANDATORY）、`flow_blueprint`（8 步）、
`capability_catalog`（4 能力）、`decision_matrix`（支付校验分支）→ **规划执行路径**。

**Step 3 实体即时指导（step-guide）**
```text
履约流程里「库存校验」这一步该怎么做？有什么约束和能力？
```
**Agent 行为**：判定「实体指导」→ 把「库存校验」映射为 `--entity-fqn order:1.0.0.Step_CheckInventory`
→ 返回实体级 6 视角（约束 `Rule_InventoryAboveZero`、能力 `Cap_InventoryAPI`、影响层、邻接上下文）→ **单步执行并规避越界**。

**Step 4 子任务委派（subtask）**
```text
我要把库存校验委派给子 Agent，给我一份只跟它相关的收窄简报。
```
**Agent 行为**：判定「子任务收窄」→ `metaforge.subtask --entry-entity Task_InventoryCheck
--scope-mode INHERITED` → 收窄简报 → **判断委派可行，子 Agent 只接触职责内内容**。

**Step 5 变更风险评估（guidance + impact_trace）**
```text
我改「库存校验」会不会影响支付校验和锁库存？
```
**Agent 行为**：判定「影响感知」→ `metaforge.guidance --entity-fqn Step_CheckInventory
--perspectives impact_trace` → forward/backward 影响分层（波及 `Step_CheckPayment`/`Step_ReserveStock`）→ **变更前规避风险**。

**Step 6 认知新鲜度（版本锚对比）**
```text
重复执行 Step 2 的查询，对比前后版本锚，确认认知未过期。
```
**Agent 行为**：对比两次 `dataVersionAnchors` → 一致则直接复用认知，变化则提示「认知可能已过期，建议重新获取」。

**验证点**：一条 Agent 工作流串起 6 个命令；每个 NL 都经「意图识别 → 参数映射 → 认知返回 → 行为指导」闭环；业务（order）与 Agent（metaforge）视角均在数据底座覆盖。

---

## 附录：命令 ↔ 模板 ↔ 能力映射

| opencode 命令 | 模板 | 能力 | 本文场景 |
|---|---|---|---|
| `metaforge.catalog` | bundle-catalog | 平台发现 | 场景 1 / 8 |
| `metaforge.navigate` | navigate | 领域导航 | 场景 1 |
| `metaforge.task-brief` | task-brief | 任务认知 | 场景 2 / 6 / 8 |
| `metaforge.step-guide` | step-guide | 实体即时指导 | 场景 3 / 8 |
| `metaforge.subtask` | sub-task-brief | 子任务认知（收窄） | 场景 4 / 8 |
| `metaforge.guidance` | cognition-guidance | 自由视角组合 / 影响感知 / bundle-scope | 场景 5 / 7 / 8 |

## 附录：数据底座文件

| 文件 | 作用 |
|---|---|
| `test/cognition-demo-seed.sql` | 四层数据（元模型/元数据/关系/认知服务端索引），业务+Agent，幂等 |
| `test/cognition-seed.sql` | 业务 Bundle（erp/order）基础 seed（既有） |
| `test/cognition-agent-seed.sql` | Agent Bundle（metaforge）基础 seed（既有） |
| `../docs/cognition2/mock/order-bundle-m1.json` | 参考 mock 数据（56 实体 + 67 关系，语义以服务端为准） |

## 附录：已定义未验证（需真实服务端/上料）

- 场景 1 的 navigate 子节点下钻（服务端 `domain_navigation` BUNDLE-scope 懒加载锚点未返回子节点）
- 场景 6 的 resolve 三态（服务端 `bundle-catalog` 要求 `bundleFqns` 非空，34003；R2 已知风险）
- 端到端性能目标（SC-011：task-brief ≤500ms、step-guide ≤150ms、CLI 开销 <5%）
