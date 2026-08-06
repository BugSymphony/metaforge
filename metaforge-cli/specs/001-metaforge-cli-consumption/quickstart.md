# Quickstart: metaforge-cli 元认知指导能力验证指南

**阶段**: Phase 1（Design & Contracts）| **日期**: 2026-08-01
**关联**: [contracts/script-cli.md](contracts/script-cli.md) / [contracts/opencode-delivery.md](contracts/opencode-delivery.md) / [data-model.md](data-model.md)

> 本文是**端到端可运行验证指南**，证明 8 项元认知指导能力按 spec 生效。实现细节见 `tasks.md`（/speckit.tasks 输出）。完整 mock 数据见 `REPO_ROOT/docs/cognition2/mock/order-bundle-m1.json`；既有验证脚本 `REPO_ROOT/test/cognition-agent-test.sh` 可复用其 REST 调用模式。

## 前置条件

1. **服务端可用**：`metaforge-boot` 单进程部署，`POST /api/v1/cognition/{templateId}` 与 `GET /actuator/health` 可访问（默认 `http://localhost:8080`）。若业务 Bundle 上料未就绪（风险 R2），使用 mock 数据验证链路。
2. **环境变量**（可选，均有默认值）：
   - `META_FORGE_SERVER_URL`（默认 `http://localhost:8080`）
   - `META_FORGE_CONNECT_MS`、`META_FORGE_TIMEOUT_MS`（默认 3000 / 10000）
3. **脚本可用**：`.metaforge/scripts/metaforge-pro.sh` 可执行；命令/Skill 文件位于 `.opencode/commands/` 与 `.opencode/skills/`。
4. **零配置兜底**：不配置任何项，默认 base-url 直连本地服务端即可运行（NFR-004/FR-020）。

## 验证场景

### V1 开发态环境（`.metaforge/`）

```bash
# 项目根定位
.metaforge/scripts/metaforge-pro.sh env root
# 预期: 输出仓库根绝对路径

# 环境摘要
.metaforge/scripts/metaforge-pro.sh env summary
# 预期: META_FORGE_ROOT / META_FORGE_SERVER_URL 等 key=value

# 服务端健康检查
.metaforge/scripts/metaforge-pro.sh health
# 预期: HEALTH OK
# 不可达时: HEALTH FAIL + 中文提示（非堆栈）
```

### V2 模板注册表投影（模板驱动，FR-CAP-002）

```bash
.metaforge/scripts/metaforge-pro.sh cognition templates
# 预期: 输出服务端实际注册模板 ID（当前 6 个: bundle-catalog / cognition-guidance /
#       task-brief / step-guide / navigate / sub-task-brief）
# 验证点: 命令清单与服务端注册表对齐，服务端新增模板时无需改代码
```

### V3 平台发现（metaforge.catalog）

```bash
.metaforge/scripts/metaforge-pro.sh cognition execute bundle-catalog --format json
# 预期（对照 spec §4.1 平台发现）:
#   - Bundle 列表（fqn/name/description）
#   - data_version_anchors 版本锚
#   - 主题域概要
# 分页: 加 --page-size 与 --cursor 续翻
```

### V4 领域导航（metaforge.navigate）

```bash
.metaforge/scripts/metaforge-pro.sh cognition execute navigate \
  --bundles order:1.0.0 --subject-domain order:1.0.0.SubjectDomain_Order --format json
# 预期: 层级树 + has_more + entryStepFqn（逐层下钻，L1→L2→Task）
# 必填校验: 缺 --bundles → 用法提示并中止（退出码 2）
```

### V5 任务认知（metaforge.task-brief）

```bash
.metaforge/scripts/metaforge-pro.sh cognition execute task-brief \
  --bundles order:1.0.0 --depth L2 --archetype execution --max-tokens 8000 --format json
# 预期: 10 视角简报（约束/流程/能力/决策矩阵），含版本锚
# 双格式: --format prompt → Markdown 可直接注入 LLM 上下文，与 json 语义一致
# 必填校验: 缺 --bundles → 用法提示并中止
```

### V6 实体即时指导（metaforge.step-guide）

```bash
.metaforge/scripts/metaforge-pro.sh cognition execute step-guide \
  --entity-fqn order:1.0.0.Step_CheckInventory --format json
# 预期: 实体级 6 视角 + 约束级别（MANDATORY/RECOMMENDED/REFERENCE）+
#       能力协议 + 决策分支 + 影响层 + adjacentContext；Bundle 级视角标注跳过
# 必填校验: 缺 --entity-fqn → 用法提示并中止
# 归属失败: 无效 entityFqn → 34004 中文提示 + 候选列表
```

### V7 子任务认知（metaforge.subtask）

```bash
.metaforge/scripts/metaforge-pro.sh cognition execute sub-task-brief \
  --bundles order:1.0.0 --entry-entity order:1.0.0.Task_OrderProcessing --scope-mode INHERITED --format json
# 预期: 三层收窄简报 + narrowedSchemaFqns（INHERITED）
# PURE: --scope-mode PURE → 仅返回 entity_profile
# 必填校验: 缺 --bundles 或 --entry-entity → 用法提示并中止
```

### V8 自由视角组合（metaforge.guidance）

```bash
# 组合 schema_inventory + instance_catalog（承接 bundle-scope 能力，R3）
.metaforge/scripts/metaforge-pro.sh cognition execute cognition-guidance \
  --bundles order:1.0.0 --perspectives schema_inventory,instance_catalog --format json
# 预期: 恰好返回所请求视角章节（Schema 清单 + 实例目录）
# 含 impact_trace 时: 返回 forward/backward impact + impact_paths（影响感知）
```

### V9 FQN 推测（cognition resolve，FR-NL）

```bash
# 唯一命中 → 自动确认
.metaforge/scripts/metaforge-pro.sh cognition resolve "订单处理任务"
# 预期: 输出唯一 FQN（如 order:1.0.0.Task_OrderProcessing），继续执行

# 多候选 → 列候选请用户选择
.metaforge/scripts/metaforge-pro.sh cognition resolve "订单"
# 预期: 列出 fqn/name/description 候选，请用户选择，不擅自猜测

# 零命中 → 终止并给原因与平台清单
.metaforge/scripts/metaforge-pro.sh cognition resolve "不存在的任务xxx"
# 预期: 终止执行，输出原因 + 平台当前已发布 Bundle 清单，不臆测 FQN
```

### V10 错误处理与认知新鲜度（FR-ERR/FR-VER）

```bash
# 无效模板 → 34001 中文提示
.metaforge/scripts/metaforge-pro.sh cognition execute not-a-template --bundles order:1.0.0
# 预期: "模板 xxx 不存在，请检查模板 ID"

# 服务不可达 → 网络错误中文提示（含地址）
META_FORGE_SERVER_URL=http://localhost:9999 .metaforge/scripts/metaforge-pro.sh health
# 预期: "无法连接 MetaForge 服务端：http://localhost:9999"

# 瞬时故障重试（FR-025）: 网络错误/34006/34005 自动重试 1 次后仍失败 → 中文提示

# 版本锚（FR-VER-001/002）
# 每次输出含 data_version_anchors；对比前后两次查询，某 Bundle 版本变化 → 提示"认知可能已过期"
# （对比由调用方完成，本产品不保存状态）
```

## 验收对照（spec 验收标准映射）

| 场景 | 对应验收点 |
|---|---|
| V2 模板投影 | 命令清单与服务端实际注册模板对齐（§8.1 交付形态） |
| V3/V4 | 平台发现/领域导航能力验收（§8.1 元认知指导能力） |
| V5/V6/V7 | 任务认知/实体指导/子任务收窄验收（§8.1） |
| V8 | 自由视角组合 + 影响感知验收（§8.1） |
| V9 | FQN 推测验收（唯一/多候选/零命中，§8.1 FQN 推测） |
| V10 | 错误映射 + 版本锚 + 幂等 + 双格式验收（§8.1/§8.2） |
| V1 | 开发态环境 `.metaforge/` 验收（§8.1 开发态环境） |

## 已定义未验证（需真实服务端/上料）

- 端到端性能目标（SC-011: task-brief ≤500ms、step-guide ≤150ms、CLI 开销 <5%）——需在真实服务端环境测量
- 真实业务 Bundle 语义内容（R2）——mock 仅验证消费链路，语义以服务端为准
