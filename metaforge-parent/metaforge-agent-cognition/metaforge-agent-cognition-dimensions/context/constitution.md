<!--
================================================================================
  Sync Impact Report
================================================================================
  BC Constitution Version Change: N/A (placeholder skeleton) → 1.0.0
  Parent Global Constitution Version: 1.0.0
  Last Amended Date: 2026-08-11

  BC-Specific Principles:
    ✅ Added: I. 八维不可变分类体系 (MUST)
    ✅ Added: II. 算子独立扩展 (MUST)
    ✅ Added: III. 统一上游访问通道 (MUST)
    ✅ Added: IV. 只读与无主权 (MUST)
    ✅ Added: V. Scope 边界强制裁剪 (MUST)
    ✅ Added: VI. 失败不扩散 (SHOULD)
    ✅ Added: VII. 运行时解耦加载 (SHOULD)
    ✅ Added: VIII. 渐进式探索 (SHOULD)
    ✅ Added: IX. 确定性计算 (SHOULD)

  Custom Sections:
    ✅ Added: 算子扩展约束
    ✅ Added: 消费约束

  BC Overrides:
    ✅ None — No parent SHOULD/MAY overrides required.

  Template Synchronization:
    ✅ N/A — No BC-specific plan/spec templates exist; alignment not required.

  Deferred TODOs: None

  Rationale: Initial population of placeholder skeleton. All 9 principles directly
  derived from the cognition-dimensions module's responsibility as the cognitive
  operator implementation layer. The BC inherits all global MUST principles and
  adds no overrides.
================================================================================
-->

# agent-cognition-dimensions Bounded Context Constitution
<!-- BC-level governance constitution. Inherits all rules from the global system constitution as read-only baseline. -->
<!-- OVERRIDE RULE: Only SHOULD/MAY level principles from the parent global constitution can be overridden. MUST level principles CANNOT be modified or removed. -->

**BC Identity**: 认知算子实现层——负责 8 分类下所有 `CognitionOperator` 的具体实现，
通过 SPI 接口注册，由引擎核心运行时发现加载。不暴露 API 端点，不持有数据主权，
不持久化业务状态。

**Parent Version**: 1.0.0
<!-- Human-readable traceability marker only. No version alignment validation during AI merge. Records the global constitution version referenced when this BC constitution was created/last updated. -->

**模块**: `metaforge-agent-cognition-dimensions` — Maven 模块，编译依赖
`metaforge-agent-cognition-api`（`CognitionOperator` 接口 + `DimensionCategory` 枚举
+ Port 接口），运行时由 `-core` 注入 Port 实现 Bean。

---

## BC-Specific Principles
<!-- Exclusive core principles for this BC only, not inherited from the global constitution. Follow MUST/SHOULD/MAY level specification. Principle names must not duplicate global constitution principles. -->

### I. 八维不可变分类体系 (MUST)

认知算子分类体系为八个维度的封闭集合（本体/结构/关系/流程/约束/能力/认知/治理）。
任何新增算子必须归入既有分类，禁止创建新分类或修改现有分类的语义边界。
分类是算子注册、输出分组与运行时裁剪的唯一依据。

### II. 算子独立扩展 (MUST)

每个认知算子为一个独立的认知行为单元，通过 SPI 接口注册，各算子之间无依赖、无耦合。
新增算子只需实现接口并声明分类，无需修改引擎核心代码。
算子以 operatorId 被模板引用，分类由类上 category 声明决定。

### III. 统一上游访问通道 (MUST)

算子对上游客户端数据的访问统一收敛于只读 Port 接口，禁止直接注入上游 BC 的任意
Service。上游不可用时算子返回失败结果，不得使整体调用崩溃或泄露内部异常。

### IV. 只读与无主权 (MUST)

所有算子查询为只读操作，认知算子实现层不持有任何业务数据存储主权，
不保存任务上下文、快照、会话状态，仅存储自身配置。

### V. Scope 边界强制裁剪 (MUST)

每个算子执行时必须按认知边界过滤查询范围——只返回边界内的结果。
超出范围的实体不得出现在输出中，须显式标注为已跳过，供上层汇总。

### VI. 失败不扩散 (SHOULD)

算子执行失败时应返回失败结果对象，而非抛出异常。
标记为可选的算子失败不能终止整体调用，必须保证其他算子的正常执行与结果的聚合。

### VII. 运行时解耦加载 (SHOULD)

引擎核心不编译依赖算子实现层。算子通过容器运行时发现注入，
新增算子无需重新编译引擎核心，只需在实现层添加实现类并注册即可被自动发现。

### VIII. 渐进式探索 (SHOULD)

面向不确定认知场景的算子应返回惰性节点模式——每层携带"是否有子节点"及
"建议的下一次调用"提示，避免单次查询返回全量数据淹没消费方。

### IX. 确定性计算 (SHOULD)

所有算子逻辑为确定性计算，不依赖 LLM 或向量语义相似度作为实现前提。
结果可复现、可审计。

---

## 算子扩展约束
<!-- 定义新认知算子扩展的约束与流程规范。-->

- 新增算子 = 实现 CognitionOperator 接口 + 声明分类 + Spring 注册 + 模板中引用，
  四项缺一不可。
- 内置八维分类为封闭集合，新增分类须修订本宪法并升级 MAJOR 版本。
- 算子 operatorId 全局唯一，命名格式为 `{分类}.{能力名}`。
- 算子注册在启动时完成，校验失败（分类非法、operatorId 重复）的算子记录告警
  并跳过，不阻塞引擎启动。

## 消费约束
<!-- 定义算子消费与模板组合调用的约束规范。-->

- 算子不独立暴露 API 端点，所有消费通过模板层组合调用。
- 模板以 operatorId 引用算子，同时声明优先级、是否必须、超时、适用原型白名单。
- 算子本身不关心自己被哪个模板消费，也不依赖模板上下文。

---

## BC Overrides
<!-- Selective override of parent global constitution SHOULD/MAY level principles only. Each entry must reference the exact parent principle name + explicit override rationale. MUST level principles are forbidden to be overridden here. -->

_本 BC 无需覆盖任何父宪法 SHOULD/MAY 级原则。_

---

**BC Constitution Version**: 1.0.0 | **Created**: 2026-08-11 | **Last Amended**: 2026-08-11
<!-- BC constitution has independent semantic versioning, decoupled from global constitution version. -->
