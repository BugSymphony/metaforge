# 架构

MetaForge 由五层组成：**元模型管理 → 元数据（含关系）→ 图查询与推理引擎 → 认知服务端 → 消费端**。自下而上为"定义结构 → 填充语义 → 推理 → 认知 → 消费"的**构建/依赖方向**（元模型是底座）；运行时请求方向相反——消费端发起，自上而下逐层调用。

## 五层架构

![架构图](assert/architecture.png "架构图")

**五层一句话**：元模型定义"语义长什么样" → 元数据填充"具体语义是什么" → 推理引擎沿关系网络推演 → 认知模板组织成 Agent 认知 → 消费端交付给 Agent。

**全链路只处理元数据，不触碰业务数据**——这是 MetaForge 的架构边界，也是它能跨系统通用的根本原因。

## 元模型与元数据（M2 → M1）

- **元模型（M2）**：声明式定义结构——Bundle 命名空间、实体类型（EntitySchema）、关系类型（RelationSchema）。是语义规则的唯一底座，也是"自建语义库"的第一步（先建模）
- **元数据（M1）**：遵循元模型结构约束填充的领域语义实例——概念、规则阈值、能力描述、业务对象结构（L3-L5），及连接它们的关系实例。是 Agent 认知的实际内容

"自建语义库"正是走这条链路：**先建元模型（定义类型/关系）→ 再上料元数据（填充语义实例）→ 发布版本 → 推理引擎即可查询、认知模板即可消费**。

## 图查询与推理引擎（技术亮点）

认知算子不是简单查表，而是**沿语义关系网络做图查询与推理**——这是 MetaForge 的技术纵深所在：

| 能力 | 说明 | 支撑的算子/场景 |
|------|------|----------------|
| **邻域/子图查询** | 以实体为中心展开关联邻域 | 邻域探索、直连关系 |
| **组成树查询** | 递归展开组成结构（父链/子树） | 业务对象结构、归属 |
| **模式匹配** | 按关系模式匹配子图 | 决策分支、结构识别 |
| **影响扩散** | 从变更点沿出边 BFS 扩散 N 度 | 变更影响（FORECAST） |
| **依赖溯源** | 沿入边逆 BFS 追溯依赖方 | 变更依赖（FORECAST） |
| **路径/闭包/可达性** | 实体间路径、传递闭包、可达判断 | 影响路径、跨层级流程 |
| **多跳推理** | 多跳关联推理 | 跨任务委派链路 |

以 FORECAST 为例：评估"修改某实体"的影响，不是查一条记录，而是**沿语义关系网络做影响扩散 + 依赖溯源**（BFS），再结合约束规则判定风险——这正是"元认知"区别于"数据查询"的技术根基。

## 接口契约

认知服务唯一入口：`POST /api/v1/cognition/{templateId}`

```json
{
  "scope":  { "bundles": ["metaforge:1.0.0"], "domains": [], "entitySchemas": [] },
  "params": { "entity_fqn": "metaforge:1.0.0.agent.Task_InventoryCheck",
              "selectOperators": ["procedural.flow-blueprint"] },
  "format": "JSON",
  "cognitionDepth": "L3",
  "agentArchetype": "EXECUTION",
  "maxTokens": 8000
}
```

- **scope**：认知边界（bundle 白名单即授权依据）
- **params**：模板参数（entity_fqn 锚点、selectOperators 算子子集等）
- **format**：JSON / PROMPT（prompt 可直接注入 LLM）
- 响应含 `dimensions`（扁平算子结果列表）+ `contextMeta`（版本锚、跳过说明）

## 消费端（opencode 原生集成）

MetaForge 已在 opencode 环境原生集成，Agent 开箱即用：

| 消费载体 | 作用 |
|---------|------|
| `metaforge` 技能 | 引导 Agent 判断认知场景、选择模板、翻译结果 |
| `metaforge_cognition` 工具 | 按模板执行认知查询 |
| `metaforge_resolve` 工具 | 自然语言 → 精确 FQN（确定性匹配，绝不臆造） |
| `metaforge-consult` 子代理 | 封装"分诊→解析→查询→翻译"工作流，白名单权限 |

Agent 的执行链路：

```
用户诉求 → metaforge 技能分诊（选模板）
        → metaforge_resolve（NL → 精确 FQN）
        → metaforge_cognition（模板 + FQN + 算子 查询说明书）
        → 读业务数据对照规则 → 决策 → 翻译成自然语言答案
```

## 自建语义库（元数据管理）

内置 agent 库只是起点。使用者可通过**元数据管理接口**（元模型 + 元数据全生命周期）自建语义库：

```
1. POST /api/v1/metamodel/bundles         创建 Bundle（命名空间）
2. POST /api/v1/metamodel/entity-schemas  定义实体类型
3. POST /api/v1/metamodel/relation-schemas 定义关系（组合/关联/流程）
4. POST /api/v1/metadata/drafts           填充语义说明书实例（草稿）
5. POST /api/v1/metadata/activate         激活
6. POST /api/v1/metamodel/versions/{fqn}/publish  发布版本
7. 认知查询 scope.bundles=["my:bundle:1.0"]  按场景消费
```

- 多 Bundle 并存，`scope.bundles` 指定消费哪个语义库
- Bundle 版本固化 + 导出边界管控，语义可治理、可追溯

## 边界说明

- **内置算子的通用性**：画像（entity-profile）、直连（direct-link）、影响（impact-*）等算子按 FQN 通用查询，对自建 Bundle 的实体基本适用
- **场景算子的依赖**：流程/决策/委派类算子依赖 agent 库的关系语义（TaskHasEntry*、DecisionStepHasNext* 等），自建业务库需设计匹配的关系建模以复用这些算子

---

**下一篇**：[示例](./examples/medical-prescription-review.md)——看真实的说明书如何指导执行。
