# Research & Decisions: 元认知指导层

**Feature**: 001-agent-cognition-guidance
**Date**: 2026-08-01

## 1. 模板驱动架构 vs 多端点独立实现

**Decision**: 统一模板驱动架构（单一入口 `/{templateId}`）

**Rationale**: 
- 5个端点（bundleCatalog/cognitionGuidance/taskBrief/stepGuide/navigate）收敛为单一入口
- 新增场景仅需 YAML 配置，零代码变更
- REST/MCP/Application Service 共享同一模板路由语义

**Alternatives considered**:
- 多端点独立 Controller：每个端点独立实现，新增端点需改代码+测试+文档，扩展成本高

## 2. DDD 菱形架构分层

**Decision**: interfaces → application → domain ← infrastructure

**Rationale**:
- 领域层隔离：domain 不依赖 Spring MVC、JPA、WebFlux 等框架
- 端口-适配器：上游 BC 通过 domain/port 抽象，infrastructure/adapter 实现
- REST/MCP 统一在 interfaces 层

## 3. 视角执行器扩展机制

**Decision**: domain/perspective/PerspectiveExecutor 接口 + application/executor/impl/*Executor 实现 + PerspectiveRegistry 自动装配

**Rationale**:
- 14 个视角解耦：每个 Executor 约 150-300 行独立实现
- 符合开闭原则：新增视角仅需实现接口 + YAML 注册

## 4. 上游 BC 依赖边界

**Decision**: core 模块仅依赖上游 api 模块，domain 层通过端口接口隔离

**Rationale**:
- 编译期隔离：domain 层不直接依赖上游具体实现
- 契约驱动：上游 api 模块是唯一权威数据源

## 5. 对象转换策略

**Decision**: MapStruct 统一 DTO ↔ 领域对象转换，放置于 infrastructure/mapper/

**Rationale**:
- 标准化：消除手动 getter/setter 样板代码
- 隔离：domain 和 api 模块不引入 MapStruct 依赖

## 6. 配置管理

**Decision**: 配置文件启动时一次性加载，无热加载

**Rationale**: 
- KISS 原则：MVP 阶段无需配置动态更新的运维复杂度
- 安全性：避免运行时配置变更导致不一致

## 7. 缓存策略

**Decision**: 基于 Caffeine 内存缓存，缓存键 = templateId + bundleFqns + entityFqn

**Rationale**:
- 无状态约束：缓存仅加速重复查询，不持有数据主权
- 版本锚驱动失效：data_version_anchors 变更时自动淘汰缓存

## 8. 错误码分配

**Decision**: 异常码段 34000-34099

**Rationale**:
- 30000-49999 为 BC 业务异常码段（foundation-core 规范）
- 上游 BC 已占用：metamodel 30100-30199, metadata 31000-31099, graph 32000-32099, compute 33000-33999
- 本 BC 使用 34000-34099 避免冲突

## 9. REST API 路径约定

**Decision**: REST `/api/v1/cognition/{templateId}`, MCP Tool `cognition_execute`

**Rationale**:
- REST 路径与 BC 配置前缀 `metaforge.agent-cognition` 语义统一
- MCP 工具命名简洁可对 Agent 直接暴露
