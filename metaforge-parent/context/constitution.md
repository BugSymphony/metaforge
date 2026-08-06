<!--
================================================================================
  Sync Impact Report
================================================================================
  BC Constitution Version Change: 0.0.0 (placeholder) → 1.0.0
  Parent Global Constitution Version: 1.0.0
  Ratification Date: 2026-08-01

  Principle Summary:
    ✅ Added: I. 纯技术属性铁则 (MUST)
    ✅ Added: II. 唯一基座原则 (MUST)
    ✅ Added: III. 单向依赖铁则 (MUST)
    ✅ Added: IV. 零侵入接入原则 (MUST)
    ✅ Added: V. 契约与兼容准则 (MUST)
    ✅ Added: VI. SPI扩展治理 (MUST)
    ✅ Added: VII. 强制接入原则 (MUST)
    ✅ Added: VIII. 能力解耦 (SHOULD)
    ✅ Added: IX. 分层依赖约束 (SHOULD)
    ✅ Added: X. 数据边界 (MUST)
    ✅ Added: Custom Section - 依赖治理细则
    ✅ Added: Custom Section - 扩展与接入治理细则

  Override Entries:
    ✅ None — all global SHOULD/MAY principles inherited as-is; no BC-level overrides required.

  Deferred TODOs: None

  Rationale: Initial BC constitution formalization for foundation-core. Codifies
  6 governance dimensions from the MetaForge architecture design: positioning &
  boundaries, dependency governance, non-intrusive access, contract &
  compatibility, extension governance, and access compliance. All principles are
  BC-specific additions that extend (not override) the global constitution.
================================================================================
-->

# foundation-core Bounded Context Constitution
<!-- BC-level governance constitution. Inherits all rules from the global system constitution as read-only baseline. -->
<!-- OVERRIDE RULE: Only SHOULD/MAY level principles from the parent global constitution can be overridden. MUST level principles CANNOT be modified or removed. -->

**Parent Version**: 1.0.0
<!-- Human-readable traceability marker only. No version alignment validation during AI merge. Records the global constitution version referenced when this BC constitution was created/last updated. -->

---

## BC-Specific Principles
<!-- Exclusive core principles for this BC only, not inherited from the global constitution. Follow MUST/SHOULD/MAY level specification. Principle names must not duplicate global constitution principles. -->

### I. 纯技术属性铁则 (MUST)

foundation-core 仅承载通用技术能力，严禁包含任何业务领域逻辑、业务实体、
业务规则。基座代码不得引用任何业务 BC 的包或类，不得对业务领域语义有任何
隐式或显式假设。基座提供的工具、配置、扩展点必须保持领域无关性，
确保跨业务领域的通用适配能力。

### II. 唯一基座原则 (MUST)

全局仅存在 foundation-core 一个技术基座。业务 BC 禁止在其模块内自建重复的
技术底座，禁止重复定义或封装 foundation-core 已提供的通用能力（如二次封装
已提供的缓存管理器、序列化工具等）。如业务 BC 发现 foundation-core 缺失
某项通用能力，应通过正式的扩展提案流程补充到 foundation-core，
而非在业务 BC 内部自建。

### III. 单向依赖铁则 (MUST)

foundation-core 所有模块严禁反向依赖任何业务 BC。依赖链路严格自上而下：
业务 BC → foundation-core，foundation-core ↛ 任何业务 BC。
Maven 构建系统须通过 `maven-enforcer-plugin` 强制校验反向依赖规则，
CI 流水线对此类违规直接构建失败。

### IV. 零侵入接入原则 (MUST)

业务 BC 接入 foundation-core 仅需两步：(1) 在 pom.xml 中添加 Maven 依赖；
(2) 在 application.yml 中添加必要的最小化配置项。业务 BC 无需继承基座基类、
无需实现基座特定接口、无需修改自身业务代码结构。所有 foundation-core 能力
默认通过 Spring Boot 自动装配（Auto Configuration）生效，支持通过
`metaforge.foundation.<capability>.enabled=false` 配置开关按需启用或禁用
单项能力，无硬编码强制绑定。

### V. 契约与兼容准则 (MUST)

foundation-core 所有对外暴露的工具类、配置项、接口、SPI 扩展点必须形成公开
契约并文档化。契约文档存放于 `context/contracts/` 目录，以 Markdown + OpenAPI
（如适用）格式发布。公开契约的变更必须保持向后兼容（Backward Compatible）；
任何不兼容变更须走 MAJOR 版本升级流程，并在发布说明中明确列出破坏性变更
清单与迁移指南。内部实现细节不得通过契约泄露，所有外部可见行为以契约文档
为唯一权威依据。

### VI. SPI 扩展治理 (MUST)

业务 BC 对 foundation-core 核心流程的定制化需求仅可通过预留的 SPI
（Service Provider Interface）扩展点实现，扩展接口定义于 foundation-core
的 contracts 包中。禁止业务 BC 通过以下方式规避治理：
(1) 反射修改 foundation-core 内部类；(2) 覆盖或替换 foundation-core 的
Spring Bean 定义；(3) 直接操作 foundation-core 管理的资源（连接池、线程池、
缓存等）。业务 BC 的自定义 SPI 扩展实现仅在声明该扩展的 BC 模块内生效，
不得泄露到其他 BC 或影响 foundation-core 的全局行为。

### VII. 强制接入原则 (MUST)

所有业务 BC 必须基于 foundation-core 构建，禁止任何业务 BC 脱离 foundation-core
独立运行。每个业务 BC 的 pom.xml 必须显式声明对 foundation-core 的依赖关系。
CI 流水线校验：若业务 BC 未声明 foundation-core 依赖或其依赖版本与父 POM
管理的版本不一致，构建直接失败。

### VIII. 能力解耦 (SHOULD)

foundation-core 内各通用能力模块（缓存、序列化、CTE 查询模板、异常处理、
虚拟线程配置等）应保持相互独立。单项能力的启用、禁用、配置变更不应影响
其他核心能力的正常运行。能力间允许存在"可选增强"依赖（如 CTE 查询模板
可选利用缓存模块加速结果），但必须具备独立降级运行能力。

### IX. 分层依赖约束 (SHOULD)

foundation-core 内部模块遵循三层分层架构：
- **common 层**：纯 Java 工具类集合，严禁引入 Spring Framework、Servlet API、
  Jackson 等任何第三方框架依赖。保持 JDK 标准库级别的框架无关性，
  确保在非 Spring 场景（如命令行工具、单元测试独立运行）下的可复用性。
- **config 层**：Spring Boot Auto Configuration 实现，负责将 common 层的
  纯 Java 工具以 Spring Bean 形式自动装配到应用上下文。此层允许引入
  Spring Boot、Spring Framework 依赖。
- **汇总层（root POM）**：统一汇总 common 与 config 层，对外发布单一
  Maven 坐标。业务 BC 仅依赖汇总层即可获得完整基座能力。

### X. 数据边界 (MUST)

foundation-core 不持有任何业务数据，仅提供通用数据访问能力
（如 JSONB 序列化器、递归 CTE 查询模板）。基座无权访问、读取、
修改属于业务 BC 专属 Schema 的数据表。基座配置数据（如缓存策略、
连接池参数）仅可存储于 foundation-core 自身的配置文件中，不得写入
业务 BC 的数据库 Schema。

---

## 依赖治理细则
<!-- 细化依赖版本仲裁、单向依赖校验、分层约束执行规则 -->

### 版本唯一仲裁

foundation-core 的父 POM（`foundation-core/pom.xml`）是全平台依赖版本的
唯一权威来源。业务 BC 的 pom.xml 严禁通过 `<properties>` 或
`<dependencyManagement>` 私自覆盖以下核心依赖版本：
- Spring Boot BOM 版本
- Spring AI BOM 版本
- PostgreSQL JDBC Driver 版本
- Jackson 版本
- Caffeine 版本
- TestContainers 版本

业务 BC 如需升级某个核心依赖版本，须发起 foundation-core 父 POM 的版本升级
提案，经基座维护者评审通过后统一升级，所有下游 BC 同步受益。

### 单向依赖强制校验

Maven 构建配置须引入 `maven-enforcer-plugin` 并声明自定义规则：
- **反向依赖检测**：扫描 foundation-core 模块的所有编译期与运行期依赖，
  若发现任何 `com.metaforge.*` 业务 BC 坐标，构建立即失败。
- **违规直接依赖检测**：扫描所有业务 BC 模块的依赖树，若发现跳过
  foundation-core 汇总层而直接依赖 common 或 config 子模块，触发警告
  （WARNING 级别），建议统一通过汇总层引入。

### 分层违规边界

foundation-core 的 common 层代码审查（Code Review）必须强制执行：
- 禁止 `import org.springframework.*`
- 禁止 `import jakarta.servlet.*`
- 禁止 `import com.fasterxml.jackson.*`
- 仅允许 `import java.*`、`import javax.*`（JDK 标准库）及 common 层内部引用

CI 流水线通过 ArchUnit 测试自动执行上述约束，违规直接构建失败。

---

## 扩展与接入治理细则
<!-- 细化 SPI 扩展流程、接入合规校验、自动装配管控规则 -->

### SPI 扩展生命周期

1. **提案**：业务 BC 提交 SPI 扩展需求，说明需定制的基座行为、业务场景、预期扩展点
2. **评审**：foundation-core 维护者评审需求，判断是否适合以 SPI 形式开放，
   以及是否可复用已有扩展点
3. **发布**：SPI 接口定义在 foundation-core 的 `contracts/spi/` 包中，
   以独立 Maven 模块发布（如有必要），保持接口与实现的编译期分离
4. **实现**：业务 BC 在自身模块中实现 SPI 接口，通过
   `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
   注册扩展实现
5. **隔离验证**：CI 验证扩展实现不修改 foundation-core 核心流程、
   不影响其他 BC 行为、扩展类加载范围不泄露

### 自动装配管控

foundation-core 提供的所有自动装配类必须满足：
- 使用 `@ConditionalOnProperty` 声明开关，前缀统一为
  `metaforge.foundation.<capability>`
- 默认状态为 `enabled=true`（开箱即用），业务 BC 可显式设为 `false` 禁用
- 自动装配类必须包含 `@AutoConfiguration(before = {...}, after = {...})`
  声明排序约束，避免装配顺序导致的不确定行为
- 禁止在自动装配中使用 `@ConditionalOnMissingBean` 配合业务 BC 可能定义的
  Bean 类型（防止业务 BC 意外覆盖基座核心 Bean）

### 接入合规自动化校验

CI 流水线在每次构建时自动执行以下校验项，任何一项不通过则构建失败：
1. 所有业务 BC 模块的 pom.xml 中是否包含 `com.metaforge:foundation-core`
   依赖声明
2. 业务 BC 依赖的 foundation-core 版本是否与父 POM 管理版本一致
3. 业务 BC 是否在 `application.yml` 中配置了 foundation-core 相关必填项
   （如数据库连接信息，由 foundation-core 提供的 DataSource 自动装配消费）
4. 业务 BC 是否在 `src/main/resources` 下存在与 foundation-core 同名配置
   文件的冲突覆盖（配置文件冲突检测）

---

## BC Overrides
<!-- Selective override of parent global constitution SHOULD/MAY level principles only. -->

无。foundation-core 完全继承全局宪法的所有 SHOULD/MAY 级原则，无需 BC 级覆盖。
全局宪法中面向业务领域的原则（V 纯组合无继承设计、VI 合约化双协议标准接口、
VII Bundle 模块化治理、VIII Agent 友好型输出）虽不直接适用于 foundation-core
的技术基础设施定位，但保留为继承规则不做覆盖——foundation-core 在实现层面
不要求符合这些业务级原则，但其通用能力设计不应阻碍上层业务 BC 遵守这些原则。

---

**BC Constitution Version**: 1.0.0 | **Created**: 2026-08-01 | **Last Amended**: 2026-08-01
<!-- BC constitution has independent semantic versioning, decoupled from global constitution version. -->
