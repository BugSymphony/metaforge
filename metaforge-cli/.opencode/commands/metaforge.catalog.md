---
description: 平台发现——列出平台已发布的 Bundle 列表、版本锚与主题域概要（平台发现能力）
handoffs: 
  - label: 领域导航
    agent: metaforge.navigate
    prompt: 我已获得平台 Bundle 清单，需要进入某个主题域逐层下钻
    send: true
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## 用途

本命令为 Agent 提供**平台发现**能力（spec §4.1 平台发现）：列出平台当前已发布的 Bundle 列表（fqn/name/description）、各 Bundle 版本锚与主题域概要，作为选型与接入的起点。输出可直接注入 Agent 上下文（零解析开销）。

**指导的问题**: 平台上有哪些可用的库？
**指导的行为**: 选型/接入起点。

## 参数

| 参数 | 必填 | 说明 |
|---|---|---|
| `--page-size <n>` | 否 | 分页大小 |
| `--cursor <n>` | 否 | 分页游标（配合 `next_cursor` 续翻） |
| `--format <json\|prompt>` | 否 | 输出格式（默认 json） |
| `--verbose` | 否 | 输出原始请求/响应与 X-Trace-Id |

## 执行方式

本命令**不直接访问服务端**（FR-DLV-009），仅调用脚本入口：

```bash
# 获取平台 Bundle 清单
.metaforge/scripts/metaforge-pro.sh cognition execute bundle-catalog

# 分页续翻（结果超单页时，用返回的 next_cursor 继续）
.metaforge/scripts/metaforge-pro.sh cognition execute bundle-catalog --page-size 10 --cursor <next_cursor>
```

## 输出说明

- **json（默认）**: 原样透传 `ApiResponse<T>`，含 Bundle 列表、`data_version_anchors` 版本锚与主题域概要
- **分页**: 列表型输出支持 `--page-size` 与游标续翻（FR-DLV-006）
- **版本锚**: 各 Bundle 已发布版本号 + 查询时间戳，判断认知是否过期
- **错误提示**: 服务端错误码与网络错误映射为简体中文提示
