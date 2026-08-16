<!--
================================================================================
  Sync Impact Report
================================================================================
  Version Change: N/A (placeholder) → 1.0.0
  Parent Global Constitution Version: 1.0.0

  Principle Summary:
    ✅ Added: I. 纯声明配置层定位 (MUST)
    ✅ Added: II. 场景定义自包含 (MUST)
    ✅ Added: III. 能力引用合法性 (MUST)
    ✅ Added: IV. 配置即数据解耦 (MUST)
    ✅ Added: V. 增量式模板演进 (SHOULD)
    ✅ Added: VI. 边界行为显式声明 (SHOULD)
    ✅ Added: VII. 只声明不实现 (MUST)
    ✅ Added: VIII. 生命周期与版本治理 (SHOULD)

  Custom Sections:
    ✅ Added: 模板扩展与校验约束
    ✅ Added: 配置稳定性约束
    ❌ Removed: [SECTION_2_NAME] → renamed to 模板扩展与校验约束
    ❌ Removed: [SECTION_3_NAME] → renamed to 配置稳定性约束

  Override Entries:
    ✅ Added: None
    ✅ Modified: None
    ❌ Removed: Placeholder Override 1, Override 2 — no actual overrides declared

  Deferred TODOs: None

  Rationale: Initial concrete population of the BC constitution skeleton.
  All 8 BC-specific principles and 2 dedicated constraint sections have been
  populated from declarative configuration requirements. No global MUST-level
  overrides exist. The BC explicitly declares zero parent principle overrides.
================================================================================
-->

# agent-cognition-templates Bounded Context Constitution
<!-- BC-level governance constitution. Inherits all rules from the global system constitution as read-only baseline. -->
<!-- OVERRIDE RULE: Only SHOULD/MAY level principles from the parent global constitution can be overridden. MUST level principles CANNOT be modified or removed. -->

**Parent Version**: 1.0.0
<!-- Human-readable traceability marker only. No version alignment validation during AI merge. Records the global constitution version referenced when this BC constitution was created/last updated. -->

---

## BC-Specific Principles
<!-- Exclusive core principles for this BC only, not inherited from the global constitution. Follow MUST/SHOULD/MAY level specification. Principle names must not duplicate global constitution principles. -->

### I. 纯声明配置层定位 (MUST)

本 BC 为纯声明式配置层，只承载认知模板的定义，不含任何可执行逻辑、领域逻辑或运行状态。
全部产物为供引擎消费的配置数据，对引擎核心而言是"数据"而非"代码"。

### II. 场景定义自包含 (MUST)

每个模板必须是自包含、可独立解析的 Agent 消费场景，完整声明其消费的认知能力组合、
入参契约、边界行为与输出结构。任一关键要素缺失，该模板即视为无效定义，不得对外服务。

### III. 能力引用合法性 (MUST)

模板只能引用已被引擎注册的认知能力，声明的能力集合不得为空，可用原型范围必须是
封闭枚举的子集。引用不存在或声明非法的模板不得注册启用，且校验失败不得影响
其他已注册模板。

### IV. 配置即数据解耦 (MUST)

模板对引擎核心是"数据"而非"代码"，经运行时扫描发现加载；本 BC 与引擎核心层、
能力实现层之间互不编译依赖。模板的任何演进不得要求修改其他模块。

### V. 增量式模板演进 (SHOULD)

新增或调整消费场景必须在本 BC 内以新增/调整声明文件的方式完成，无需触碰其他模块。
模板标识全局唯一、命名规范统一，保证注册幂等、演进可控、可安全回退。

### VI. 边界行为显式声明 (SHOULD)

每个模板必须显式声明其对认知边界的接受、必填、产出与生效范围，不得隐式依赖缺省行为。
模板执行严格受边界约束，越界内容不得出现在输出中，并须显式标注被裁剪/跳过的范围。

### VII. 只声明不实现 (MUST)

模板内容只表达消费场景"是什么"（能力、契约、边界、输出结构），绝不表达"怎么做"。
模板为确定性配置，不承载智能推断、可执行逻辑或语义相似度语义。

### VIII. 生命周期与版本治理 (SHOULD)

模板须记录版本并支持启停控制；停用模板不得被路由消费，但保留注册信息。
任一模板的生命周期变更不得中断其他已注册模板的正常服务。

---

## 模板扩展与校验约束
<!-- 定义新认知模板扩展的约束与流程规范。-->

- 新增模板只在本 BC 内新增一个声明文件，不修改引擎核心与算子实现层代码。
- 模板标识全局唯一，遵循统一命名规范；同一标识不得重复注册。
- 模板必须完整声明场景全部关键要素，缺任一要素即为无效模板。
- 模板引用的认知能力必须已注册、可用原型必须为封闭枚举子集，否则该模板校验失败。
- 校验失败的模板不得注册、不得对外服务，记录告警并跳过，不得污染或影响已注册模板。

---

## 配置稳定性约束
<!-- 本 BC 配置纯度与运行边界声明。-->

- 模板内容为确定性配置，仅声明场景与契约，不承载任何实现逻辑或可执行产物。
- 本 BC 无状态运行，不保存会话、快照、任务上下文，不持有任何业务数据存储主权。
- 模板与引擎核心、算子实现层之间互不编译依赖，全部经运行时扫描发现加载。
- 模板声明与实现彻底分离：消费场景如何被解读与执行，由引擎核心与算子层负责。

---

## BC Overrides
<!-- Selective override of parent global constitution SHOULD/MAY level principles only. Each entry must reference the exact parent principle name + explicit override rationale. MUST level principles are forbidden to be overridden here. -->

_本 BC 无需覆盖任何父宪法 SHOULD/MAY 级原则。_

---

**BC Constitution Version**: 1.0.0 | **Created**: 2026-08-11 | **Last Amended**: 2026-08-11
<!-- BC constitution has independent semantic versioning, decoupled from global constitution version. -->
