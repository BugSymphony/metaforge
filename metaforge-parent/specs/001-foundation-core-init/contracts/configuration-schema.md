# Configuration Schema: foundation-core 配置项参考

所有配置项使用 Spring Boot 原生键名，不引入 `metaforge.*` 自定义命名空间。配置集中在 `metaforge-boot/src/main/resources/` 下（`application.yml` 或按 BC 拆分的 `application-<bc-name>.yml`）。

---

## 1. 数据源配置

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.datasource.url` | String | `jdbc:postgresql://localhost:5432/metaforge` | valid JDBC URL | PostgreSQL 连接地址 |
| `spring.datasource.username` | String | `metaforge` | — | 数据库用户名 |
| `spring.datasource.password` | String | `metaforge` | — | 数据库密码 |
| `spring.datasource.driver-class-name` | String | `org.postgresql.Driver` | PostgreSQL driver | JDBC 驱动类 |
| `spring.datasource.hikari.maximum-pool-size` | int | 10 | 1–100 | 连接池最大连接数 |
| `spring.datasource.hikari.minimum-idle` | int | 5 | 0–maximum-pool-size | 连接池最小空闲连接数 |
| `spring.datasource.hikari.idle-timeout` | long | 300000 | ≥ 10000 | 空闲连接超时（ms） |
| `spring.datasource.hikari.connection-timeout` | long | 20000 | ≥ 1000 | 获取连接超时（ms） |
| `spring.datasource.hikari.max-lifetime` | long | 1200000 | ≥ 30000 | 连接最大生存时间（ms） |

---

## 2. 缓存配置

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.cache.type` | String | `caffeine` | `caffeine` \| `none` | 缓存类型（MVP 仅支持 Caffeine） |
| `spring.cache.caffeine.spec` | String | `expireAfterWrite=30m,maximumSize=1000,recordStats` | Caffeine spec string | 缓存规格（TTL、容量、统计） |

**Caffeine spec 格式**: `expireAfterWrite=<duration>,maximumSize=<size>,recordStats`
- `expireAfterWrite`: 写入后过期时间（支持 d/h/m/s 单位）
- `maximumSize`: 最大缓存条目数
- `recordStats`: 是否记录缓存统计（暴露到 Actuator metrics）

---

## 3. Jackson 序列化配置

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.jackson.date-format` | String | `yyyy-MM-dd HH:mm:ss` | SimpleDateFormat pattern | 全局日期/时间格式化 |
| `spring.jackson.time-zone` | String | `Asia/Shanghai` | Java TimeZone ID | 全局时区 |
| `spring.jackson.default-property-inclusion` | String | `non_null` | `always` \| `non_null` \| `non_absent` \| `non_empty` \| `non_default` \| `use_defaults` | 属性包含策略 |
| `spring.jackson.serialization.write-dates-as-timestamps` | boolean | false | — | 日期序列化格式（false=字符串，true=时间戳） |

---

## 4. 虚拟线程配置

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.threads.virtual.enabled` | boolean | true | — | 全局启用虚拟线程 |

**生效范围**:
- **Tomcat 请求处理线程池**: 自动切换到虚拟线程
- **`@Async` 异步任务**: `SimpleAsyncTaskExecutor` 使用虚拟线程工厂
- **`@Scheduled` 定时任务**: 使用虚拟线程执行（如有）

---

## 5. Flyway 配置

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.flyway.enabled` | boolean | true | — | 启用 Flyway 迁移 |
| `spring.flyway.locations` | String | `classpath:db/migration` | classpath paths | 迁移脚本路径（支持逗号分隔多个路径） |
| `spring.flyway.baseline-on-migrate` | boolean | true | — | 对非空数据库执行 baseline |
| `spring.flyway.baseline-version` | String | `1` | — | Baseline 版本号 |
| `spring.flyway.validate-on-migrate` | boolean | true | — | 迁移前校验已有脚本的 checksum |

---

## 6. 服务端口配置

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `server.port` | int | 8080 | 1–65535 | 应用 HTTP 端口 |

---

## 7. Actuator 可观测性配置

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `management.endpoints.web.exposure.include` | String | `health,info,metrics` | comma-separated endpoint IDs | 暴露的 Actuator 端点 |
| `management.endpoint.health.show-details` | String | `when-authorized` | `never` \| `when-authorized` \| `always` | 健康检查详情展示策略 |

---

## 8. SpringDoc OpenAPI 配置

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `springdoc.swagger-ui.tags-sorter` | String | `alpha` | `alpha` \| `order` | Swagger UI 标签排序方式 |
| `springdoc.api-docs.path` | String | `/v3/api-docs` | valid path | OpenAPI JSON 文档路径 |

---

## 9. 安全基线配置

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `server.max-http-request-header-size` | String | `8KB` | — | 请求头最大尺寸 |
| `spring.servlet.multipart.max-file-size` | String | `10MB` | — | 文件上传最大尺寸 |
| `spring.servlet.multipart.max-request-size` | String | `10MB` | — | 请求体最大尺寸 |

---

## 10. 国际化配置

| Property | Type | Default | Range | Description |
|----------|------|---------|-------|-------------|
| `spring.messages.basename` | String | `i18n/messages` | classpath paths | 国际化资源文件路径（逗号分隔） |
| `spring.messages.encoding` | String | `UTF-8` | — | 资源文件编码 |
| `spring.messages.fallback-to-system-locale` | boolean | false | — | 无匹配语言时是否回退到系统默认语言 |
| `spring.messages.use-code-as-default-message` | boolean | true | — | 无匹配消息键时是否使用键名本身作为默认消息 |

---

## 示例完整配置 (`application.yml`)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/metaforge
    username: metaforge
    password: metaforge
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1200000

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

  cache:
    type: caffeine
    caffeine:
      spec: expireAfterWrite=30m,maximumSize=1000,recordStats

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false

  threads:
    virtual:
      enabled: true

  messages:
    basename: i18n/messages
    encoding: UTF-8
    fallback-to-system-locale: false
    use-code-as-default-message: true

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized

springdoc:
  swagger-ui:
    tags-sorter: alpha
```

---

## 配置组织方式

### 方式一：单一 application.yml（推荐用于 <5 个 BC）

所有配置集中在 `metaforge-boot/src/main/resources/application.yml`。

### 方式二：按 BC 拆分（推荐用于 ≥5 个 BC）

- `application.yml`: 全局基础设施配置（数据源、缓存、端口等）
- `application-<bc-name>.yml`: BC 专属配置
- 在 `application.yml` 中通过 `spring.config.import` 统一引入:

```yaml
spring:
  config:
    import:
      - classpath:application-bc-sample.yml
      - classpath:application-bc-user.yml
```
