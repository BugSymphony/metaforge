# MetaForge 认知服务契约文档

面向外部使用的接口契约——6 个场景模板（认知查询） + 1 份公共约定。每个模板一份契约，含模板定位、请求/响应结构、算子详解、错误场景与完整示例。

## 公共约定

| 文档 | 内容 |
|------|------|
| [00-common.md](./00-common.md) | 公共约定：服务入口、请求体、scope 认知边界、响应结构、错误码 |

## 场景模板契约

| 模板 | 文档 | 定位 |
|------|------|------|
| DISCOVER | [03-discover.md](./03-discover.md) | 元模型发现（Bundle/包/实体类型/关系类型盘点） |
| ORIENT | [06-orient.md](./06-orient.md) | 业务域定位（域树逐层下钻） |
| BRIEF | [01-brief.md](./01-brief.md) | 任务/实体全景（画像/流程/规则/能力/关系） |
| GUIDE | [05-guide.md](./05-guide.md) | 单步执行指南（执行步骤/决策步骤双态） |
| FORECAST | [04-forecast.md](./04-forecast.md) | 变更影响链路（影响/风险/冲突/回归） |
| DELEGATE | [02-delegate.md](./02-delegate.md) | 子任务上下文委派（scope 收窄 + 与 BRIEF 协作） |

## 快速查阅

- **服务入口**：`POST http://localhost:8080/api/v1/cognition/{templateId}`（详见 [00-common.md](./00-common.md)）
- **请求体**：`{ scope, params, format, cognitionDepth, agentArchetype, maxTokens }`
- **错误码**：34001（模板不存在）~ 34014（算子选择无效），详见公共约定
- **算子组合**：各模板支持 `params.selectOperators` 自由裁剪算子子集
