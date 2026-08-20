# DeepSeek Harness 插件（metaforge-dsh）

MetaForge 的 **DeepSeek Harness (dsh) 插件** —— 为 dsh Agent 提供"语义说明书"查询能力。源码位于 `integrations/dsh/`。

让 dsh Agent 从"凭训练先验猜测业务"变为"查询 MetaForge 语义说明书执行"：MetaForge 存"数据的数据"（规则阈值、业务对象结构、能力边界），Agent 通过本插件按需获取。

> 接入关系：MetaForge 认知服务是独立 REST 接口，dsh 插件是消费端适配（服务端零改动）。另一消费端为 opencode（见 [架构 - 消费端](./../architecture.md)）。

## 提供的能力

| 工具 | 作用 |
|------|------|
| `metaforge_cognition` | 按认知模板（DISCOVER/ORIENT/BRIEF/GUIDE/FORECAST/DELEGATE）查询语义说明书：流程/规则/能力/决策/变更影响 |
| `metaforge_resolve` | 自然语言 → 精确实体 FQN（域树枚举 + 确定性匹配，绝不臆造） |

## 开发 / 编译 / 使用

### 1. 开发（改代码）

```sh
cd /绝对路径/metaforge/integrations/dsh
# 编辑 src/index.ts（metaforge_cognition / metaforge_resolve 两个工具 + Config）
```

主要文件：
- `src/index.ts`：两个工具 + 插件配置（Config）
- `cordis.patch.yml`：dsh bundle 层（插件注册 + 默认配置）
- `package.json`：`dsh.bundle` 声明 + peer 依赖

### 2. 编译

```sh
# 首次：准备开发环境（从 dsh 全局安装链接 @deepseek-ai/* 共享运行时 + 装 typescript）
bash setup.sh

npm run typecheck   # 类型检查
npm run build       # 编译 → lib/index.js + lib/types
```

> 依赖 `@deepseek-ai/dsh-tools`、`@deepseek-ai/schemastery`、`@deepseek-ai/cordis` 为 peer（由 dsh 运行时提供）；这些包在开发者预览期未全部发布 npm，故开发构建需从 dsh 安装目录链接（`setup.sh` 自动定位 `npm root -g`）。

### 3. 安装到 dsh 并运行

```sh
# 安装插件到 headless profile（本地路径）
dsh plugin --profile headless add /绝对路径/metaforge/integrations/dsh

# 验证 bundle 层生效
dsh --profile headless --dump-config | grep -A5 metaforge

# 运行真实任务（需 DEEPSEEK_API_KEY + MetaForge 服务在 localhost:8080）
dsh --profile headless "用 metaforge_resolve 解析「库存盘点」，再用 metaforge_cognition（BRIEF）查该任务说明书，报告流程与规则"
```

### 常见问题

| 现象 | 处理 |
|------|------|
| `npm run build` 找不到 @deepseek-ai 类型 | 先 `bash setup.sh`（链接全局 dsh 包） |
| `dsh plugin add` 后 `--dump-config` 无 metaforge 层 | 确认 `package.json` 有 `dsh.bundle.patch` 指向 `cordis.patch.yml` |
| 改代码后不生效 | 重跑 `npm run build`，重启 dsh（bundle 内容变化需重启，host 无热更新） |
| 任务报错/无输出 | 确认 MetaForge 服务在 `localhost:8080`（`curl /actuator/health`）+ `DEEPSEEK_API_KEY` 已设 |

### 验证结果（实测）

`dsh --profile headless` 跑通完整链路：

- `metaforge_resolve`「库存盘点」→ `metaforge:1.0.0.agent.Task_InventoryCheck`
- `metaforge_cognition`(BRIEF) → 完整语义说明书（4 步流程：检查库存→核验库存充足性→触发补货→支付校验子任务；能力 `Cap_InventoryAPI`；入边/出边关系）

## 安装

前置：已安装 DeepSeek Harness（`dsh`）。

```sh
# 从本地路径安装到 headless profile
dsh plugin --profile headless add /绝对路径/metaforge/integrations/dsh

# 或发布到 Git 后安装
# dsh plugin --profile headless add git+https://github.com/<owner>/metaforge-dsh
```

安装后重启 profile。插件通过 `cordis.patch.yml` 声明，默认服务地址 `http://localhost:8080`。

## 配置

在 profile 的 `cordis.patch.yml` 或 `$DSH_HOME/cordis.patch.yml` 覆盖：

```yaml
- id: metaforge
  config:
    serverUrl: http://localhost:8080   # MetaForge 认知服务地址
    timeoutMs: 15000
    defaultBundles: [metaforge:1.0.0]  # 默认认知边界
    defaultDepth: L3
    defaultArchetype: EXECUTION
```

## 使用示例

```
dsh --profile headless "用 metaforge_resolve 解析「库存盘点」，再用 metaforge_cognition（BRIEF）查该任务说明书，报告流程与规则"
```

## 许可证

Apache License 2.0
