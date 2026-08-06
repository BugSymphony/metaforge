---
description: 领域导航——沿主题域层级逐层下钻，返回层级树、has_more 与 entryStepFqn（领域导航能力）
handoffs: 
  - label: 任务认知简报
    agent: metaforge.task-brief
    prompt: 我已定位到目标主题域/任务，需要获取任务认知简报
    send: true
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## 用途

本命令为 Agent 提供**领域导航**能力（spec §4.1 领域导航）：沿 L1 SubjectDomainGroup → L2 SubjectDomain → Task 逐层下钻，返回层级树、`has_more`（是否还有更多子节点）与 `entry_step_fqn`（进入点步骤 FQN），规划探索路径。输出可直接注入 Agent 上下文（零解析开销）。

**指导的问题**: 这个领域里有什么？怎么逐层下钻？
**指导的行为**: 探索路径规划。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--bundles <a,b,...>` | ✅ | Bundle FQN 列表（如 `order:1.0.0`）；缺失时命令中止并提示用法 |
| `--subject-domain <fqn>` | 否 | 主体域 FQN（下钻锚点） |
| `--expand <lazy\|all>` | 否 | 展开模式（默认 lazy；all 全量展开） |
| `--page-size <n>` | 否 | 分页大小 |
| `--cursor <n>` | 否 | 分页游标 |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json） |
| `--verbose` | 否 | 输出原始请求/响应与 X-Trace-Id |

## 执行方式

本命令**不直接访问服务端**（FR-DLV-009），仅调用脚本入口：

```bash
# 逐层下钻（L1 开始）
.metaforge/scripts/metaforge-pro.sh cognition execute navigate --bundles order:1.0.0

# 指定主题域下钻 + 分页
.metaforge/scripts/metaforge-pro.sh cognition execute navigate \
  --bundles order:1.0.0 --subject-domain order:1.0.0.SubjectDomain_Order --page-size 20
```

## 输出说明

- **json（默认）**: 原样透传 `ApiResponse<T>`，含层级树、`has_more`、`entry_step_fqn` 与版本锚
- **分页**: 列表型输出支持 `--page-size` 与游标续翻（FR-DLV-006）
- **版本锚**: 各 Bundle 已发布版本号 + 查询时间戳
- **错误提示**: 服务端错误码与网络错误映射为简体中文提示
