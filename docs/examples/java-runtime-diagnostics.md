# 示例六：Java 运行时诊断（独立 Bundle 垂直领域）

场景：Java 运行时诊断 Agent 处理 JVM 性能/稳定性问题诊断任务——从独立 Bundle `java-diag:1.0.0` 获取九阶段诊断方法论（现象→指标→分类→采样→证据→定位→根因→优化→验证），读取独立存放的 JVM 采集样本，对照诊断链定位与处置。**这是首个验证"跨 Bundle 垂直领域"的示例**——通用元模型（metaforge）与领域知识（java-diag）分离、正交组合。

## 场景概述

- **Agent 角色**：Java 运行时诊断 Agent
- **业务目标**：对 CPU 高 / Full GC / OOM / 内存泄漏 / 线程池耗尽 / ClassLoader 泄漏 6 大场景，沿九阶段诊断链定位根因并给出优化
- **MetaForge 角色**：`java-diag` Bundle 提供诊断方法论语义（九阶段 EntitySchema + 链边 RelationSchema + 6 场景实例）；`metaforge` Bundle 提供通用元模型（任务/流程/规则/能力 schema）
- **核心亮点**：跨 Bundle 垂直领域——`scope.bundles=["java-diag:1.0.0"]` 独立消费，六能力零代码改动

## 语义说明书（存 MetaForge）

### 诊断方法论模型（java-diag 自建元模型）

```
java-diag:1.0.0.diag（9 阶段诊断链路）
  现象(Symptom) → 指标(Metric) → 分类(Classification) → 采样(Sampling)
  → 证据(Evidence) → 定位(Location) → 根因(RootCause)
  → 优化(Optimization) → 验证(Verification)
```

- 9 个 EntitySchema，每阶段带垂直属性（如 Metric.unit/normal_range、Sampling.tool/command、RootCause.root_cause_type/confirmed）
- 8 条链边 RelationSchema（PROCESS_SEQUENCE）：`SymptomManifestsAsMetric` → `MetricClassifiedBy` → `ClassificationRequiresSampling` → `SamplingProducesEvidence` → `EvidenceLocates` → `LocationRevealsRootCause` → `RootCauseSuggestsOptimization` → `OptimizationValidatedBy`

### 6 大场景实例（每场景一条九阶段链）

```
S1 CPU 高：       锁竞争/忙等 → 锁粒度细化 → CPU 回落验证
S2 Full GC：      缓存无上限 → 加容量上限+LRU → GC 频率回落
S3 OOM：          缓冲区未释放 → 池化缓冲区 → 无 OOM
S4 内存泄漏：     ThreadLocal 未清理 → finally remove+弱引用 → 内存平坦
S5 线程池耗尽：   外部依赖无超时 → 加超时+熔断 → 队列恢复
S6 ClassLoader泄漏：静态引用持有 ClassLoader → 清理引用 → Metaspace 平稳
```

### 域树（ORIENT 定位支撑）

```
Java 运行时域组 (java-diag:1.0.0.common.Group_JavaRuntime)   [复用 metaforge common schema]
└── Java 运行时诊断域 (Domain_JavaDiagnosis)
    └── 6 大现象 (Symptom_*)
```

## 通用 vs 垂直（本次示例的核心证明）

| 层 | 归属 Bundle | 内容 |
|----|------------|------|
| **通用元模型（形状）** | `metaforge:1.0.0` | Task/Step/DecisionStep/Capability/ExecutionRule 等 schema |
| **垂直领域知识（内容）** | `java-diag:1.0.0` | 九阶段诊断链路 EntitySchema/RelationSchema + 场景实例 |

- `bundle_dependency (java-diag:1.0.0 → metaforge:1.0.0)` 声明复用
- 六能力按 `scope.bundles` 过滤，垂直实体 FQN 前缀 `java-diag:1.0.0.*` 即可被消费
- 通用算子（entity-profile/flow-blueprint/direct-link/impact-*）按 FQN 图查询，对自建 schema 的实例基本通用

## 业务数据（独立存放）

```
examples/example-java-runtime-diagnostics/data/incidents/
├── inc-cpu-001.json    { cpu_usage_pct: 94.2, jstack: 412 线程 BLOCKED @ OrderService#processLine }
├── inc-fgc-001.json    { FGC: 48/h, old: 96.8%, jstat FGCT: 29.8 }
├── inc-oom-001.json    { heap: 8.7/8.0GB, byte[]: 54%, stack: MessageConsumer#onMessage }
├── inc-leak-001.json   { heap +180MB/h, 引用链: TraceFilter->ThreadLocal<RequestContext> }
├── inc-pool-001.json   { queue: 4850, 200 worker BLOCKED @ PaymentClient#notify }
└── inc-cl-001.json     { classes: 182k, 3 个旧 ClassLoader 被静态引用 }
```

## 决策链路（Agent 对照说明书执行）

```
1. ORIENT 下钻：域组 → 诊断域 → 6 大现象（定位入口）
2. BRIEF 拿说明书：九阶段诊断链 + 阶段属性（阈值/工具/根因类型）
3. 读 inc-cpu-001.json → CPU 94.2%、412 线程 BLOCKED
4. 对照诊断链：证据(线程栈)→定位(锁点)→根因(锁竞争)→优化(锁粒度细化)
```

## 关键结论

| 场景 | 采集数据特征 | 诊断结论 |
|------|-------------|---------|
| CPU 高 | 94.2%，412 线程 BLOCKED | 锁竞争 → 锁粒度细化 |
| Full GC | 48 次/h，老年代 96.8% | 缓存无上限 → 加容量上限+LRU |
| OOM | heap 8.7/8.0GB，byte[] 54% | 缓冲区未释放 → 池化缓冲区 |
| 内存泄漏 | +180MB/h，Full GC 不回落 | ThreadLocal 未清理 → remove+弱引用 |
| 线程池耗尽 | 队列 4850，worker 全 BLOCKED | 外部依赖无超时 → 加超时+熔断 |
| ClassLoader 泄漏 | 类 182k，Metaspace 1.5GB | 静态引用持有 → 清理引用 |

所有阈值（70%/5 次/h/Xmx 80% 等）、工具命令（jstack/jmap/jstat/jcmd）、根因类型均来自 MetaForge 说明书——不是模型先验，而是可查询的语义依据。

## 快速验证

```bash
cd /data/ext/source-8/metaforge
opencode run "你是 Java 运行时诊断 Agent，执行 CPU 高诊断任务：resolve『CPU 使用率持续高』→ BRIEF 拿九阶段诊断链 → 读 examples/example-java-runtime-diagnostics/data/incidents/inc-cpu-001.json → 对照诊断链报告证据/定位/根因/优化。"
```

预期：CPU 高 → 证据（线程 BLOCKED）→ 定位（锁点）→ 根因（锁竞争）→ 优化（锁粒度细化）。

详见 `examples/example-java-runtime-diagnostics/test-cases.md`（JD-1 ~ JD-10）。
