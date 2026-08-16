<!--
===============================================================================
  Sync Impact Report
===============================================================================
  Version Change: 1.0.0 → 1.1.0 (MINOR)
  Parent Global Constitution: 1.0.0
  Last Amended: 2026-07-25 | Created: 2026-07-20

  Principle Summary:
    ✅ Modified BC-Specific: I. 三层正交架构 — 重构为 REQ §3.1 严格三层，增加 Package 嵌套深度上限 5 层及各层实体定位表格
    ✅ Modified BC-Specific: II. 全局唯一标识 — 完整替换为 REQ §3.2 FQN 文法体系（纯净 FQN、类型前缀、版本省略、派生字段、文件系统映射、不可变性、可见性规则）
    ✅ Modified BC-Specific: III. 版本全生命周期 — 增加 enabled 状态推导、草稿创建限制、并发编辑策略、升级等级匹配校验
    ✅ Modified BC-Specific: IV. 依赖治理 — 增加不同 Bundle 可传递依赖同一第三方 Bundle 的不同版本
    ✅ Modified BC-Specific: V. 导出即边界 — 增加关联Schema 导出自动校验关联两端实体所在包均已导出
    ✅ Modified BC-Specific: VI. 纯组合复用 — 大幅扩展：属性双态体系（草稿态组合式结构 + 发布时平铺合并 JSON Schema）、属性名唯一性双环节校验、json_schema 不可变性
    ✅ Modified BC-Specific: VII. 属性标准对齐 — 改为 JSON Schema Draft 2020-12 规范子集（五类属性类型，不支持 object 嵌套），删除 $vocabulary 扩展表述
    ✅ Modified BC-Specific: VIII. 校验前置 — 增加升级等级匹配校验、属性名冲突双环节校验、预览模式
    ✅ Modified BC-Specific: IX. 预置不可侵犯 — 增加预置 Bundle 包含 agent 和 common 两个包

  Custom Sections:
    ✅ Modified: 版本管理规范 — 增加 enabled 状态推导、升级等级声明与匹配校验、草稿创建限制、并发编辑策略
    ✅ Modified: 校验与发布工作流 — 增加升级等级校验项、json_schema 自动生成校验项、属性名冲突校验项、预览模式
    ✅ Added: 导入导出规范 — 基于 REQ §4.7 (导入解析顺序、幂等规则、格式互逆性、Package 级导出依赖包含、仅草稿态导入)

  BC Override Summary:
    ✅ Kept: Override 1 — V. 纯组合无继承设计 (SHOULD→MUST), unchanged
    ✅ Kept: Override 2 — VII. Bundle 模块化治理 (SHOULD→MUST), unchanged

  Deferred TODOs: None

  Rationale: MINOR upgrade from 1.0.0 to 1.1.0: full rewrite of all 9 BC-specific principles
  to align with REQ document, with two new core constraints (attribute dual-state system, FQN
  detailed grammar) and one new custom section (导入导出规范). Backward compatible — no
  removal of existing capabilities.
===============================================================================
-->

# metamodel-governance Bounded Context Constitution

**Parent Version**: 1.0.0

---

## BC-Specific Principles

### I. 三层正交架构 (MUST)

元模型体系划分为严格三层架构，各层职责明确分离，不可混淆：

| 层级 | 核心定位 | 权责说明 |
|------|----------|----------|
| **治理管控层** | Bundle、BundleVersion、Package、BundleDependency、ExportManifest | 提供版本/依赖/导出治理能力，非语义元素，对元数据消费方完全透明 |
| **核心语义层** | EntitySchema、RelationSchema | 唯一承载语义规则的一等核心元素，发布时属性平铺合并为 JSON Schema |
| **属性定义层** | AttributeTemplate、Native Attribute | 为 EntitySchema 和 RelationSchema 提供属性复用与专属定义 |

#### 各层实体定位

| 实体 | 定位 | FQN 全局唯一 | 治理能力 |
|------|------|-------------|----------|
| Bundle | 顶层治理单元、最小交付单元，版本/依赖/导出的唯一锚点 | ✓ | 版本管理、跨 Bundle 依赖、导出管控 |
| BundleVersion | Bundle 的版本化内容容器 | ✓ | 状态机（草稿/已发布），发布时触发元模型 JSON Schema 生成 |
| Package | Bundle 版本内的纯分类容器，嵌套深度上限 5 层 | ✓ | 无独立治理能力，仅承载元素归属 |
| EntitySchema | 核心语义层一等元素——领域概念建模定义 | ✓ | 属性平铺合并为 JSON Schema |
| RelationSchema | 核心语义层一等元素——实体间关联建模定义 | ✓ | 源/目标端引用、基数约束 |
| AttributeTemplate | 属性定义层辅助复用单元，全局归属 Bundle 版本级 | ✓ | 属性模板定义与挂载复用 |

Package 嵌套深度上限为 5 层（根层 + 4 级子层），超限时写入校验直接拦截。

治理管控层元素对元数据消费方完全透明——消费方仅通过核心语义层 FQN 引用元模型
JSON Schema，无需感知治理层内部结构。

---

### II. 全局唯一标识 (MUST)

全限定名（Fully Qualified Name, FQN）是系统内所有实体唯一的对外标识。
`fqn` 为存储层唯一标识字段，不存在独立 `code` 字段。

#### FQN 文法

```
<bundle-code> ::= [a-z][a-z0-9_-]{2,63}
<version>     ::= \d+\.\d+\.\d+
<segment>     ::= [A-Za-z][A-Za-z0-9_-]*
```

#### 纯净 FQN 格式（存储层）

在 entity_schema 等专属表中，FQN 不含类型前缀，直接采用以下格式：

| 实体 | 纯净 FQN 示例 | 短名（推导） | 父 FQN（推导） |
|------|--------------|-------------|---------------|
| Bundle | `metaforge` | `metaforge` | — |
| BundleVersion | `metaforge:1.0.0` | `1.0.0` | `metaforge` |
| Package | `metaforge:1.0.0.common` | `common` | `metaforge:1.0.0` |
| EntitySchema | `order:1.0.0.pkg_order.Order` | `Order` | `order:1.0.0.pkg_order` |
| RelationSchema | `order:1.0.0.pkg_order.Order_contains_Item` | `Order_contains_Item` | `order:1.0.0.pkg_order` |
| AttributeTemplate | `order:1.0.0.AuditFields` | `AuditFields` | `order:1.0.0` |

**分隔符约定**：
- `:` 分隔 Bundle code 与版本
- `.` 分隔路径 segment
- 版本号 `\d+.\d+.\d+` 与路径 segment 的 `.` 不冲突——前三个数字 segment 为版本号

#### 类型前缀（API 层）

在 `metaforge.get("...")` 等未限定实体类型的通用查询场景，可加类型前缀：

| 类型前缀 FQN | 说明 |
|-------------|------|
| `entity:order:1.0.0.pkg_order.Order` | 明确为 EntitySchema |
| `relation:order:1.0.0.pkg_order.Order_contains_Item` | 明确为 RelationSchema |
| `template:order:1.0.0.AuditFields` | 明确为 AttributeTemplate |
| `package:order:1.0.0.pkg_order` | 明确为 Package |
| `version:order:1.0.0` | 明确为 BundleVersion |
| `bundle:order` | 明确为 Bundle |

类型前缀仅在 API/查询入口层面使用，存储层 fqn 永远是纯净格式。
解析规则：第一个 `:` 前的内容匹配已知类型前缀时剥离，后续部分解析为纯净 FQN。

#### 版本省略规则

Agent 代码中 FQN 可省略版本，按"最新已发布版本"解析：

| 完整写法 | 省略版本写法 | 解析结果 |
|---------|------------|---------|
| `order:1.0.0.pkg_order.Order` | `order.pkg_order.Order` | → 最新已发布版本 |
| `order:1.0.0.AuditFields` | `order.AuditFields` | → 最新已发布版本 |

解析不歧义：版本号必须匹配 `\d+.\d+.\d+`，`pkg_order` 不匹配该模式，
所以 `order.` 后遇非数字 segment 即判定为省略版本。

**元数据实例持久化时 FQN 必须为已解析的完整带版本格式**。

#### 派生字段规则

以下字段不存储，查询/输出时即时从 FQN 推导：
- **短名**：FQN 最后一段（`fqn.rsplit(".", 1)[-1]`）
- **bundle-code**：FQN 中 `:` 前部分（`fqn.split(":")[0]`）
- **version**：`:` 后前三个数字 segment
- **parent-fqn**：FQN 去掉最后一段（`fqn.rsplit(".", 1)[0]`）
- **文件系统映射**：`fqn.replace(":", "/").replace(".", "/") + ".json"`

#### FQN 不可变性

已发布元素的 FQN 不可修改。语义迁移需通过新建版本完成。

#### 跨版本独立性

不同 Bundle 版本的 FQN 同名元素为完全独立定义，语义无关联，
禁止隐式兼容。各版本元素拥有各自独立的生命期与 json_schema。

#### 全局可见性规则

1. **同 Bundle 跨包可见性**：同一 Bundle 版本内所有 Package 的元素默认天然互相可见，
   FQN 引用仅校验目标存在性。
2. **跨 Bundle 可见性**：仅能访问目标 Bundle 已导出清单中的 Package 内元素，
   必须显式声明依赖后方可引用。

---

### III. 版本全生命周期 (MUST)

版本治理全量收敛至 Bundle 层，Package 与元素无独立版本号。

#### 两态生命周期

Bundle 版本严格遵循 **草稿 → 已发布** 两态生命周期，正向流转不可逆：
- **草稿态**：全字段可编辑，支持增删改 Package 和元素
- **已发布态**：全字段只读冻结，禁止任何热修改

已发布版本不可回退为草稿态，亦不可直接删除。

#### enabled 状态推导

元素 enabled 状态从 BundleVersion.status 推导，不独立存储元素级 enabled 字段：
- 草稿态下所有元素 `enabled=false`，仅内部可见，不可被下游消费
- 已发布版本内所有元素 `enabled=true`
- 已发布版本的 `enabled=true` 状态不可修改

#### 草稿创建限制

仅允许从**最新已发布版本**创建新草稿，禁止从历史非最新版本创建草稿。
新草稿创建为原子操作，全量复制源版本内容（元模型、依赖、导出配置）无遗漏。
同一 Bundle 下同一时间只允许存在一个草稿版本。

#### 并发编辑策略

同一 Bundle 草稿版本采用"最后写入覆盖"策略，不引入锁机制。

#### 升级等级匹配校验

创建草稿时用户声明的升级等级（MAJOR/MINOR/PATCH），
在发布时校验变更内容是否匹配：
- 声明 PATCH 但包含元素删除、元素新增、关系类型变更 → 拦截
- 声明 MINOR 但包含元素删除、关系类型变更 → 拦截
- 声明 MAJOR 无额外限制（允许所有类型变更）
- 校验不匹配时阻止发布，全量回滚

#### 发布原子性

Bundle 发布为原子操作——要么全量成功发布，要么全量回滚至草稿态，
不产生中间态脏数据。

---

### IV. 依赖治理 (MUST)

跨 Bundle 依赖声明必须锚定目标 Bundle 的精确已发布版本号，
禁止使用版本范围表达式或 `latest` 语义。

传递依赖遵循以下硬约束：
- **范围不放大**：传递依赖的可见性严格不超出直接依赖方声明的范围
- **版本不自动升级**：传递依赖版本号锁定为直接依赖方声明时锚定的版本，
  不随目标 Bundle 新版本发布而自动升级
- **独立共存**：不同 Bundle 可传递依赖同一第三方 Bundle 的不同版本，
  彼此独立共存

循环依赖零容忍——元模型加载时检测到任意长度的依赖环立即拦截并报错，
包含环中所有参与方信息。

---

### V. 导出即边界 (MUST)

跨 Bundle 可见性以导出清单（Export Manifest）为唯一边界。

导出清单包含 Bundle 已发布版本对外公开的 Package 命名空间白名单及可选的
元素粒度细化声明。未列入导出清单的 Package 和元素仅 Bundle 内部可见，
不可被任何外部 Bundle 引用。

消费侧按需导入——仅导入声明中列出的 Bundle 版本与 Package 范围对消费方可见。
未导出或未导入的元素完全不可访问，引用即报错。

**关联Schema 导出校验**：导出包含 RelationSchema 的 Package 时，
自动校验其关联两端 EntitySchema 所在 Package 均已导出或可访问，
不满足则拦截。

---

### VI. 纯组合复用 (MUST)

元模型元素间采用纯组合关系（Composition），不支持继承语义。

#### 属性双态体系

草稿态与已发布态采用不同的属性表示方式：

**草稿态**：保留"原生属性定义 + 挂载属性模板组 FQN 列表"组合式结构。
原生属性定义以 JSONB 形式存储，属性模板组以 FQN 列表引用。

**已发布态**：发布时自动完成全量平铺合并——将所有挂载的属性模板组展开，
与原生属性定义合并，生成符合 JSON Schema Draft 2020-12 规范子集的
扁平 JSON Schema，写入对应 EntitySchema 及 RelationSchema 的
`json_schema` 字段永久固化。生成的 Schema 为扁平化结构，无嵌套引用，
可直接用于校验。

#### 属性名唯一性

同一 EntitySchema 或 RelationSchema 内属性名唯一：
- **写入阶段校验**：挂载属性模板组或添加原生属性时，检测到同名属性立即报错拦截
- **发布阶段校验**：发布时再次全量校验属性名唯一性，不通过则全量回滚
- 不做优先级覆盖与自动重命名

#### 不可变性

已发布版本的 `json_schema` 不可修改。不同版本间 FQN 同名元素的
`json_schema` 完全独立，互不影响。

---

### VII. 属性标准对齐 (MUST)

属性约束对齐 JSON Schema Draft 2020-12 规范子集：
- 仅支持 `string` / `number` / `integer` / `boolean` / `array` 五类属性类型
- 不支持 `object` 嵌套
- 不自定义类型体系
- 不引入 `$vocabulary` 扩展机制

支持的约束关键字包括但不限于：`type`、`properties`、`required`、`default`、
`enum`、`pattern`、`minimum`、`maximum`、`minLength`、`maxLength`、
`minItems`、`maxItems`、`uniqueItems` 等符合规范子集的标准关键字。

---

### VIII. 校验前置 (MUST)

校验分为两级，在生命周期不同阶段执行：

#### 写入时轻量校验

Bundle 草稿态阶段，每次保存触发基础结构校验：
1. FQN 全局唯一性检查
2. 引用完整性检查（引用目标是否存在且可达）
3. 循环依赖检测
4. Package 嵌套深度检查（不超过 5 层）
5. 属性名冲突校验（挂载属性模板组或原生属性定义时检测同名）
6. 命名规范合规性（bundle-code 正则匹配等）

任一校验失败阻止保存。

#### 发布前全局校验

Bundle 从草稿态发布为已发布态前，执行全量全局校验：
1. 跨 Bundle 引用可达性
2. 导出清单一致性
3. 关联Schema 端点合法性（两端 EntitySchema 存在且可见）
4. 依赖链自洽性（循环依赖、缺失依赖、版本不存在）
5. 升级等级与变更内容匹配性（声明 PATCH 但含元素删除则拦截）
6. 属性名冲突全量校验（写入后编辑导致的冲突）
7. JSON Schema 自动生成与合规性（属性平铺合并后验证）
8. 预置约束（仅 `metaforge` Bundle）

任一校验失败全量回滚发布操作。

#### 预览模式

支持仅校验不落库的预览操作，错误信息精准定位元素与字段。
预览模式不修改任何数据、不创建快照，仅返回校验结果报告。

不合法变更零容忍——任何阶段的校验失败均阻止操作继续，
不允许跳过或压制校验。

---

### IX. 预置不可侵犯 (MUST)

内置 `metaforge` Bundle 作为平台语义基座，享有最高保护级别：
- 禁止删除 `metaforge` Bundle 及其任何已发布版本
- 已发布的 `metaforge` 核心结构（元素定义、RelationSchema、AttributeTemplate）禁止修改
- 扩展仅允许通过新建自定义 Bundle 并依赖 `metaforge` 的方式实现

`metaforge` Bundle 包含两个预置包：
- `agent` 包：Agent 相关元模型（EntitySchema + RelationSchema + AttributeTemplate）
- `common` 包：通用业务语义层级（主题域分组→主题域→业务对象→逻辑数据实体→属性），
  每个层级作为 EntitySchema 实例，以 RelationSchema 连接层级关系，属性挂载 AttributeTemplate

---

## 版本管理规范

### 语义化版本

Bundle 版本号遵循 SemVer 2.0：
- **MAJOR**：不兼容的元模型变更——元素删除、必填属性新增、关系类型变更、枚举值删除
- **MINOR**：向后兼容的新增——新元素、新可选属性、新关系类型、新枚举值
- **PATCH**：非结构性修正——描述文本更新、示例补充、元数据修正

### enabled 状态推导规则

- 所有元模型元素的 `enabled` 状态从 BundleVersion.status 唯一推导，不独立存储元素级 enabled 字段
- 草稿态：`enabled=false`
- 已发布态：`enabled=true`
- 已发布版本的 `enabled=true` 不可修改

### 升级等级声明与匹配校验

创建草稿时用户选择目标升级等级（MAJOR/MINOR/PATCH，默认 PATCH）。
系统基于源版本号与选定等级自动计算新版本号。

发布时执行等级匹配校验：
- PATCH：仅允许描述修改、示例补充、元数据修正；增删元素或关系类型变更则拦截
- MINOR：允许新增元素、新增可选属性、新增关系类型；删除元素或关系类型变更则拦截
- MAJOR：允许所有类型变更（含元素删除、必填属性新增、关系类型变更）

### 草稿创建限制

- 仅允许从**最新已发布版本**创建新草稿，禁止版本分叉
- 禁止从历史非最新版本创建草稿
- 新草稿创建为原子操作，全量复制源版本内容无遗漏
- 同一 Bundle 下同一时间只允许存在一个草稿版本

### 并发编辑策略

同一 Bundle 草稿版本采用"最后写入覆盖"策略，不引入锁机制。
并发写入时以最后一次成功写入的内容为准。

### 版本冻结

已发布版本创建后即进入全字段只读冻结状态。冻结实现包括：
- 数据库事务级原子提交（确保全量或全不）
- 发布后对应记录标记为只读，ORM 层拦截 UPDATE/DELETE 操作
- 导出清单快照随版本一同冻结，不可独立修改

### 版本废弃

已发布版本允许标记为废弃（Deprecated），但不可删除。废弃版本仍可被已有依赖方读取，
但新建依赖时禁止引用已废弃版本。废弃标记需提供替代版本建议
（可为空，表示无替代方案）。

---

## 校验与发布工作流

### 写入校验清单

每次 Bundle 草稿保存时自动执行：

1. **FQN 唯一性**：全限定名在系统全局范围内无重复
2. **引用完整性**：所有全限定名引用目标存在且可达（含跨 Bundle 引用）
3. **循环依赖**：依赖图中无环（拓扑排序检测）
4. **Package 嵌套深度**：Package 嵌套深度不超过 5 层
5. **属性名冲突**：挂载属性模板组或添加原生属性时检测同名属性，
   多模板组同名或与原生属性同名均拦截
6. **命名规范**：bundle-code 符合 `[a-z][a-z0-9_-]{2,63}` 正则，
   segment 符合 `[A-Za-z][A-Za-z0-9_-]*` 正则

### 发布校验清单

Bundle 发布操作执行以下全量校验，任一失败即回滚：

1. **跨 Bundle 引用可达性**：所有跨 Bundle 全限定名引用目标确认为
   已发布且未废弃版本
2. **导出清单一致性**：导出清单列出的 Package 均存在于当前 Bundle 版本中
3. **导入授权一致性**（如有消费方）：依赖当前 Bundle 的消费方所导入的
   Package 均在导出清单范围内
4. **关联Schema 端点合法性**：所有 RelationSchema 的关联两端 EntitySchema
   存在且可见（含跨 Bundle 可见性校验）
5. **依赖链自洽性**：全量依赖链（含传递依赖）无循环、无缺失、目标版本均存在
6. **升级等级匹配性**：变更内容与声明的 MAJOR/MINOR/PATCH 等级匹配；
   如声明 PATCH 但含元素删除则拦截
7. **属性名冲突全量校验**：发布时对所有 EntitySchema 和 RelationSchema
   全量校验属性名唯一性（写入后可能因间接编辑产生冲突）
8. **JSON Schema 自动生成与合规性**：属性平铺合并生成的最终 JSON Schema
   为合法 JSON Schema Draft 2020-12 规范子集文档，随版本快照固化
   写入对应元模型的 `json_schema` 字段
9. **预置约束**（仅 `metaforge`）：不违反预置不可侵犯规则
10. **版本号合规**：新版本号相对于前一已发布版本符合 SemVer 递增规则

### 预览模式

- 支持仅校验不落库的预览操作
- 预览模式下，系统执行与正式发布完全一致的校验清单（含 JSON Schema 生成校验）
- 错误信息**精准定位**到具体的元素 FQN 与字段名称
- 预览模式不修改任何数据、不创建快照、不生成版本记录
- 预览输出作为校验报告返回给调用方

### 发布回滚

发布校验失败时执行完整回滚：
- 草稿态数据恢复至发布前状态
- 数据库事务级回滚，确保原子性
- 回滚后草稿仍可继续编辑和重试发布

---

## 导入导出规范

### 导入解析顺序

批量导入时严格按照以下顺序解析，确保依赖前置：

1. **Bundle**：创建或匹配目标 Bundle
2. **Package**：创建 Package 层级结构
3. **AttributeTemplate**：创建属性模板组
4. **EntitySchema**：创建实体Schema，挂载属性模板组、写入原生属性定义
5. **RelationSchema**：创建关联Schema，校验源/目标端 FQN 存在性，
   挂载属性模板组、写入原生属性定义

任一阶段的依赖缺失（如引用的 Bundle 不存在、Package 不存在、目标 EntitySchema
不存在）立即终止导入并报错，不产生部分导入的脏数据。

### 幂等规则

- 以 FQN 为基准，重复导入支持「跳过」和「报错」两种策略，由调用方指定
- **禁止覆盖已发布版本**：若导入目标对应的 Bundle 版本已发布，无论选择何种策略
  均直接报错终止
- 仅对草稿版本生效的导入操作执行幂等判断

### 导出格式互逆性

- 导出文件格式与导入格式完全兼容，导出的文件可直接重新导入
- 导出内容使用 YAML 或 JSON 格式，包含完整的元模型定义
- 导出文件中所有引用均以 FQN 为标识

### Package 级导出依赖包含

- Package 级导出时，自动包含元素依赖的属性模板组（即使该属性模板组不在当前导出
  Package 范围内），保证导入后语义完整
- 导出内容仅包含已发布的启用元素（`enabled=true`），停用元素不纳入导出文件

### 导入态约束

- 仅支持生成/更新草稿版本，禁止自动发布
- 导入操作仅写入草稿态数据，不触发发布流程
- 导入完成后需用户手动执行发布操作

---

## BC Overrides

### Override 1: V. 纯组合无继承设计

- **Original Parent Rule** (SHOULD)：元模型元素间采用纯组合关系而非继承，保持结构扁平化
- **Override Content** (MUST)：本 BC 强制纯组合复用为 MUST 级约束——属性模板组以组合方式嵌入，
  实体Schema与属性模板组均不支持继承语义。任何引入继承结构的设计在元模型定义层即被校验拦截
- **Rationale**：metamodel-governance 作为全平台元模型定义的唯一写入入口，必须在源头彻底杜绝
  继承语义。若允许继承结构进入元模型定义，将导致下游所有 BC 的 JSON Schema 编译、图遍历推理、
  关系边构建全部复杂化。从根源强制组合，确保全链路语义简洁性

### Override 2: VII. Bundle 模块化治理

- **Original Parent Rule** (SHOULD)：每个 Bundle 通过导出清单明确对外暴露的命名空间边界，
  未导出内容仅模块内部可见
- **Override Content** (MUST)：本 BC 将导出清单的边界强制执行提升为 MUST 级——任何未列入
  导出清单的 Package 或元素绝对不可被外部 Bundle 引用，校验层在加载阶段即拦截越界引用。
  导出清单随版本冻结，不可独立热修改
- **Rationale**：metamodel-governance 作为导出清单的生产者与唯一授权点，必须在源头确保导出
  边界的不可绕过性。若导出清单仅作为建议性约束（SHOULD），消费方可能绕过边界直接引用未
  导出元素，导致元模型内部结构泄露与版本耦合失控。强制执行导出边界是保证模块化治理闭环的前提

---

**BC Constitution Version**: 1.1.0 | **Created**: 2026-07-20 | **Last Amended**: 2026-07-25
