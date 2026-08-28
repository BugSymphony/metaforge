-- ============================================================================
-- 06-java-diag.sql — Java 运行时诊断域元模型 Bundle（独立 Bundle）
--
-- 定位：垂直领域"领域知识"库，复用 metaforge:1.0.0 的 agent/common/protocol 元模型，
--       自身只承载诊断方法论模型（9 阶段诊断链路）。
--
-- 诊断方法论（适用于 CPU 高 / Full GC / OOM / 内存泄漏 / 线程池耗尽 / ClassLoader 泄漏）：
--
--   现象(Symptom) ↓ 指标(Metric) ↓ 分类(Classification) ↓ 采样(Sampling)
--   ↓ 证据(Evidence) ↓ 定位(Location) ↓ 根因(RootCause)
--   ↓ 优化(Optimization) ↓ 验证(Verification)
--
-- 关联类型说明：链路 8 条边统一用 PROCESS_SEQUENCE（流程时序）——
--   诊断是"有序的方法论流程"，下游阶段依赖上游阶段产出，方向即诊断推进方向。
--   影响/邻域遍历（FORECAST）按出入边 BFS，与类型无关，故选择不改变可查询性。
-- ============================================================================

-- ============================================================
-- 1. Bundle & BundleVersion
-- ============================================================
INSERT INTO metamodel_governance.bundle (fqn, name, description, owner, is_system)
VALUES ('java-diag', 'Java 运行时诊断', 'Java 运行时问题诊断方法论语义库：覆盖 CPU 高、Full GC、OOM、内存泄漏、线程池耗尽、ClassLoader 泄漏等场景', 'jvm-team', FALSE)
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metamodel_governance.bundle_version (fqn, bundle_fqn, status, source_version_fqn, upgrade_level)
VALUES ('java-diag:1.0.0', 'java-diag', 'PUBLISHED', NULL, NULL)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 2. Package（diag — 诊断方法论模型）
-- ============================================================
INSERT INTO metamodel_governance.package (fqn, bundle_version_fqn, parent_package_fqn, description, depth)
VALUES ('java-diag:1.0.0.diag', 'java-diag:1.0.0', NULL, '诊断方法论模型：9 阶段诊断链路（现象→指标→分类→采样→证据→定位→根因→优化→验证）', 0)
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 3. EntitySchema（9 个诊断阶段）
-- ============================================================

-- 3.1 Symptom（现象）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('java-diag:1.0.0.diag.Symptom', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'Symptom', '诊断现象的入口——用户感知/监控告警的异常表现，如 CPU 持续高、频繁 Full GC、OOM、内存泄漏、线程池耗尽、ClassLoader 泄漏。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"severity","type":"ENUM","required":true,"description":"现象严重级别","enum_values":["CRITICAL","HIGH","MEDIUM","LOW"]},
    {"name":"service","type":"STRING","required":false,"description":"发生现象的服务/应用"},
    {"name":"trigger_condition","type":"STRING","required":false,"description":"触发条件——如 CPU 持续>90%"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 3.2 Metric（指标）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('java-diag:1.0.0.diag.Metric', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'Metric', '表征现象的量化指标——CPU 使用率、GC 频率/耗时、堆内存占用、线程数、类加载数量等。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"unit","type":"STRING","required":false,"description":"指标单位——如 %、次/min、MB"},
    {"name":"collect_method","type":"STRING","required":false,"description":"采集方式——如 jstat/jmx/prometheus/arthas"},
    {"name":"normal_range","type":"STRING","required":false,"description":"正常范围——如 <70%"},
    {"name":"abnormal_direction","type":"ENUM","required":false,"description":"异常方向","enum_values":["HIGH","LOW","EITHER"]}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 3.3 Classification（分类）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('java-diag:1.0.0.diag.Classification', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'Classification', '对现象/指标的分类判别——如 CPU 高分为用户态/内核态/GC 线程/锁竞争；Full GC 分为内存泄漏/大对象/堆配置过小。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"dimension","type":"STRING","required":false,"description":"分类维度——如 CPU 高 按线程状态分类"},
    {"name":"candidates","type":"ARRAY<STRING>","required":false,"description":"候选类别列表"},
    {"name":"criteria","type":"STRING","required":false,"description":"判别条件——如何判定属于某类别"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 3.4 Sampling（采样）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('java-diag:1.0.0.diag.Sampling', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'Sampling', '针对分类假设的采样动作——线程转储、堆转储、GC 日志、CPU profiling 等。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"tool","type":"STRING","required":false,"description":"采样工具——如 jstack/jmap/jstat/perf/async-profiler"},
    {"name":"command","type":"STRING","required":false,"description":"采样命令——如 jstack -l <pid>"},
    {"name":"target","type":"STRING","required":false,"description":"采集对象——线程/堆/GC/类加载器"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 3.5 Evidence（证据）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('java-diag:1.0.0.diag.Evidence', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'Evidence', '采样产出的证据——某线程长时间 BLOCKED、某类实例占据堆 80%、GC 日志显示频繁 Young GC 等。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"evidence_type","type":"STRING","required":false,"description":"证据类型——线程栈/堆直方图/GC 日志/类实例计数"},
    {"name":"confidence","type":"ENUM","required":false,"description":"置信度","enum_values":["HIGH","MEDIUM","LOW"]},
    {"name":"content_summary","type":"STRING","required":false,"description":"证据内容摘要"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 3.6 Location（定位）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('java-diag:1.0.0.diag.Location', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'Location', '由证据收敛出的问题位置——具体组件、代码类/方法、线程、配置项等。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"scope","type":"STRING","required":false,"description":"定位范围/组件"},
    {"name":"code_location","type":"STRING","required":false,"description":"代码位置——类/方法/行号"},
    {"name":"certainty","type":"ENUM","required":false,"description":"确定性","enum_values":["CONFIRMED","SUSPECTED"]}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 3.7 RootCause（根因）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('java-diag:1.0.0.diag.RootCause', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'RootCause', '定位后的根本原因——死锁、内存泄漏源、缓存无上限、线程池饱和、类加载器未回收等。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"root_cause_type","type":"STRING","required":false,"description":"根因类型——死锁/内存泄漏/缓存无上限/线程池饱和"},
    {"name":"confirmed","type":"BOOLEAN","required":false,"description":"是否已确认","default_value":"false"},
    {"name":"rationale","type":"STRING","required":false,"description":"判定依据——为何定位到该根因"}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 3.8 Optimization（优化）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('java-diag:1.0.0.diag.Optimization', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'Optimization', '针对根因的优化措施——修复代码、调整 JVM 参数、加缓存上限、扩容线程池等。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"measure","type":"STRING","required":false,"description":"优化措施"},
    {"name":"expected_effect","type":"STRING","required":false,"description":"预期效果"},
    {"name":"risk_level","type":"ENUM","required":false,"description":"优化风险","enum_values":["HIGH","MEDIUM","LOW"]}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- 3.9 Verification（验证）
INSERT INTO metamodel_governance.entity_schema (fqn, package_fqn, bundle_version_fqn, name, description, native_attributes)
VALUES ('java-diag:1.0.0.diag.Verification', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'Verification', '对优化效果的验证——压测、灰度观察、监控对比，确认指标回到正常范围。名称/描述由实例保留字段 name、description 承载。',
  '[
    {"name":"method","type":"STRING","required":false,"description":"验证方式——压测/灰度/监控观察"},
    {"name":"pass_criteria","type":"STRING","required":false,"description":"通过标准"},
    {"name":"result","type":"ENUM","required":false,"description":"验证结果","enum_values":["PASSED","FAILED","PENDING"]}
  ]')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 4. RelationSchema（8 条诊断链路边，PROCESS_SEQUENCE）
-- ============================================================

-- 4.1 现象 → 指标
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('java-diag:1.0.0.diag.SymptomManifestsAsMetric', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'SymptomManifestsAsMetric', '现象表现为指标——现象通过若干量化指标表征（如 CPU 高 → CPU 使用率）',
  'java-diag:1.0.0.diag.Symptom', 'java-diag:1.0.0.diag.Metric',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 4.2 指标 → 分类
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('java-diag:1.0.0.diag.MetricClassifiedBy', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'MetricClassifiedBy', '指标被分类——指标观测值驱动分类判别（如 GC 频率高 → 分类为内存泄漏/大对象）',
  'java-diag:1.0.0.diag.Metric', 'java-diag:1.0.0.diag.Classification',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 4.3 分类 → 采样
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('java-diag:1.0.0.diag.ClassificationRequiresSampling', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'ClassificationRequiresSampling', '分类决定采样——分类假设决定需要何种采样动作验证（如怀疑内存泄漏 → 堆转储）',
  'java-diag:1.0.0.diag.Classification', 'java-diag:1.0.0.diag.Sampling',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 4.4 采样 → 证据
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('java-diag:1.0.0.diag.SamplingProducesEvidence', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'SamplingProducesEvidence', '采样产出证据——采样动作产出可解读的证据（如堆转储 → 大对象占据堆 80%）',
  'java-diag:1.0.0.diag.Sampling', 'java-diag:1.0.0.diag.Evidence',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 4.5 证据 → 定位
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('java-diag:1.0.0.diag.EvidenceLocates', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'EvidenceLocates', '证据指向定位——证据收敛出问题位置（如大对象是 X 类 → 定位到 X 缓存）',
  'java-diag:1.0.0.diag.Evidence', 'java-diag:1.0.0.diag.Location',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 4.6 定位 → 根因
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('java-diag:1.0.0.diag.LocationRevealsRootCause', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'LocationRevealsRootCause', '定位揭示根因——由位置进一步确认根本原因（如 X 缓存无上限 → 根因：缓存泄漏）',
  'java-diag:1.0.0.diag.Location', 'java-diag:1.0.0.diag.RootCause',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 4.7 根因 → 优化
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('java-diag:1.0.0.diag.RootCauseSuggestsOptimization', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'RootCauseSuggestsOptimization', '根因建议优化——根因决定优化措施（如缓存泄漏 → 加缓存上限 + LRU）',
  'java-diag:1.0.0.diag.RootCause', 'java-diag:1.0.0.diag.Optimization',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- 4.8 优化 → 验证
INSERT INTO metamodel_governance.relation_schema (fqn, package_fqn, bundle_version_fqn, name, description, source_fqn, target_fqn, association_type, cardinality_source, cardinality_target)
VALUES ('java-diag:1.0.0.diag.OptimizationValidatedBy', 'java-diag:1.0.0.diag', 'java-diag:1.0.0',
  'OptimizationValidatedBy', '优化被验证——优化效果通过验证确认（如重新压测 → 内存稳定）',
  'java-diag:1.0.0.diag.Optimization', 'java-diag:1.0.0.diag.Verification',
  'PROCESS_SEQUENCE', '0..*', '1')
ON CONFLICT (fqn) DO NOTHING;

-- ============================================================
-- 5. Bundle 依赖：java-diag:1.0.0 → metaforge:1.0.0（复用 agent/common/protocol 元模型）
-- ============================================================
INSERT INTO metamodel_governance.bundle_dependency (source_version_fqn, target_version_fqn)
VALUES ('java-diag:1.0.0', 'metaforge:1.0.0')
ON CONFLICT (source_version_fqn, target_version_fqn) DO NOTHING;

-- ============================================================
-- 6. 导出清单：导出 diag 包
-- ============================================================
INSERT INTO metamodel_governance.export_manifest (bundle_version_fqn, exported_package_fqns)
VALUES ('java-diag:1.0.0', '["java-diag:1.0.0.diag"]')
ON CONFLICT (bundle_version_fqn) DO NOTHING;
