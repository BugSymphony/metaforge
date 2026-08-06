# REST API Contract: 统一响应格式

## Overview

foundation-core 所有 REST 接口遵循统一响应体格式。本文档定义响应结构、错误码体系、HTTP 状态码映射。面向 API 消费者（前端/外部系统/Agent 消费端）。

---

## 统一响应体格式

### Success Response

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| code | integer | Yes | 200 for all successful responses |
| message | string | Yes | "success" for successful operations |
| data | any | No | Response payload, `null` if no data |
| traceId | string | Yes | 32-char hex UUID for request tracing |

### Error Response

```json
{
  "code": 20001,
  "message": "参数校验失败: 字段 'email' 格式不正确",
  "data": null,
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| code | integer | Yes | Error code (see Error Code Catalog) |
| message | string | Yes | Human-readable error message (Chinese or i18n) |
| data | null | Yes | Always `null` on error |
| traceId | string | Yes | 32-char hex UUID, use this for support tickets |

### Response Headers

| Header | Type | Value |
|--------|------|-------|
| `X-Trace-Id` | string | Same value as `traceId` field in response body |
| `Content-Type` | string | `application/json;charset=UTF-8` |

---

## Error Code Catalog

### System Errors (10000-19999)

| Code | Message (zh-CN) | HTTP Status | Description |
|------|-----------------|-------------|-------------|
| 10000 | 系统内部错误 | 500 | Unclassified internal error |
| 10001 | 系统异常 | 500 | System operation failure |
| 10002 | 数据库操作异常 | 500 | Database connection/query/update failure |
| 10003 | 远程服务调用异常 | 502 | Downstream service call failed |
| 10004 | 资源不存在 | 404 | Requested resource not found (use for 404) |

### Validation Errors (20000-29999)

| Code | Message (zh-CN) | HTTP Status | Description |
|------|-----------------|-------------|-------------|
| 20001 | 参数校验失败 | 400 | JSR-380 validation failure (field-level details in message) |
| 20002 | 业务校验失败 | 422 | Business rule validation failure |
| 20003 | 请求体解析失败 | 400 | Malformed JSON or unsupported Content-Type |
| 20004 | 参数类型不匹配 | 400 | Type conversion failure (e.g., string to int) |
| 20005 | 缺少必填参数 | 400 | Required parameter missing |
| 20006 | 请求方法不支持 | 405 | HTTP method not allowed for this endpoint |

### Business Errors (30000-49999 - BC allocated)

Each business BC uses this range for domain-specific errors. BCs must document their error codes in their own contracts.

| BC | Code Range | Source |
|----|-----------|--------|
| bc-sample | 30100-30199 | bc-sample contract |
| (future BCs) | 30000-49999 | Per-BC contracts |

### Third-party Errors (50000-59999)

| Code | Message (zh-CN) | HTTP Status | Description |
|------|-----------------|-------------|-------------|
| 50001 | 外部服务调用超时 | 504 | Gateway/request timeout to upstream service |
| 50002 | 外部服务返回错误 | 502 | Upstream service returned an error |

---

## HTTP Status Code Mapping

| HTTP Status | Typical Error Codes | Usage |
|-------------|---------------------|-------|
| 200 OK | 200 | Successful request |
| 201 Created | 200 | Resource created (uses same success code=200) |
| 400 Bad Request | 20001, 20003, 20004, 20005 | Client input error |
| 404 Not Found | 10004 | Resource not found |
| 405 Method Not Allowed | 20006 | Wrong HTTP method |
| 422 Unprocessable Entity | 20002 | Business validation failure |
| 500 Internal Server Error | 10000, 10001, 10002 | Server-side error |
| 502 Bad Gateway | 10003, 50002 | Upstream service error |
| 504 Gateway Timeout | 50001 | Upstream timeout |

**Convention**: HTTP status code provides protocol-level semantics; `code` field provides application-level error categorization. Always check `code` for error handling logic.

---

## i18n Support

Error messages support internationalization based on `Accept-Language` header:

| Header Value | Language |
|-------------|----------|
| `zh-CN` (default) | Simplified Chinese |
| `en-US` | English |

**Example**:

```
# Request with Accept-Language: en-US
GET /api/users/999 HTTP/1.1
Accept-Language: en-US

# Response
{
  "code": 10004,
  "message": "User not found: id=999",
  "data": null,
  "traceId": "..."
}

# Request with Accept-Language: zh-CN
GET /api/users/999 HTTP/1.1
Accept-Language: zh-CN

# Response
{
  "code": 10004,
  "message": "用户不存在: id=999",
  "data": null,
  "traceId": "..."
}
```

---

## Pagination Convention

Endpoints returning collections use paginated response:

**Request**:
```
GET /api/users?page=1&size=20&sort=createdTime:desc
```

**Parameters**:
| Parameter | Type | Default | Constraint |
|-----------|------|---------|------------|
| page | integer | 1 | >= 1 |
| size | integer | 20 | 1-100 |
| sort | string | null | `field:asc` or `field:desc` |

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [...],
    "total": 150,
    "page": 1,
    "size": 20,
    "totalPages": 8
  },
  "traceId": "..."
}
```
