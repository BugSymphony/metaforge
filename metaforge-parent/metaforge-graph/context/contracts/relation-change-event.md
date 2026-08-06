---
id: semantic-relation-network.relation-change-event
protocol: Domain Event
version: 1.0.0
owner: semantic-relation-network
description: 关系实例正式态变更（生效/下线/重新生效）时对外发布的关系变更事件契约，供 semantic-query-engine、agent-consumption 等下游 BC 监听消费以同步语义拓扑数据。
type: business
---

# Domain Event Contract: semantic-relation-network

> **自动生成草稿，需人工复核**（Auto-generated draft, manual verification required）

**Protocol**: Domain Event（Spring ApplicationEvent，接口抽象层预留 MQ 替换能力）
**Producer**: `semantic-relation-network`（metaforge-graph）
**Consumer**: `semantic-query-engine`、`agent-consumption`
**事件定义模块**: `metaforge-graph-api`（event 包）
**Version**: 1.0.0

---

## Event Overview

本契约定义关系实例正式态发生变更时对外发布的关系变更事件。事件在原子事务成功提交后发布，事务回滚时不发布任何事件（杜绝脏通知）。下游 BC 通过实现 `RelationChangeListener` 接口或 Spring `@EventListener` 注解消费。

---

## Trigger Scenarios

| 场景 | 变更类型 | 说明 |
|------|----------|------|
| 草稿生效 | `ACTIVATED` | DRAFT → ACTIVE，草稿通过全量校验并执行生效操作，原子事务成功提交 |
| 生效关系下线 | `DEPRECATED` | ACTIVE → DEPRECATED，下线操作原子事务成功提交 |
| 历史版本重新生效 | `ACTIVATED` | ARCHIVED → ACTIVE，复用生效事件类型，事件版本号为原归档版本号 |
| 自动构建生效 | `ACTIVATED` | 实体生效触发的关系自动生效，事件处理逻辑与手动生效一致，下游无需区分关系来源 |

---

## Event Payload Schema

`RelationChangeEvent`（继承 Spring ApplicationEvent）核心负载字段：

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| `fqn` | String | 是 | 关系 FQN，格式 `{源实体FQN}#{关系类型FQN}#{目标实体FQN}` |
| `changeType` | Enum | 是 | 变更类型：`ACTIVATED` / `DEPRECATED` |
| `currentVersion` | Integer | 是 | 当前版本号（重新生效时为原归档版本号） |
| `relationSchemaFqn` | String | 是 | 关联的 RelationSchema FQN（含版本号） |
| `sourceEntityFqn` | String | 是 | 源实体 FQN |
| `targetEntityFqn` | String | 是 | 目标实体 FQN |
| `occurredAt` | Timestamp | 是 | 事件产生时间戳 |

---

## Delivery Rules

- **发布时机**: 原子事务成功提交后发布（`@TransactionalEventListener(phase = AFTER_COMMIT)` 或事务成功后手动 publish）；事务回滚不得发布任何事件。
- **传播方式**: MVP 阶段使用 Spring ApplicationEvent 同步事件（调用方线程内执行）；事件接口抽象层预留后续替换为消息队列（RabbitMQ/Kafka）的能力。
- **幂等**: 同一关系连续生效（v2 → v3）每次生效独立发布 ACTIVATED 事件，下游通过版本号区分。
- **重复投递**: 消费方需自行做幂等补偿（MVP 无 MQ 持久化能力，不做事件重试）。

---

## Consumption Acknowledgement

- 下游 BC 通过实现 `RelationChangeListener` 接口或 Spring `@EventListener` 注解消费。
- 事件消费方处理失败（如下游 BC 抛异常）不影响已提交的关系变更事务，不进行事件重试，需下游自行做幂等补偿。
- 事件消费目的：更新下游缓存、索引或上下文数据，保障全平台语义一致性收敛。
