# test6 测试用例：Java 运行时诊断（独立 Bundle 垂直领域）

## 测试环境

- MetaForge 服务端：`http://localhost:8080`（6 模板已注册）
- Bundle seed 已应用：`docker/seed/06-java-diag.sql`（元模型）+ `docker/seed/07-java-diag-scenarios.sql`（场景实例 + 域树 + 关系索引）
- 业务数据：`examples/example-java-runtime-diagnostics/data/incidents/{inc-cpu-001,inc-fgc-001,inc-oom-001,inc-leak-001,inc-pool-001,inc-cl-001}.json`
- 测试目录：`/data/ext/source-8/metaforge`

> 验证前先确认 boot 存活：`curl http://localhost:8080/actuator/health` 应返回 `UP`。
> 以下 curl 均用 `scope.bundles=["java-diag:1.0.0"]`，证明**独立 Bundle 按 scope 独立消费**。

---

## JD-1 Bundle 发现：java-diag 独立 Bundle 可见

**命令**：
```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/DISCOVER \
  -H 'Content-Type: application/json' -d '{
    "scope":{"bundles":[],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params":{"selectOperators":["ontological.bundle-discovery"]},
    "format":"JSON","cognitionDepth":"L1","agentArchetype":"EXPLORATION"}' | python3 -m json.tool
```

**预期**：`dimensions[0].data` 含两个 Bundle：`metaforge`（system:true）+ `java-diag`（system:false，owner=jvm-team）。

**通过标准**：自建 Bundle 出现在 DISCOVER 结果中。

---

## JD-2 域定位：Java 运行时域组 → 诊断域 → 6 大现象

**命令**（三层下钻）：
```bash
# 顶层：域组
curl -s -X POST http://localhost:8080/api/v1/cognition/ORIENT \
  -H 'Content-Type: application/json' -d '{
    "scope":{"bundles":["java-diag:1.0.0"]},
    "params":{"selectOperators":["ontological.domain-drilldown"]},
    "format":"JSON","cognitionDepth":"L1","agentArchetype":"EXPLORATION"}'

# 下钻域组 → 诊断域
curl -s -X POST http://localhost:8080/api/v1/cognition/ORIENT \
  -H 'Content-Type: application/json' -d '{
    "scope":{"bundles":["java-diag:1.0.0"]},
    "params":{"selectOperators":["ontological.domain-drilldown"],
              "parent_fqn":"java-diag:1.0.0.common.Group_JavaRuntime"},
    "format":"JSON","cognitionDepth":"L1","agentArchetype":"EXPLORATION"}'

# 下钻诊断域 → 6 大现象
curl -s -X POST http://localhost:8080/api/v1/cognition/ORIENT \
  -H 'Content-Type: application/json' -d '{
    "scope":{"bundles":["java-diag:1.0.0"]},
    "params":{"selectOperators":["ontological.domain-drilldown"],
              "parent_fqn":"java-diag:1.0.0.common.Domain_JavaDiagnosis"},
    "format":"JSON","cognitionDepth":"L1","agentArchetype":"EXPLORATION"}'
```

**预期**：第三层返回 `children_grouped` 中 `java-diag:1.0.0.diag.Symptom` 类型下 6 个现象（CPU高/FullGC/OOM/内存泄漏/线程池耗尽/ClassLoader泄漏）。

**通过标准**：垂直 Bundle 的领域实体可被 ORIENT 域树下钻发现。

---

## JD-3 现象全景：BRIEF 查 CPU 高（九阶段链路 + 画像 + 直连）

**命令**：
```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/BRIEF \
  -H 'Content-Type: application/json' -d '{
    "scope":{"bundles":["java-diag:1.0.0"]},
    "params":{"entity_fqn":"java-diag:1.0.0.diag.Symptom_CPUHigh"},
    "format":"JSON","cognitionDepth":"L2","agentArchetype":"EXECUTION"}' | python3 -m json.tool
```

**预期**：
- `entity-profile`：CPU 使用率持续高，severity=CRITICAL，service=order-service
- `flow-blueprint`：九阶段链路完整（Metric_CPUUsage → Classification_CPUCategory → Sampling_ThreadDump → Evidence_BusyThreads → Location_LockPoint → RootCause_LockContention → Optimization_ReduceLockScope → Verification_CPUStable）
- `adjacent-step` / `direct-link`：直连关系正常

**通过标准**：通用算子（entity-profile/flow-blueprint/direct-link）对自建 schema 的实例零改动可用。

---

## JD-4 单阶段指引：GUIDE 查采样步骤

**命令**：
```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/GUIDE \
  -H 'Content-Type: application/json' -d '{
    "scope":{"bundles":["java-diag:1.0.0"]},
    "params":{"entity_fqn":"java-diag:1.0.0.diag.Sampling_ThreadDump",
              "selectOperators":["ontological.entity-profile","procedural.adjacent-step","relational.direct-link"]},
    "format":"JSON","cognitionDepth":"L2","agentArchetype":"EXECUTION"}' | python3 -m json.tool
```

**预期**：`entity-profile` 返回 tool=jstack、command=`jstack -l <pid>`；`adjacent-step` 返回上一步（分类）与下一步（证据）。

**通过标准**：GUIDE 对垂直采样实体返回可执行的工具命令上下文。

---

## JD-5 变更影响：FORECAST 查根因节点（核心用例）

**命令**：
```bash
curl -s -X POST http://localhost:8080/api/v1/cognition/FORECAST \
  -H 'Content-Type: application/json' -d '{
    "scope":{"bundles":["java-diag:1.0.0"]},
    "params":{"entity_fqn":"java-diag:1.0.0.diag.RootCause_LockContention",
              "change_type":"MODIFY","max_depth":4},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION"}' | python3 -m json.tool
```

**预期**：
- `impact-backward`：`totalImpacted=5`，追溯上游 Classification → Sampling → Evidence → Location
- `impact-forward`：`totalImpacted=3`，扩散下游 Optimization → Verification
- `risk-assessment`：返回 risk_level 与建议

**通过标准**：FORECAST 沿垂直领域链边（PROCESS_SEQUENCE）做影响扩散——**依赖 compute-engine 的 `entity_relation_index`，seed 必须同步该索引（07 文件末尾已含）**。

---

## JD-6 端到端：CPU 高诊断（说明书指导执行）—— 核心用例

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是 Java 运行时诊断 Agent，执行 CPU 高诊断任务：
1) resolve『CPU 使用率持续高』拿 FQN（java-diag:1.0.0.diag.Symptom_CPUHigh）；
2) BRIEF 查该现象拿九阶段诊断链（现象→指标→分类→采样→证据→定位→根因→优化→验证）；
3) 读 examples/example-java-runtime-diagnostics/data/incidents/inc-cpu-001.json（CPU 94.2%，jstack 显示 412 线程 BLOCKED 在 OrderService#processLine）；
4) 对照诊断链报告：证据、定位、根因、优化建议。报告：FQN、采集指标值、诊断结论。"
```

**预期结论**：
- 指标：cpu_usage_pct=94.2%（>70% 异常），thread_count=860
- 证据：412 线程 BLOCKED，栈顶集中在 `OrderService#processLine:214`（Evidence_BusyThreads）
- 定位：锁竞争位置 order-service（Location_LockPoint）
- 根因：锁竞争/忙等（RootCause_LockContention）
- 优化：锁粒度细化 + 移除忙等自旋（Optimization_ReduceLockScope）

**通过标准**：Agent 能精确报告虚构的诊断链 + 阈值 + 采集数据，**全部来自 metaforge 说明书与独立数据，非 LLM 先验**。

---

## JD-7 端到端：Full GC 诊断（缓存无上限）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是 Java 运行时诊断 Agent，执行 Full GC 诊断任务：
1) resolve『频繁 Full GC』拿 FQN；
2) BRIEF 查诊断链（重点：指标→分类→采样→证据→定位→根因）；
3) 读 examples/example-java-runtime-diagnostics/data/incidents/inc-fgc-001.json（Full GC 48 次/h，老年代 96.8%，jstat FGCT=29.8）；
4) 报告：分类候选、证据、定位、根因、优化建议。"
```

**预期结论**：分类候选 [内存泄漏/大对象/堆配置过小/元空间不足] → 证据（缓存对象占堆 70%）→ 根因（缓存无上限）→ 优化（加容量上限 + LRU）。

**通过标准**：Full GC 场景的九阶段链正确引导诊断。

---

## JD-8 端到端：OOM 诊断（缓冲区未释放）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是 Java 运行时诊断 Agent，执行 OOM 诊断任务：
1) resolve『OutOfMemoryError』拿 FQN；
2) BRIEF 查诊断链；
3) 读 examples/example-java-runtime-diagnostics/data/incidents/inc-oom-001.json（heap 8.7/8.0GB，byte[] 占 54%，堆栈指向 MessageConsumer#onMessage）；
4) 报告：OOM 类型分类、证据、定位、根因、优化建议。"
```

**预期结论**：分类 [堆OOM/元空间OOM/直接内存OOM/线程栈OOM] → 证据（byte[]/String 占堆 90%）→ 定位（MessageConsumer#onMessage）→ 根因（缓冲区未释放）→ 优化（池化缓冲区）。

**通过标准**：OOM 场景诊断链引导至正确处置。

---

## JD-9 端到端：内存泄漏诊断（ThreadLocal 未清理）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是 Java 运行时诊断 Agent，执行内存泄漏诊断任务：
1) resolve『内存持续增长』拿 FQN；
2) BRIEF 查诊断链；
3) 读 examples/example-java-runtime-diagnostics/data/incidents/inc-leak-001.json（堆 180MB/h 增长，Full GC 后不回落，引用链 TraceFilter#doFilter->ThreadLocal<RequestContext>）；
4) 报告：泄漏类型、证据、定位、根因、优化建议。"
```

**预期结论**：分类 [堆泄漏/元空间泄漏/直接内存泄漏] → 证据（ThreadLocal 持有大量过期对象）→ 定位（TraceFilter#doFilter）→ 根因（ThreadLocal 未清理）→ 优化（finally remove + 弱引用）。

**通过标准**：内存泄漏场景诊断链定位到 ThreadLocal 泄漏源。

---

## JD-10 端到端：线程池耗尽诊断（外部依赖无超时）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是 Java 运行时诊断 Agent，执行线程池耗尽诊断任务：
1) resolve『线程池任务堆积』拿 FQN；
2) BRIEF 查诊断链；
3) 读 examples/example-java-runtime-diagnostics/data/incidents/inc-pool-001.json（队列 4850，200 worker 全 BLOCKED，PaymentClient#notify 无超时）；
4) 报告：阻塞分类、证据、定位、根因、优化建议。"
```

**预期结论**：分类 [外部依赖慢/IO阻塞/死锁] → 证据（全部 worker 阻塞在无超时 HTTP）→ 定位（PaymentClient#notify）→ 根因（外部依赖无超时）→ 优化（加超时 + 熔断）。

**通过标准**：线程池耗尽场景诊断链定位到阻塞调用点。

---

## JD-11 端到端：ClassLoader 泄漏诊断（静态引用持有）

**命令**：
```bash
cd /data/ext/source-8/metaforge
opencode run "你是 Java 运行时诊断 Agent，执行 ClassLoader 泄漏诊断任务：
1) resolve『类加载数量持续增长』拿 FQN；
2) BRIEF 查诊断链；
3) 读 examples/example-java-runtime-diagnostics/data/incidents/inc-cl-001.json（类 18.2 万，Metaspace 1.5/2.0GB，14 次部署，引用链 PluginRegistry#handlers->PluginHandler->PluginClassLoader）；
4) 报告：泄漏分类、证据、定位、根因、优化建议。"
```

**预期结论**：分类 [重部署未卸载/动态生成类/静态引用持有] → 证据（旧 ClassLoader 被全局静态字段持有）→ 定位（PluginRegistry#handlers）→ 根因（静态引用持有 ClassLoader）→ 优化（卸载时清理引用 + 工具类下沉共享 ClassLoader）。

**通过标准**：ClassLoader 泄漏场景诊断链定位到静态持有点。

---

## 测试记录表

| 用例 | 结果（PASS/FAIL） | 备注 |
|------|------------------|------|
| JD-1 Bundle 发现（java-diag 可见） | PASS | DISCOVER 返回 java-diag（system:false） |
| JD-2 域下钻（域组→域→6 现象） | PASS | ORIENT 三层下钻均命中 |
| JD-3 现象全景（BRIEF 九阶段链） | PASS | flow-blueprint 完整九阶段链路 |
| JD-4 单阶段指引（GUIDE 采样） | PASS | jstack 工具命令 + 上下游导航 |
| JD-5 变更影响（FORECAST 根因节点） | PASS | backward=5 / forward=3 节点扩散 |
| JD-6 CPU 高端到端（核心） | PASS | 九阶段链完整，定位锁竞争→锁粒度细化 |
| JD-7 Full GC 端到端 | PASS | 分类排除元空间，根因缓存无上限→LRU |
| JD-8 OOM 端到端 | PASS | 堆OOM，根因缓冲区未释放→池化 |
| JD-9 内存泄漏端到端 | PASS | ThreadLocal未清理→finally remove+弱引用 |
| JD-10 线程池耗尽端到端 | PASS | 外部依赖无超时→加超时+熔断 |
| JD-11 ClassLoader 泄漏端到端 | PASS | 静态引用持有→清理注册表引用 |

---

## 验收结论（写在此处）

> 本测试证明：MetaForge 支持**独立 Bundle 垂直领域**——`java-diag:1.0.0` 自建九阶段诊断链路元模型，
> 复用 `metaforge:1.0.0` 通用 agent/common 元模型（bundle_dependency 关联），
> 业务数据（JVM 采集样本）独立存放；六能力（DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE）
> 对该垂直 Bundle 的领域实体**零代码改动**即可消费。
> 诊断方法论是"领域知识"（java-diag），任务/流程/规则/能力是"执行语义"（metaforge）——
> **通用性在元模型形状，垂直性在领域内容**，二者正交组合。
>
> 关键实现细节：`relation_instance` 直插 SQL 后必须同步 `entity_relation_index` 双向索引，
> 否则 compute-engine 图遍历（FORECAST 影响扩散）返回空——见 seed 文件末尾的索引同步段。
> 实测：JD-1 ~ JD-11 全部通过（2026-08-27）——JD-1~5 用 curl 直接验证，JD-6~11 用 opencode 端到端验证（Agent 均完整走通九阶段诊断链并给出正确结论）。
