---
id: metadata-management.change-events
protocol: Domain Event
version: 1.0.0
owner: metadata-management
description: 元数据管理 BC 的元数据变更事件契约。元数据生效/下线事务成功提交后发布 MetadataChangeEvent，下游 BC 通过实现 MetadataChangeListener 接口（位于 metaforge-metadata-api 模块）以监听者身份消费事件，驱动语义关系网络等模块同步数据。
type: business
---

# Domain Event Contract: metadata-management

**Protocol**: In-Memory Domain Event（Spring ApplicationEvent，MVP 阶段不引入消息队列）
**Publisher BC**: `metadata-management`
**Consumer BC**: `semantic-relation-network`（响应实体变更进行关系重建）、`semantic-query-engine`（同步元数据内容）
**Consumption API**: `metaforge-metadata-api` 模块 `com.metaforge.metadata.api.event` 包
**Version**: 1.0.0

> **Auto-generated draft, manual verification required.**
> 本契约基于 metaforge-metadata-api 模块 `api/event/` 实际发布的 `MetadataChangeEvent` 与 `MetadataChangeListener` 生成，需人工审核确认后发布。

---

## maven 依赖

```xml
<dependency>
    <groupId>com.metaforge</groupId>
    <artifactId>metaforge-metadata-api</artifactId>
    <version>${revision}</version>
</dependency>
```

---

## Event Overview

元数据管理 BC 在元数据生命周期状态发生变化（生效、下线）时，自动发布 `MetadataChangeEvent`。事件承载元数据 FQN、变更类型、版本号与时间戳，驱动下游语义关系网络与查询引擎及时感知主表变更，确保元数据变更后的全平台语义一致性快速收敛。

本契约面向**消费方（下游 BC）**，核心是 `MetadataChangeListener` 监听器接口——下游 BC 以监听者身份实现该接口即可消费事件，无需感知发布侧内部实现。

事件仅在元数据生效/下线**原子事务成功提交后**发布；事务回滚时不发布任何事件，杜绝脏通知。

---

## Consumer Interface Definition

对外消费契约由 `metaforge-metadata-api` 模块发布（唯一对外依赖入口，禁止依赖 `metaforge-metadata-core`）：

### 监听器接口 `MetadataChangeListener`

```java
package com.metaforge.metadata.api.event;

/**
 * 元数据变更事件监听器接口，供下游 BC 实现。
 */
@FunctionalInterface
public interface MetadataChangeListener {
    void onMetadataChange(MetadataChangeEvent event);
}
```

下游 BC 在自身模块内实现该接口并注册为 Spring Bean，即可收到所有元数据变更事件。

### 事件类型 `ChangeType`

```java
package com.metaforge.metadata.api.enums;

/**
 * 变更事件类型。
 */
public enum ChangeType {

    /** 生效 */
    ACTIVATE,

    /** 下线 */
    DEPRECATE
}
```

---

## Event Payload Schema

`MetadataChangeEvent` 继承 Spring `ApplicationEvent`，构造时自动记录 `eventTime`：

```java
package com.metaforge.metadata.api.event;

import com.metaforge.metadata.api.enums.ChangeType;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 元数据变更事件。在元数据生效或下线时发布，供下游 BC 监听。
 */
public class MetadataChangeEvent extends ApplicationEvent {

    private final String fqn;
    private final ChangeType changeType;
    private final Integer version;
    private final LocalDateTime eventTime;

    public MetadataChangeEvent(Object source, String fqn, ChangeType changeType, Integer version) {
        super(source);
        this.fqn = fqn;
        this.changeType = changeType;
        this.version = version;
        this.eventTime = LocalDateTime.now();
    }

    public String getFqn() { return fqn; }
    public ChangeType getChangeType() { return changeType; }
    public Integer getVersion() { return version; }
    public LocalDateTime getEventTime() { return eventTime; }
}
```

### 事件字段说明

| 字段 | 类型 | 必填 | 描述 |
|------|------|------|------|
| `fqn` | `String` | 是 | 变更元数据的全限定名，如 `SalesOrder_001`（getFqn()） |
| `changeType` | `ChangeType` | 是 | 变更类型：`ACTIVATE`（生效）/ `DEPRECATE`（下线）（getChangeType()） |
| `version` | `Integer` | 是 | 变更时对应的元数据版本号（getVersion()） |
| `eventTime` | `LocalDateTime` | 是 | 事件产生时间，构造时自动记录（getEventTime()） |

---

## Trigger Scenarios

### 场景 1：草稿生效（changeType = ACTIVATE）

- 当草稿执行生效操作且原子事务成功提交后发布。
- 对应 FR-034：元数据生效事务成功提交后自动发布变更事件（操作类型="生效"）。

### 场景 2：生效版本下线（changeType = DEPRECATE）

- 当生效版本执行下线操作且原子事务成功提交后发布。
- 对应 FR-035：元数据下线事务成功提交后自动发布变更事件（操作类型="下线"）。

### 场景 3：事务回滚（不触发）

- 生效/下线事务中途失败回滚，则不发布任何变更事件（对应 FR-017、research.md §6 事务内发布约束）。

---

## Consumption Model

### 方式一（推荐）：实现 `MetadataChangeListener` 注册为 Spring Bean

下游 BC 实现 `MetadataChangeListener` 并声明为 Spring Bean，发布方按 `Set<MetadataChangeListener>` 收集全部监听器并逐个分发：

```java
package com.example.semanticrelation;

import com.metaforge.metadata.api.event.MetadataChangeEvent;
import com.metaforge.metadata.api.event.MetadataChangeListener;
import org.springframework.stereotype.Component;

@Component
public class RelationRebuildListener implements MetadataChangeListener {

    @Override
    public void onMetadataChange(MetadataChangeEvent event) {
        // 消费事件：按 event.getFqn() / getChangeType() / getVersion() 触发关系重建
    }
}
```

### 方式二：Spring `@EventListener` / `@TransactionalEventListener`

发布方同时通过 `ApplicationEventPublisher.publishEvent(event)` 发布，下游也可直接监听 `MetadataChangeEvent` 类型的事件：

```java
@Component
public class RelationRebuildListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMetadataChange(MetadataChangeEvent event) {
        // 消费事件
    }
}
```

两种方式择一即可，均通过 `metaforge-metadata-api` 模块依赖引入（`pom.xml` 声明 `metaforge-metadata-api` 依赖后可直接使用）。

---

## Delivery Rules

- **发布通道**: Spring `ApplicationEventPublisher` + `MetadataChangeListener` 直连分发（进程内内存事件，MVP 阶段不引入 MQ）。
- **发布时机**: 仅在原子事务**成功提交后**触发，事务回滚不发布。
- **同步性**: 同步事件，调用方线程内执行，分发顺序与监听器注册顺序一致。
- **失败隔离**: 事件消费异常不影响已提交事务，不进行重试（MVP 阶段无消息持久化与重试需求）。
- **不保证跨 BC 事务一致性**: 下游消费失败不回滚发布方事务。

---

## Consumption Acknowledgement

- 下游 BC 通过实现 `MetadataChangeListener`（或 Spring Event 监听）消费事件。
- 消费方负责幂等处理（以事件 `fqn` + `version` 为幂等键），避免重复事件导致的关系重建重复执行。
- 事件中的 `fqn` 与 `version` 组合是消费方识别单次变更的唯一依据。
- 监听器注册需显式依赖 `metaforge-metadata-api` 模块，且不得依赖 `metaforge-metadata-core`。
