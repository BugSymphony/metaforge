---
name: metaforge-navigate
description: 领域导航——沿主题域层级逐层下钻，返回层级树、has_more 与 entryStepFqn，规划探索路径。调用 /metaforge.navigate 命令或 .metaforge/scripts/metaforge-pro.sh。
metadata:
  capability: domain-navigation
  template: navigate
  command: metaforge.navigate
---

## 用途

为 Agent 提供**领域导航**能力：沿 L1 SubjectDomainGroup → L2 SubjectDomain → Task 逐层下钻，返回层级树、`has_more` 与 `entry_step_fqn`，规划探索路径。内容可直接注入上下文（零解析开销）。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--bundles <a,b,...>` | ✅ | Bundle FQN 列表（如 `order:1.0.0`）；缺失时命令中止 |
| `--subject-domain <fqn>` | 否 | 主体域 FQN（下钻锚点） |
| `--expand <lazy\|all>` | 否 | 展开模式（默认 lazy；all 全量展开） |
| `--page-size <n>` | 否 | 分页大小 |
| `--cursor <n>` | 否 | 分页游标 |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json） |
| `--verbose` | 否 | 排障：输出原始请求/响应与 X-Trace-Id |

## 调用示例

```bash
# 逐层下钻（L1 开始）
.metaforge/scripts/metaforge-pro.sh cognition execute navigate --bundles order:1.0.0

# 指定主题域下钻 + 分页
.metaforge/scripts/metaforge-pro.sh cognition execute navigate \
  --bundles order:1.0.0 --subject-domain order:1.0.0.SubjectDomain_Order --page-size 20
```

或直接调用命令：`/metaforge.navigate --bundles order:1.0.0`

## 输出格式

- **json（默认）**: `ApiResponse<T>` 原样透传，含层级树、`has_more`、`entry_step_fqn` 与版本锚
- **分页**: 支持 `--page-size` 与游标续翻（FR-DLV-006）
- **版本锚**: 各 Bundle 已发布版本号 + 查询时间戳
- **错误**: 服务端错误码与网络错误 → 简体中文提示

## 约束

- 不直接访问服务端（FR-DLV-009）——REST 调用统一经 `.metaforge/scripts/` 脚本承载
- 无状态幂等，可安全重复执行
