# Java 运行时诊断（example-java-runtime-diagnostics）

**场景概述**：Java 运行时诊断 Agent 处理 JVM 性能/稳定性问题诊断任务——从 MetaForge 的 `java-diag` 独立 Bundle 获取**诊断方法论语义**（现象→指标→分类→采样→证据→定位→根因→优化→验证 九阶段链路），读取独立存放的 JVM 运行时采集样本（CPU 高 / Full GC / OOM / 内存泄漏 / 线程池耗尽 / ClassLoader 泄漏 6 大场景），对照诊断链给出定位与处置建议。

核心验证：**MetaForge 的"跨 Bundle 垂直领域"能力**——`metaforge:1.0.0` 提供 Agent 通用元模型（任务/流程/规则/能力），`java-diag:1.0.0` 提供 Java 运行时领域知识（九阶段诊断链路），两个 Bundle 通过 `bundle_dependency` 关联、按 `scope.bundles` 独立消费，六能力（DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE）对垂直 Bundle 的领域实体**零代码改动直接可用**。

## 目录结构

```
examples/example-java-runtime-diagnostics/
├── README.md                     # 本文件
├── seed-java-diag.sql            # java-diag Bundle seed（06 元模型 + 07 场景实例 + 域树 + 关系索引 合并版）
├── data/incidents/               # 业务数据（JVM 运行时采集样本，独立存放）
│   ├── inc-cpu-001.json          #   CPU 高（order-service，线程锁竞争）
│   ├── inc-fgc-001.json          #   Full GC（gateway-service，缓存无上限）
│   ├── inc-oom-001.json          #   OOM（user-service，缓冲区未释放）
│   ├── inc-leak-001.json         #   内存泄漏（search-service，ThreadLocal 未清理）
│   ├── inc-pool-001.json         #   线程池耗尽（pay-service，外部依赖无超时）
│   └── inc-cl-001.json           #   ClassLoader 泄漏（admin-console，静态引用持有）
└── test-cases.md                 # 测试用例（JD-1 ~ JD-10）
```

## 核心概念：通用元模型 vs 垂直领域知识

| | 归属 | 内容 |
|---|------|------|
| **通用元模型（形状）** | `metaforge:1.0.0.agent/common/protocol` | Task/Step/DecisionStep/Capability/ExecutionRule 及领域层级 schema |
| **垂直领域知识（内容）** | `java-diag:1.0.0.diag` | 九阶段诊断链：现象→指标→分类→采样→证据→定位→根因→优化→验证 |

**关键设计**：java-diag Bundle 只建自己的**领域元模型**（9 个诊断阶段 EntitySchema + 8 条链边 RelationSchema），诊断实例的 `entity_schema_fqn`/`relation_schema_fqn` 指向 `java-diag:1.0.0.diag.*`，域树实例则跨 Bundle 复用 `metaforge:1.0.0.common.*` schema。二者通过 `bundle_dependency (java-diag:1.0.0 → metaforge:1.0.0)` 声明关联。

## 诊断方法论模型（九阶段链路）

```
现象(Symptom) ──▶ 指标(Metric) ──▶ 分类(Classification) ──▶ 采样(Sampling)
      ──▶ 证据(Evidence) ──▶ 定位(Location) ──▶ 根因(RootCause)
      ──▶ 优化(Optimization) ──▶ 验证(Verification)
```

- 9 个 EntitySchema（`java-diag:1.0.0.diag.*`），每阶段带垂直属性（severity/unit/normal_range/tool/confidence/root_cause_type…）
- 8 条链边 RelationSchema（`PROCESS_SEQUENCE`），方向即诊断推进方向
- 6 大场景各实例化一条完整九阶段链（见 `docker/seed/07-java-diag-scenarios.sql`）

## 快速开始

```bash
# 1. 应用 java-diag Bundle seed（含元模型 + 场景实例 + 域树 + 关系索引，单文件）
export PGPASSWORD=metaforge
psql -h localhost -U metaforge -d metaforge \
  -f examples/example-java-runtime-diagnostics/seed-java-diag.sql

# 2. 端到端执行（CPU 高诊断）
cd /data/ext/source-8/metaforge
opencode run "你是 Java 运行时诊断 Agent，执行 CPU 高诊断：ORIENT 下钻『Java 运行时域组』定位现象 → BRIEF 查『CPU 使用率持续高』拿九阶段诊断链 → 读 examples/example-java-runtime-diagnostics/data/incidents/inc-cpu-001.json → 对照诊断链报告证据/定位/根因/优化建议"
```

## 六能力消费示例（对 java-diag Bundle 零改动可用）

| 能力 | 查询 | 作用 |
|------|------|------|
| DISCOVER | bundle-discovery | 发现 `java-diag` Bundle（system:false） |
| ORIENT | domain-drilldown | 域组→诊断域→6 大现象下钻 |
| BRIEF | entity-profile + flow-blueprint + direct-link | 现象全景 + 九阶段链路 + 直连关系 |
| GUIDE | entity-profile + adjacent-step + direct-link | 单阶段执行指引（如采样步骤的上下游） |
| FORECAST | impact-forward/backward + risk-assessment | 变更某阶段的影响链路（根因→优化→验证） |
| DELEGATE | scope-narrowing | 子任务认知边界（预留） |

详见 [test-cases.md](test-cases.md)。
