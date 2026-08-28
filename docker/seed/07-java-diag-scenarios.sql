-- ============================================================================
-- 07-java-diag-scenarios.sql — Java 运行时诊断域知识实例（6 大场景诊断链）
--
-- 依赖：06-java-diag.sql（元模型：9 阶段 EntitySchema + 8 条链边 RelationSchema）
--
-- 每个场景实例化一条 9 阶段线性诊断链：
--   现象 → 指标 → 分类 → 采样 → 证据 → 定位 → 根因 → 优化 → 验证
--
-- 覆盖场景：
--   S1 CPU 高          S2 Full GC          S3 OOM
--   S4 内存泄漏        S5 线程池耗尽       S6 ClassLoader 泄漏
--
-- 实例 FQN 前缀 java-diag:1.0.0.diag.*，entity_schema_fqn / relation_schema_fqn
-- 指向 java-diag:1.0.0.diag.*（本 bundle 自建元模型）。
-- ============================================================================

-- ============================================================================
-- S1. CPU 高
-- ============================================================================

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Symptom_CPUHigh', 'CPU 使用率持续高', NULL, 'java-diag:1.0.0.diag.Symptom',
  '{"severity":"CRITICAL","service":"order-service","trigger_condition":"CPU 持续>90%"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Metric_CPUUsage', 'CPU 使用率', NULL, 'java-diag:1.0.0.diag.Metric',
  '{"unit":"%","collect_method":"top/jmx","normal_range":"<70%","abnormal_direction":"HIGH"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Classification_CPUCategory', 'CPU 高分类', NULL, 'java-diag:1.0.0.diag.Classification',
  '{"dimension":"线程状态","candidates":["用户态计算","内核态","GC线程","锁竞争"],"criteria":"jstack 线程状态 + 栈顶热点"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Sampling_ThreadDump', '线程转储采样', NULL, 'java-diag:1.0.0.diag.Sampling',
  '{"tool":"jstack","command":"jstack -l <pid>","target":"线程"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Evidence_BusyThreads', '忙等线程证据', NULL, 'java-diag:1.0.0.diag.Evidence',
  '{"evidence_type":"线程栈","confidence":"HIGH","content_summary":"多线程栈顶集中在同一锁方法，大量线程 BLOCKED 等待"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Location_LockPoint', '锁竞争位置', NULL, 'java-diag:1.0.0.diag.Location',
  '{"scope":"订单核心链路","code_location":"OrderService#processLine","certainty":"CONFIRMED"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.RootCause_LockContention', '根因：锁竞争', NULL, 'java-diag:1.0.0.diag.RootCause',
  '{"root_cause_type":"锁竞争/忙等","confirmed":true,"rationale":"同一把锁被高频争用，等待线程堆积，CPU 大量消耗在锁调度"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Optimization_ReduceLockScope', '优化：锁粒度细化', NULL, 'java-diag:1.0.0.diag.Optimization',
  '{"measure":"锁粒度细化 + 移除忙等自旋","expected_effect":"CPU 回落至 <40%","risk_level":"MEDIUM"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Verification_CPUStable', '验证：CPU 回落', NULL, 'java-diag:1.0.0.diag.Verification',
  '{"method":"压测+监控","pass_criteria":"CPU<70% 持续 30min","result":"PENDING"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- S1 链边
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_CPUHigh_Manifests', 'CPU高-表现为-CPU使用率', 'java-diag:1.0.0.diag.Symptom_CPUHigh', 'java-diag:1.0.0.diag.Metric_CPUUsage', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SymptomManifestsAsMetric', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_CPUHigh_Classified', 'CPU使用率-被分类', 'java-diag:1.0.0.diag.Metric_CPUUsage', 'java-diag:1.0.0.diag.Classification_CPUCategory', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.MetricClassifiedBy', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_CPUHigh_RequiresSampling', '分类-需要-线程转储', 'java-diag:1.0.0.diag.Classification_CPUCategory', 'java-diag:1.0.0.diag.Sampling_ThreadDump', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.ClassificationRequiresSampling', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_CPUHigh_ProducesEvidence', '线程转储-产出-忙等证据', 'java-diag:1.0.0.diag.Sampling_ThreadDump', 'java-diag:1.0.0.diag.Evidence_BusyThreads', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SamplingProducesEvidence', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_CPUHigh_Locates', '忙等证据-定位-锁点', 'java-diag:1.0.0.diag.Evidence_BusyThreads', 'java-diag:1.0.0.diag.Location_LockPoint', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.EvidenceLocates', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_CPUHigh_RevealsRootCause', '锁点-揭示-锁竞争根因', 'java-diag:1.0.0.diag.Location_LockPoint', 'java-diag:1.0.0.diag.RootCause_LockContention', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.LocationRevealsRootCause', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_CPUHigh_SuggestsOptimization', '锁竞争-建议-锁粒度细化', 'java-diag:1.0.0.diag.RootCause_LockContention', 'java-diag:1.0.0.diag.Optimization_ReduceLockScope', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.RootCauseSuggestsOptimization', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_CPUHigh_ValidatedBy', '锁粒度优化-由-验证确认', 'java-diag:1.0.0.diag.Optimization_ReduceLockScope', 'java-diag:1.0.0.diag.Verification_CPUStable', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.OptimizationValidatedBy', '{}') ON CONFLICT (fqn) DO NOTHING;

-- ============================================================================
-- S2. Full GC
-- ============================================================================

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Symptom_FullGC', '频繁 Full GC', NULL, 'java-diag:1.0.0.diag.Symptom',
  '{"severity":"HIGH","service":"gateway-service","trigger_condition":"Full GC 次数/耗时飙升"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Metric_FullGCCount', 'Full GC 频率', NULL, 'java-diag:1.0.0.diag.Metric',
  '{"unit":"次/h","collect_method":"jstat","normal_range":"<5 次/h","abnormal_direction":"HIGH"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Classification_FullGCCause', 'Full GC 原因分类', NULL, 'java-diag:1.0.0.diag.Classification',
  '{"dimension":"GC 原因","candidates":["内存泄漏","大对象","堆配置过小","元空间不足"],"criteria":"堆使用趋势 + GC 日志"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Sampling_HeapDump', '堆转储采样', NULL, 'java-diag:1.0.0.diag.Sampling',
  '{"tool":"jmap","command":"jmap -dump:live,format=b,file=heap.hprof <pid>","target":"堆"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Evidence_LargeObjects', '大对象证据', NULL, 'java-diag:1.0.0.diag.Evidence',
  '{"evidence_type":"堆直方图","confidence":"HIGH","content_summary":"某缓存对象实例占据堆 70%"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Location_CacheField', '缓存字段位置', NULL, 'java-diag:1.0.0.diag.Location',
  '{"scope":"商品服务","code_location":"ProductCache#instance","certainty":"CONFIRMED"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.RootCause_UnboundedCache', '根因：缓存无上限', NULL, 'java-diag:1.0.0.diag.RootCause',
  '{"root_cause_type":"缓存无上限","confirmed":true,"rationale":"静态 HashMap 无淘汰机制，key 持续增长导致对象无法回收"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Optimization_CacheLimit', '优化：缓存限容', NULL, 'java-diag:1.0.0.diag.Optimization',
  '{"measure":"加容量上限 + LRU 淘汰","expected_effect":"Full GC 回落到 <5 次/h","risk_level":"LOW"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Verification_GCStable', '验证：GC 频率回落', NULL, 'java-diag:1.0.0.diag.Verification',
  '{"method":"监控观察","pass_criteria":"Full GC<5 次/h 持续 48h","result":"PENDING"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- S2 链边
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_FullGC_Manifests', 'FullGC-表现为-FGC频率', 'java-diag:1.0.0.diag.Symptom_FullGC', 'java-diag:1.0.0.diag.Metric_FullGCCount', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SymptomManifestsAsMetric', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_FullGC_Classified', 'FGC频率-被分类', 'java-diag:1.0.0.diag.Metric_FullGCCount', 'java-diag:1.0.0.diag.Classification_FullGCCause', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.MetricClassifiedBy', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_FullGC_RequiresSampling', '分类-需要-堆转储', 'java-diag:1.0.0.diag.Classification_FullGCCause', 'java-diag:1.0.0.diag.Sampling_HeapDump', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.ClassificationRequiresSampling', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_FullGC_ProducesEvidence', '堆转储-产出-大对象证据', 'java-diag:1.0.0.diag.Sampling_HeapDump', 'java-diag:1.0.0.diag.Evidence_LargeObjects', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SamplingProducesEvidence', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_FullGC_Locates', '大对象证据-定位-缓存字段', 'java-diag:1.0.0.diag.Evidence_LargeObjects', 'java-diag:1.0.0.diag.Location_CacheField', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.EvidenceLocates', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_FullGC_RevealsRootCause', '缓存字段-揭示-缓存无上限根因', 'java-diag:1.0.0.diag.Location_CacheField', 'java-diag:1.0.0.diag.RootCause_UnboundedCache', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.LocationRevealsRootCause', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_FullGC_SuggestsOptimization', '缓存无上限-建议-限容', 'java-diag:1.0.0.diag.RootCause_UnboundedCache', 'java-diag:1.0.0.diag.Optimization_CacheLimit', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.RootCauseSuggestsOptimization', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_FullGC_ValidatedBy', '缓存限容-由-验证确认', 'java-diag:1.0.0.diag.Optimization_CacheLimit', 'java-diag:1.0.0.diag.Verification_GCStable', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.OptimizationValidatedBy', '{}') ON CONFLICT (fqn) DO NOTHING;

-- ============================================================================
-- S3. OOM
-- ============================================================================

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Symptom_OOM', 'OutOfMemoryError', NULL, 'java-diag:1.0.0.diag.Symptom',
  '{"severity":"CRITICAL","service":"user-service","trigger_condition":"应用抛出 OutOfMemoryError"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Metric_HeapPeak', '堆内存峰值', NULL, 'java-diag:1.0.0.diag.Metric',
  '{"unit":"MB","collect_method":"监控","normal_range":"<Xmx 的 80%","abnormal_direction":"HIGH"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Classification_OOMType', 'OOM 类型分类', NULL, 'java-diag:1.0.0.diag.Classification',
  '{"dimension":"OOM 类型","candidates":["堆OOM","元空间OOM","直接内存OOM","线程栈OOM"],"criteria":"异常堆栈 + 内存区域指标"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Sampling_HeapDumpLive', '存活堆转储', NULL, 'java-diag:1.0.0.diag.Sampling',
  '{"tool":"jmap","command":"jmap -dump:live,format=b,file=oom.hprof <pid>","target":"堆"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Evidence_InstanceHistogram', '实例直方图证据', NULL, 'java-diag:1.0.0.diag.Evidence',
  '{"evidence_type":"实例直方图","confidence":"HIGH","content_summary":"byte[]/String 占堆 90%，持有者集中于消息消费线程"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Location_AllocationSite', '分配点位置', NULL, 'java-diag:1.0.0.diag.Location',
  '{"scope":"消息消费","code_location":"MessageConsumer#onMessage","certainty":"SUSPECTED"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.RootCause_BufferLeak', '根因：缓冲区未释放', NULL, 'java-diag:1.0.0.diag.RootCause',
  '{"root_cause_type":"缓冲区未释放","confirmed":true,"rationale":"每消息新建大 byte[] 未释放且被内存 List 持续持有"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Optimization_ReleaseBuffer', '优化：池化缓冲区', NULL, 'java-diag:1.0.0.diag.Optimization',
  '{"measure":"池化缓冲区 + 控制持有集合大小","expected_effect":"OOM 消除","risk_level":"MEDIUM"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Verification_NoOOM', '验证：无 OOM', NULL, 'java-diag:1.0.0.diag.Verification',
  '{"method":"压测","pass_criteria":"峰值内存<80% Xmx 且无 OOM","result":"PENDING"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- S3 链边
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_OOM_Manifests', 'OOM-表现为-堆峰值', 'java-diag:1.0.0.diag.Symptom_OOM', 'java-diag:1.0.0.diag.Metric_HeapPeak', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SymptomManifestsAsMetric', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_OOM_Classified', '堆峰值-被分类', 'java-diag:1.0.0.diag.Metric_HeapPeak', 'java-diag:1.0.0.diag.Classification_OOMType', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.MetricClassifiedBy', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_OOM_RequiresSampling', 'OOM分类-需要-存活堆转储', 'java-diag:1.0.0.diag.Classification_OOMType', 'java-diag:1.0.0.diag.Sampling_HeapDumpLive', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.ClassificationRequiresSampling', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_OOM_ProducesEvidence', '存活堆转储-产出-直方图证据', 'java-diag:1.0.0.diag.Sampling_HeapDumpLive', 'java-diag:1.0.0.diag.Evidence_InstanceHistogram', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SamplingProducesEvidence', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_OOM_Locates', '直方图证据-定位-分配点', 'java-diag:1.0.0.diag.Evidence_InstanceHistogram', 'java-diag:1.0.0.diag.Location_AllocationSite', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.EvidenceLocates', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_OOM_RevealsRootCause', '分配点-揭示-缓冲区泄漏根因', 'java-diag:1.0.0.diag.Location_AllocationSite', 'java-diag:1.0.0.diag.RootCause_BufferLeak', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.LocationRevealsRootCause', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_OOM_SuggestsOptimization', '缓冲区泄漏-建议-池化', 'java-diag:1.0.0.diag.RootCause_BufferLeak', 'java-diag:1.0.0.diag.Optimization_ReleaseBuffer', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.RootCauseSuggestsOptimization', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_OOM_ValidatedBy', '池化优化-由-验证确认', 'java-diag:1.0.0.diag.Optimization_ReleaseBuffer', 'java-diag:1.0.0.diag.Verification_NoOOM', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.OptimizationValidatedBy', '{}') ON CONFLICT (fqn) DO NOTHING;

-- ============================================================================
-- S4. 内存泄漏
-- ============================================================================

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Symptom_MemoryLeak', '内存持续增长', NULL, 'java-diag:1.0.0.diag.Symptom',
  '{"severity":"HIGH","service":"search-service","trigger_condition":"堆内存持续增长不回落"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Metric_HeapTrend', '堆内存趋势', NULL, 'java-diag:1.0.0.diag.Metric',
  '{"unit":"MB/h","collect_method":"监控","normal_range":"平坦或轻微波动","abnormal_direction":"HIGH"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Classification_LeakType', '泄漏区域分类', NULL, 'java-diag:1.0.0.diag.Classification',
  '{"dimension":"泄漏区域","candidates":["堆泄漏","元空间泄漏","直接内存泄漏"],"criteria":"Full GC 后内存是否回落"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Sampling_HeapDumpLive2', '泄漏堆转储', NULL, 'java-diag:1.0.0.diag.Sampling',
  '{"tool":"jmap","command":"jmap -dump:live,format=b,file=leak.hprof <pid>","target":"堆"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Evidence_RetainedObjects', '保留对象证据', NULL, 'java-diag:1.0.0.diag.Evidence',
  '{"evidence_type":"引用链分析","confidence":"HIGH","content_summary":"ThreadLocal 持有大量过期请求对象"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Location_ThreadLocalRef', 'ThreadLocal 位置', NULL, 'java-diag:1.0.0.diag.Location',
  '{"scope":"网关","code_location":"TraceFilter#doFilter","certainty":"CONFIRMED"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.RootCause_ThreadLocalLeak', '根因：ThreadLocal 未清理', NULL, 'java-diag:1.0.0.diag.RootCause',
  '{"root_cause_type":"ThreadLocal 未清理","confirmed":true,"rationale":"请求级 ThreadLocal 未 remove，线程复用致对象积累"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Optimization_CleanThreadLocal', '优化：清理 ThreadLocal', NULL, 'java-diag:1.0.0.diag.Optimization',
  '{"measure":"finally 中 remove + 弱引用","expected_effect":"内存曲线平坦","risk_level":"LOW"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Verification_MemoryFlat', '验证：内存平坦', NULL, 'java-diag:1.0.0.diag.Verification',
  '{"method":"监控观察","pass_criteria":"堆使用 72h 平坦","result":"PENDING"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- S4 链边
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_MemoryLeak_Manifests', '内存增长-表现为-堆趋势', 'java-diag:1.0.0.diag.Symptom_MemoryLeak', 'java-diag:1.0.0.diag.Metric_HeapTrend', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SymptomManifestsAsMetric', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_MemoryLeak_Classified', '堆趋势-被分类', 'java-diag:1.0.0.diag.Metric_HeapTrend', 'java-diag:1.0.0.diag.Classification_LeakType', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.MetricClassifiedBy', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_MemoryLeak_RequiresSampling', '泄漏分类-需要-泄漏堆转储', 'java-diag:1.0.0.diag.Classification_LeakType', 'java-diag:1.0.0.diag.Sampling_HeapDumpLive2', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.ClassificationRequiresSampling', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_MemoryLeak_ProducesEvidence', '泄漏堆转储-产出-保留对象证据', 'java-diag:1.0.0.diag.Sampling_HeapDumpLive2', 'java-diag:1.0.0.diag.Evidence_RetainedObjects', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SamplingProducesEvidence', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_MemoryLeak_Locates', '保留对象证据-定位-ThreadLocal', 'java-diag:1.0.0.diag.Evidence_RetainedObjects', 'java-diag:1.0.0.diag.Location_ThreadLocalRef', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.EvidenceLocates', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_MemoryLeak_RevealsRootCause', 'ThreadLocal位置-揭示-未清理根因', 'java-diag:1.0.0.diag.Location_ThreadLocalRef', 'java-diag:1.0.0.diag.RootCause_ThreadLocalLeak', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.LocationRevealsRootCause', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_MemoryLeak_SuggestsOptimization', 'ThreadLocal泄漏-建议-清理', 'java-diag:1.0.0.diag.RootCause_ThreadLocalLeak', 'java-diag:1.0.0.diag.Optimization_CleanThreadLocal', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.RootCauseSuggestsOptimization', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_MemoryLeak_ValidatedBy', 'ThreadLocal清理-由-验证确认', 'java-diag:1.0.0.diag.Optimization_CleanThreadLocal', 'java-diag:1.0.0.diag.Verification_MemoryFlat', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.OptimizationValidatedBy', '{}') ON CONFLICT (fqn) DO NOTHING;

-- ============================================================================
-- S5. 线程池耗尽
-- ============================================================================

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Symptom_ThreadPoolExhaust', '线程池任务堆积', NULL, 'java-diag:1.0.0.diag.Symptom',
  '{"severity":"HIGH","service":"pay-service","trigger_condition":"任务排队/拒绝，接口超时"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Metric_QueueDepth', '队列深度', NULL, 'java-diag:1.0.0.diag.Metric',
  '{"unit":"个","collect_method":"JMX","normal_range":"<100","abnormal_direction":"HIGH"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Classification_PoolCause', '线程池阻塞分类', NULL, 'java-diag:1.0.0.diag.Classification',
  '{"dimension":"阻塞原因","candidates":["外部依赖慢","IO阻塞","死锁"],"criteria":"线程栈是否集中于 IO/锁等待"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Sampling_ThreadDumpSerial', '连续线程转储', NULL, 'java-diag:1.0.0.diag.Sampling',
  '{"tool":"jstack","command":"jstack 连续 3 次采集","target":"线程"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Evidence_WorkersBlocked', 'worker 阻塞证据', NULL, 'java-diag:1.0.0.diag.Evidence',
  '{"evidence_type":"线程栈","confidence":"HIGH","content_summary":"所有 worker 阻塞在外部 HTTP 调用且无超时"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Location_BlockingCall', '阻塞调用位置', NULL, 'java-diag:1.0.0.diag.Location',
  '{"scope":"支付回调","code_location":"PaymentClient#notify","certainty":"CONFIRMED"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.RootCause_NoTimeout', '根因：外部依赖无超时', NULL, 'java-diag:1.0.0.diag.RootCause',
  '{"root_cause_type":"外部依赖无超时","confirmed":true,"rationale":"HTTP 调用未设超时，依赖挂起致线程池被占满"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Optimization_AddTimeout', '优化：加超时与熔断', NULL, 'java-diag:1.0.0.diag.Optimization',
  '{"measure":"设连接/读超时 + 熔断","expected_effect":"队列恢复 <100","risk_level":"LOW"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Verification_PoolRecover', '验证：队列恢复', NULL, 'java-diag:1.0.0.diag.Verification',
  '{"method":"压测+依赖故障注入","pass_criteria":"故障时排队<100 不拒绝","result":"PENDING"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- S5 链边
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_PoolExhaust_Manifests', '线程池堆积-表现为-队列深度', 'java-diag:1.0.0.diag.Symptom_ThreadPoolExhaust', 'java-diag:1.0.0.diag.Metric_QueueDepth', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SymptomManifestsAsMetric', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_PoolExhaust_Classified', '队列深度-被分类', 'java-diag:1.0.0.diag.Metric_QueueDepth', 'java-diag:1.0.0.diag.Classification_PoolCause', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.MetricClassifiedBy', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_PoolExhaust_RequiresSampling', '阻塞分类-需要-连续线程转储', 'java-diag:1.0.0.diag.Classification_PoolCause', 'java-diag:1.0.0.diag.Sampling_ThreadDumpSerial', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.ClassificationRequiresSampling', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_PoolExhaust_ProducesEvidence', '连续转储-产出-worker阻塞证据', 'java-diag:1.0.0.diag.Sampling_ThreadDumpSerial', 'java-diag:1.0.0.diag.Evidence_WorkersBlocked', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SamplingProducesEvidence', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_PoolExhaust_Locates', 'worker阻塞证据-定位-阻塞调用', 'java-diag:1.0.0.diag.Evidence_WorkersBlocked', 'java-diag:1.0.0.diag.Location_BlockingCall', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.EvidenceLocates', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_PoolExhaust_RevealsRootCause', '阻塞调用-揭示-无超时根因', 'java-diag:1.0.0.diag.Location_BlockingCall', 'java-diag:1.0.0.diag.RootCause_NoTimeout', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.LocationRevealsRootCause', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_PoolExhaust_SuggestsOptimization', '无超时-建议-加超时熔断', 'java-diag:1.0.0.diag.RootCause_NoTimeout', 'java-diag:1.0.0.diag.Optimization_AddTimeout', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.RootCauseSuggestsOptimization', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_PoolExhaust_ValidatedBy', '加超时-由-验证确认', 'java-diag:1.0.0.diag.Optimization_AddTimeout', 'java-diag:1.0.0.diag.Verification_PoolRecover', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.OptimizationValidatedBy', '{}') ON CONFLICT (fqn) DO NOTHING;

-- ============================================================================
-- S6. ClassLoader 泄漏
-- ============================================================================

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Symptom_ClassLoaderLeak', '类加载数量持续增长', NULL, 'java-diag:1.0.0.diag.Symptom',
  '{"severity":"MEDIUM","service":"admin-console","trigger_condition":"多次部署后 Metaspace 持续增长"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Metric_ClassCount', '类加载数量', NULL, 'java-diag:1.0.0.diag.Metric',
  '{"unit":"个","collect_method":"JMX","normal_range":"<5 万","abnormal_direction":"HIGH"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Classification_LoaderLeakType', '类加载器泄漏分类', NULL, 'java-diag:1.0.0.diag.Classification',
  '{"dimension":"泄漏位置","candidates":["重部署未卸载","动态生成类","静态引用持有"],"criteria":"ClassLoader 引用链分析"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Sampling_ClassHistogram', '类直方图采样', NULL, 'java-diag:1.0.0.diag.Sampling',
  '{"tool":"jcmd","command":"jcmd <pid> GC.class_histogram","target":"类加载器"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Evidence_RetainedLoader', '类加载器持有证据', NULL, 'java-diag:1.0.0.diag.Evidence',
  '{"evidence_type":"引用链","confidence":"HIGH","content_summary":"上一次部署的 ClassLoader 被全局静态字段持有"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Location_StaticHolder', '静态持有位置', NULL, 'java-diag:1.0.0.diag.Location',
  '{"scope":"插件框架","code_location":"PluginRegistry#handlers","certainty":"CONFIRMED"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.RootCause_StaticRefLoader', '根因：静态引用持有 ClassLoader', NULL, 'java-diag:1.0.0.diag.RootCause',
  '{"root_cause_type":"静态引用持有 ClassLoader","confirmed":true,"rationale":"插件注册表持有 handler 及类加载器，卸载时未清理"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Optimization_ClearLoaderRefs', '优化：清理类加载器引用', NULL, 'java-diag:1.0.0.diag.Optimization',
  '{"measure":"卸载时清理注册表引用 + 工具类下沉共享 ClassLoader","expected_effect":"Metaspace 平稳","risk_level":"MEDIUM"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.diag.Verification_MetaspaceStable', '验证：Metaspace 平稳', NULL, 'java-diag:1.0.0.diag.Verification',
  '{"method":"重部署压测","pass_criteria":"连续 10 次部署 Metaspace 不增长","result":"PENDING"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- S6 链边
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_LoaderLeak_Manifests', '类增长-表现为-类数量', 'java-diag:1.0.0.diag.Symptom_ClassLoaderLeak', 'java-diag:1.0.0.diag.Metric_ClassCount', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SymptomManifestsAsMetric', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_LoaderLeak_Classified', '类数量-被分类', 'java-diag:1.0.0.diag.Metric_ClassCount', 'java-diag:1.0.0.diag.Classification_LoaderLeakType', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.MetricClassifiedBy', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_LoaderLeak_RequiresSampling', '泄漏分类-需要-类直方图', 'java-diag:1.0.0.diag.Classification_LoaderLeakType', 'java-diag:1.0.0.diag.Sampling_ClassHistogram', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.ClassificationRequiresSampling', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_LoaderLeak_ProducesEvidence', '类直方图-产出-持有证据', 'java-diag:1.0.0.diag.Sampling_ClassHistogram', 'java-diag:1.0.0.diag.Evidence_RetainedLoader', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.SamplingProducesEvidence', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_LoaderLeak_Locates', '持有证据-定位-静态持有', 'java-diag:1.0.0.diag.Evidence_RetainedLoader', 'java-diag:1.0.0.diag.Location_StaticHolder', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.EvidenceLocates', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_LoaderLeak_RevealsRootCause', '静态持有-揭示-静态引用根因', 'java-diag:1.0.0.diag.Location_StaticHolder', 'java-diag:1.0.0.diag.RootCause_StaticRefLoader', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.LocationRevealsRootCause', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_LoaderLeak_SuggestsOptimization', '静态引用-建议-清理引用', 'java-diag:1.0.0.diag.RootCause_StaticRefLoader', 'java-diag:1.0.0.diag.Optimization_ClearLoaderRefs', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.RootCauseSuggestsOptimization', '{}') ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.diag.Rel_LoaderLeak_ValidatedBy', '清理引用-由-验证确认', 'java-diag:1.0.0.diag.Optimization_ClearLoaderRefs', 'java-diag:1.0.0.diag.Verification_MetaspaceStable', 'PROCESS_SEQUENCE', 'java-diag:1.0.0.diag.OptimizationValidatedBy', '{}') ON CONFLICT (fqn) DO NOTHING;

-- ============================================================================
-- 实体关系双向索引（compute-engine 图遍历依赖）
--    relation_instance 直接 SQL 插入绕过了应用层激活服务，
--    需手动补插 entity_relation_index（出边 OUTBOUND + 入边 INBOUND）。
-- ============================================================================
INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT source_entity_fqn, 'OUTBOUND', fqn FROM semantic_relation_network.relation_instance WHERE fqn LIKE 'java-diag:%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT target_entity_fqn, 'INBOUND', fqn FROM semantic_relation_network.relation_instance WHERE fqn LIKE 'java-diag:%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;

-- ============================================================================
-- 主题域树（ORIENT 域定位支撑）——复用 metaforge common 的 L1/L2 schema，
-- 实例 FQN 用 java-diag 命名空间，把 6 大现象（Symptom）收进 Java 运行时诊断域。
-- ============================================================================

-- L1 主题域组
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.common.Group_JavaRuntime', 'Java 运行时域组', NULL, 'metaforge:1.0.0.common.SubjectDomainGroup',
  '{"group_level":1,"description":"Java 运行时诊断域分组"}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- L2 主题域
INSERT INTO metadata_management.metadata_entity (fqn, name, parent_fqn, entity_schema_fqn, content, created_by, updated_by)
VALUES ('java-diag:1.0.0.common.Domain_JavaDiagnosis', 'Java 运行时诊断域', 'java-diag:1.0.0.common.Group_JavaRuntime', 'metaforge:1.0.0.common.SubjectDomain',
  '{"keywords":["CPU","GC","OOM","内存泄漏","线程池","ClassLoader"]}', 'seed', 'seed')
ON CONFLICT (fqn) DO NOTHING;

-- L2 → L1 归属
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.common.Rel_GroupToDomain', 'Java运行时域组-包含-诊断域', 'java-diag:1.0.0.common.Group_JavaRuntime', 'java-diag:1.0.0.common.Domain_JavaDiagnosis', 'COMPOSITION', 'metaforge:1.0.0.common.SubjectDomainGroupContainsSubjectDomain', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 域成员：6 大现象（Symptom）挂到诊断域（COMPOSITION，域→现象）
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.common.Rel_Domain_CPUHigh', '诊断域-包含-CPU高现象', 'java-diag:1.0.0.common.Domain_JavaDiagnosis', 'java-diag:1.0.0.diag.Symptom_CPUHigh', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.common.Rel_Domain_FullGC', '诊断域-包含-FullGC现象', 'java-diag:1.0.0.common.Domain_JavaDiagnosis', 'java-diag:1.0.0.diag.Symptom_FullGC', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.common.Rel_Domain_OOM', '诊断域-包含-OOM现象', 'java-diag:1.0.0.common.Domain_JavaDiagnosis', 'java-diag:1.0.0.diag.Symptom_OOM', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.common.Rel_Domain_MemoryLeak', '诊断域-包含-内存泄漏现象', 'java-diag:1.0.0.common.Domain_JavaDiagnosis', 'java-diag:1.0.0.diag.Symptom_MemoryLeak', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.common.Rel_Domain_PoolExhaust', '诊断域-包含-线程池耗尽现象', 'java-diag:1.0.0.common.Domain_JavaDiagnosis', 'java-diag:1.0.0.diag.Symptom_ThreadPoolExhaust', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;
INSERT INTO semantic_relation_network.relation_instance (fqn, name, source_entity_fqn, target_entity_fqn, relation_type, relation_schema_fqn, content)
VALUES ('java-diag:1.0.0.common.Rel_Domain_LoaderLeak', '诊断域-包含-ClassLoader泄漏现象', 'java-diag:1.0.0.common.Domain_JavaDiagnosis', 'java-diag:1.0.0.diag.Symptom_ClassLoaderLeak', 'COMPOSITION', 'metaforge:1.0.0.agent.SubjectDomainComposesTask', '{}')
ON CONFLICT (fqn) DO NOTHING;

-- 域树关系索引同步
INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT source_entity_fqn, 'OUTBOUND', fqn FROM semantic_relation_network.relation_instance WHERE fqn LIKE 'java-diag:1.0.0.common.Rel_%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;
INSERT INTO semantic_relation_network.entity_relation_index (entity_fqn, direction, relation_fqn)
SELECT target_entity_fqn, 'INBOUND', fqn FROM semantic_relation_network.relation_instance WHERE fqn LIKE 'java-diag:1.0.0.common.Rel_%'
ON CONFLICT (entity_fqn, direction, relation_fqn) DO NOTHING;
