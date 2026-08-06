---
name: metaforge-catalog
description: 平台发现——列出平台已发布的 Bundle 列表、版本锚与主题域概要，可作为选型与接入起点。调用 /metaforge.catalog 命令或 .metaforge/scripts/metaforge-pro.sh。
metadata:
  capability: platform-discovery
  template: bundle-catalog
  command: metaforge.catalog
---

## 用途

为 Agent 提供**平台发现**能力：列出平台当前已发布的 Bundle 列表（fqn/name/description）、各 Bundle 版本锚与主题域概要，作为选型与接入的起点。内容可直接注入上下文（零解析开销）。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--page-size <n>` | 否 | 分页大小 |
| `--cursor <n>` | 否 | 分页游标（配合 `next_cursor` 续翻） |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json） |
| `--verbose` | 否 | 排障：输出原始请求/响应与 X-Trace-Id |

## 调用示例

```bash
# 获取平台 Bundle 清单
.metaforge/scripts/metaforge-pro.sh cognition execute bundle-catalog

# 分页续翻
.metaforge/scripts/metaforge-pro.sh cognition execute bundle-catalog --page-size 10 --cursor <next_cursor>
```

或直接调用命令：`/metaforge.catalog`

## 输出格式

- **json（默认）**: `ApiResponse<T>` 原样透传，含 Bundle 列表、版本锚与主题域概要
- **分页**: 支持 `--page-size` 与游标续翻（FR-DLV-006）
- **版本锚**: 各 Bundle 已发布版本号 + 查询时间戳
- **错误**: 服务端错误码与网络错误 → 简体中文提示

## 约束

- 不直接访问服务端（FR-DLV-009）——REST 调用统一经 `.metaforge/scripts/` 脚本承载
- 无状态幂等，可安全重复执行
