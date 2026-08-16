---
id: metamodel-governance.rest-api
protocol: REST
version: 1.0.0
owner: metamodel-governance
description: 提供元模型治理 BC 的 REST API 接口契约，覆盖 Bundle 管理、版本管理、EntitySchema/RelationSchema/AttributeTemplate 管理、Package 管理、导出清单管理、依赖管理、声明式导入导出及校验能力，所有接口返回 ApiResponse<T> 统一响应格式。
type: business
---

# REST API Contract: metamodel-governance

**Protocol**: RESTful HTTP/1.1 + JSON
**Base Path**: `/api/v1/metamodel`
**Version**: 1.0.0

> 所有接口返回 `ApiResponse<T>` 统一响应格式。分页接口支持 `page`、`size`、`sort` 参数。

---

## Overview

本契约定义元模型治理 BC 对外暴露的 REST 管理接口，覆盖元模型定义全生命周期：Bundle 治理、版本治理、核心语义元素（EntitySchema / RelationSchema / AttributeTemplate）、Package 命名空间、导出清单、跨 Bundle 依赖、声明式批量导入导出及两级校验能力。

---

## Endpoints

### 1. Bundle 管理

#### POST /api/v1/metamodel/bundles

创建 Bundle。

**Request Body**:
```json
{
  "fqn": "order",
  "name": "订单领域",
  "description": "覆盖电商订单的完整生命周期建模，含订单创建、支付、履约、售后等核心子领域",
  "owner": "zhangsan"
}
```

**Response** (`201 Created`):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fqn": "order",
    "name": "订单领域",
    "description": "覆盖电商订单的完整生命周期建模...",
    "owner": "zhangsan",
    "isSystem": false,
    "embedding": null,
    "createdTime": "2026-07-31 10:00:00",
    "updatedTime": "2026-07-31 10:00:00"
  },
  "traceId": "a1b2c3d4..."
}
```

**Errors**:
| 错误码 | 场景 |
|--------|------|
| 30101 | FQN 已存在 |

#### GET /api/v1/metamodel/bundles/{fqn}

按 FQN 查询 Bundle。

> fqn 参数示例: `order`

**Response** (`200 OK`): 同创建响应。

**Errors**:
| 错误码 | 场景 |
|--------|------|
| 30102 | FQN 不存在 |

#### GET /api/v1/metamodel/bundles

分页查询 Bundle 列表。

**Query Params**: `page=1&size=20&sort=createdTime:desc`

**Response** (`200 OK`): `ApiResponse<PageResult<BundleDto>>`

#### PUT /api/v1/metamodel/bundles/{fqn}

更新 Bundle 元信息。

**Request Body**:
```json
{
  "name": "订单领域 V2",
  "description": "更新后的描述",
  "owner": "lisi"
}
```

---

### 2. Bundle 版本管理

#### POST /api/v1/metamodel/bundles/{bundleFqn}/versions

从最新已发布版本创建草稿版本。

**Request Body**:
```json
{
  "upgradeLevel": "MINOR"
}
```

**Response** (`201 Created`):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fqn": "order:0.1.0",
    "bundleFqn": "order",
    "status": "DRAFT",
    "sourceVersionFqn": "order:0.0.1",
    "upgradeLevel": "MINOR",
    "createdTime": "2026-07-31 11:00:00"
  },
  "traceId": "a1b2c3d4..."
}
```

**Errors**:
| 错误码 | 场景 |
|--------|------|
| 30102 | Bundle 不存在 |
| 30103 | 已存在草稿版本 |
| 30104 | 无可用的已发布源版本 |

#### POST /api/v1/metamodel/versions/{versionFqn}/publish

发布草稿版本。

> versionFqn 参数示例: `order:0.1.0`

**Response** (`200 OK`):
```json
{
  "code": 200,
  "message": "版本发布成功",
  "data": {
    "fqn": "order:0.1.0",
    "status": "PUBLISHED"
  },
  "traceId": "a1b2c3d4..."
}
```

**Errors**:
| 错误码 | 场景 |
|--------|------|
| 30102 | 版本不存在 |
| 30103 | 非草稿态（可能已发布） |
| 30104 | 升级等级与变更不匹配 |
| 30105 | 依赖链存在循环依赖 |
| 30106 | 属性名冲突 |
| 30108 | 导出清单校验失败 |

#### GET /api/v1/metamodel/bundles/{bundleFqn}/versions

查询 Bundle 的所有版本列表。

**Query Params**: `page=1&size=20`

---

### 3. EntitySchema 管理

#### POST /api/v1/metamodel/entity-schemas

创建 EntitySchema。

**Request Body**:
```json
{
  "packageFqn": "order:0.1.0.pkg_order",
  "segment": "Order",
  "name": "订单实体",
  "description": "描述电商订单的核心概念，包含订单生命周期状态与金额信息。适用场景：订单创建、支付、履约。",
  "nativeAttributes": [
    {
      "name": "orderAmount",
      "type": "number",
      "required": true,
      "description": "订单金额",
      "constraints": { "minimum": 0 }
    }
  ],
  "mountedTemplateFqns": ["order:0.1.0.AuditFields"]
}
```

**Response** (`201 Created`):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fqn": "order:0.1.0.pkg_order.Order",
    "packageFqn": "order:0.1.0.pkg_order",
    "name": "订单实体",
    "description": "描述电商订单的核心概念...",
    "nativeAttributes": [...],
    "mountedTemplateFqns": ["order:0.1.0.AuditFields"],
    "jsonSchema": null,
    "embedding": null,
    "enabled": false,
    "shortName": "Order",
    "bundleCode": "order",
    "version": "0.1.0",
    "createdTime": "2026-07-31 11:30:00",
    "updatedTime": "2026-07-31 11:30:00"
  },
  "traceId": "a1b2c3d4..."
}
```

**Errors**:
| 错误码 | 场景 |
|--------|------|
| 30101 | FQN 已存在 |
| 30102 | Package 不存在 |
| 30103 | 所属版本非草稿态 |
| 30106 | 属性名冲突 |

#### GET /api/v1/metamodel/entity-schemas/{fqn}

按 FQN 查询 EntitySchema（含派生字段）。

> fqn 参数示例: `order:0.1.0.pkg_order.Order`

#### PUT /api/v1/metamodel/entity-schemas/{fqn}

更新 EntitySchema 元信息（仅草稿态）。

#### DELETE /api/v1/metamodel/entity-schemas/{fqn}

删除 EntitySchema（仅草稿态，无外部引用）。

#### GET /api/v1/metamodel/entity-schemas

分页查询 EntitySchema 列表。通过 FQN 前缀集合过滤（多值 OR 逻辑，交集取并集）。

**Query Params**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fqnPrefix` | String[] | 否 | FQN 前缀列表（可重复），如 `?fqnPrefix=order:1.0.0.pkg_order.&fqnPrefix=order:1.0.0.pkg_common.`，匹配所有指定前缀下的元素 |
| `page` | int | 否(默认1) | 页码 |
| `size` | int | 否(默认20) | 每页大小 |

**Example**: `GET /api/v1/metamodel/entity-schemas?fqnPrefix=order:1.0.0.pkg_order.&page=1&size=20`

---

### 4. RelationSchema 管理

#### POST /api/v1/metamodel/relation-schemas

创建 RelationSchema。

**Request Body**:
```json
{
  "packageFqn": "order:0.1.0.pkg_order",
  "segment": "Order_contains_Item",
  "name": "订单包含商品",
  "description": "订单与订单项之间的组成关系",
  "sourceFqn": "order:0.1.0.pkg_order.Order",
  "targetFqn": "order:0.1.0.pkg_order.Item",
  "associationType": "组成",
  "cardinalitySource": "1",
  "cardinalityTarget": "1..*"
}
```

**Errors**:
| 错误码 | 场景 |
|--------|------|
| 30102 | sourceFqn 或 targetFqn 不存在或不可见 |
| 30101 | FQN 已存在 |

#### GET /api/v1/metamodel/relation-schemas/{fqn}

按 FQN 查询 RelationSchema。

#### GET /api/v1/metamodel/relation-schemas

分页查询 RelationSchema 列表。Query Params 同 [GET /entity-schemas](#get-apiv1metamodelentity-schemas)（`fqnPrefix` 集合 + `page` / `size`）。

#### PUT /api/v1/metamodel/relation-schemas/{fqn}

更新（仅草稿态）。

#### DELETE /api/v1/metamodel/relation-schemas/{fqn}

删除（仅草稿态）。

---

### 5. AttributeTemplate 管理

#### POST /api/v1/metamodel/attribute-templates

创建 AttributeTemplate。

**Request Body**:
```json
{
  "bundleVersionFqn": "order:0.1.0",
  "segment": "AuditFields",
  "name": "审计字段模板",
  "description": "通用审计字段集合",
  "attributeDefinitions": [
    {
      "name": "createdBy",
      "type": "string",
      "required": true,
      "description": "创建人"
    }
  ]
}
```

**Errors**:
| 错误码 | 场景 |
|--------|------|
| 30101 | FQN 已存在 |
| 30102 | BundleVersion 不存在 |
| 30103 | 非草稿态 |

#### GET /api/v1/metamodel/attribute-templates/{fqn}

按 FQN 查询 AttributeTemplate。

#### PUT /api/v1/metamodel/attribute-templates/{fqn}

更新（仅草稿态）。

#### DELETE /api/v1/metamodel/attribute-templates/{fqn}

删除（仅草稿态，需前置校验无 EntitySchema/RelationSchema 引用）。

---

### 6. Package 管理

#### POST /api/v1/metamodel/packages

创建 Package。

**Request Body**:
```json
{
  "bundleVersionFqn": "order:0.1.0",
  "parentPackageFqn": null,
  "segment": "pkg_order",
  "description": "订单领域子包，承载订单实体及关联定义"
}
```

**Errors**:
| 错误码 | 场景 |
|--------|------|
| 30107 | 嵌套深度超限 |
| 30102 | 父 Package 不存在 |

#### GET /api/v1/metamodel/packages/{fqn}

按 FQN 查询 Package。

#### GET /api/v1/metamodel/packages

分页查询。Query: `bundleVersionFqn=order:0.1.0`

#### DELETE /api/v1/metamodel/packages/{fqn}

删除（仅草稿态，Package 下无元素）。

---

### 7. 导出清单管理

#### PUT /api/v1/metamodel/versions/{versionFqn}/export-manifest

配置导出清单。

**Request Body**:
```json
{
  "packageFqns": [
    "order:0.1.0.pkg_order",
    "order:0.1.0.pkg_common"
  ]
}
```

#### GET /api/v1/metamodel/versions/{versionFqn}/export-manifest

查询导出清单。

---

### 8. 依赖管理

#### POST /api/v1/metamodel/versions/{versionFqn}/dependencies

声明依赖。

**Request Body**:
```json
{
  "targetVersionFqn": "common:1.0.0"
}
```

**Errors**:
| 错误码 | 场景 |
|--------|------|
| 30102 | 目标版本不存在 |
| 30105 | 引入循环依赖 |
| 30111 | 目标版本未发布 |

#### DELETE /api/v1/metamodel/versions/{versionFqn}/dependencies/{targetVersionFqn}

移除依赖声明。

#### GET /api/v1/metamodel/versions/{versionFqn}/dependencies

查询依赖列表。

---

### 9. 导入导出

#### GET /api/v1/metamodel/export/bundle/{versionFqn}

导出完整 Bundle（YAML/JSON）。

**Query Params**: `format=YAML`

#### GET /api/v1/metamodel/export/package/{packageFqn}

导出单个 Package（含依赖属性模板组）。

#### POST /api/v1/metamodel/import

导入元模型。

**Request Body** (multipart/form-data):
- `file`: YAML/JSON 文件
- `format`: YAML 或 JSON
- `strategy`: SKIP 或 ERROR

**Errors**:
| 错误码 | 场景 |
|--------|------|
| 30112 | 导入解析失败（依赖缺失、格式错误） |
| 30103 | 目标为已发布版本（禁止覆盖） |

---

### 10. 校验

#### POST /api/v1/metamodel/versions/{versionFqn}/validate/save

写入轻量校验。

#### POST /api/v1/metamodel/versions/{versionFqn}/validate/publish

发布前全局校验（仅校验不发布）。

**Response** (`200 OK`):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "passed": false,
    "errors": [
      {
        "elementFqn": "order:0.1.0.pkg_order.Order",
        "field": "mountedTemplateFqns",
        "errorCode": "30106",
        "message": "属性名冲突: 'createdAt' 在属性模板组 'order:0.1.0.AuditFields' 与原生属性中重复"
      }
    ],
    "warnings": []
  },
  "traceId": "a1b2c3d4..."
}
```

#### POST /api/v1/metamodel/versions/{versionFqn}/validate/preview

预览发布（执行全量校验 + JSON Schema 生成，不落库）。

---

## Error Codes

本 BC 使用错误码范围 **30100-30199**，统一由 foundation-core 全局异常处理机制映射到 `ApiResponse<T>` 错误响应。

| 错误码 | 场景 |
|--------|------|
| 30101 | FQN 已存在 |
| 30102 | FQN / 引用目标不存在或不可见 |
| 30103 | 非草稿态 / 已发布（禁止修改或覆盖） |
| 30104 | 升级等级与变更不匹配 / 无可用的已发布源版本 |
| 30105 | 依赖链存在循环依赖 |
| 30106 | 属性名冲突 |
| 30107 | Package 嵌套深度超限 |
| 30108 | 导出清单校验失败 |
| 30111 | 目标版本未发布 |
| 30112 | 导入解析失败（依赖缺失、格式错误） |

## Authentication Rules

所有 REST 接口为管理类查询与写操作，由平台统一安全基线管控，本 BC 不自定义安全过滤器或 CORS 规则。写操作遵循宪法 IX 纯元数据边界，仅操作 `metamodel_governance` Schema。
