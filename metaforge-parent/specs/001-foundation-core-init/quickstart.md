# Quickstart: foundation-core 验证指南

本文档提供 foundation-core 基座初始化完成后的端到端验证场景，证明所有核心能力正确生效。

---

## 前置条件

| 前提 | 版本/说明 |
|------|----------|
| JDK | 21 |
| Maven | 3.9+ |
| Docker | 已安装并运行 Docker daemon |
| Docker Compose | 已安装 |

---

## 1. 启动开发环境

```bash
# 在 metaforge-parent/ 目录下
cd metaforge-parent
docker compose up -d
```

**验证**: `docker ps` 应显示 `metaforge-postgres` 容器运行中。

**数据源对齐**: `application.yml` 中默认连接信息与 `docker-compose.yml` 一致：
- Host: `localhost:5432`
- Database: `metaforge`
- User/Password: `metaforge/metaforge`

---

## 2. 全量构建

```bash
cd metaforge-parent
mvn clean install -pl metaforge-boot -am
```

**预期输出**: `BUILD SUCCESS`，所有模块编译通过。

**验证点**:
- [ ] metaforge-common 编译通过（无 Spring 依赖）
- [ ] metaforge-framework 编译通过（含 Spring/JPA/Caffeine 工具）
- [ ] metaforge-server 编译通过（AutoConfiguration 类注册）
- [ ] metaforge-boot 编译通过（聚合所有 BC，Enforcer 校验通过）
- [ ] bc-sample 编译通过（演示 BC 标准接入）

---

## 3. 启动应用（验证运行时基座）

```bash
mvn spring-boot:run -pl metaforge-boot
```

**预期输出**: 控制台输出含 TraceId 的结构化日志，最终显示 `Started MetaforgeApplication in X.XXX seconds`。

### 3.1 验证虚拟线程

查看启动日志中线程名包含 `virtual`：
```
[2026-08-01 14:30:00.123] [virtual-1] [INFO] [a1b2c3d4...] [...]
```

### 3.2 验证 TraceId 自动生成

```bash
curl -i http://localhost:8080/api/sample/hello
```

**预期响应头**:
```
HTTP/1.1 200
X-Trace-Id: a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6
Content-Type: application/json;charset=UTF-8
```

**预期响应体**（统一格式）:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "traceId": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6"
}
```

---

## 4. 验证横切技术能力

### 4.1 统一响应体包装

```bash
# 成功响应
curl http://localhost:8080/api/sample/hello | jq '.code, .message, .traceId'

# 预期: 200, "success", 32位hex字符串
```

### 4.2 全局异常处理

```bash
# 触发参数校验异常
curl -X POST http://localhost:8080/api/sample/validate \
  -H "Content-Type: application/json" \
  -d '{"name":""}'

# 预期响应:
# { "code": 20001, "message": "参数校验失败: ...", "data": null, "traceId": "..." }
```

### 4.3 缓存读写

验证 CacheManager 可用（通过 bc-sample 的 `/api/sample/cache-test` 端点，如有）：

```bash
# 写入缓存
curl -X POST http://localhost:8080/api/sample/cache-test \
  -H "Content-Type: application/json" \
  -d '{"key":"test-key","value":"test-value"}'

# 读取缓存（预期返回缓存中的数据）
curl http://localhost:8080/api/sample/cache-test/test-key
```

### 4.4 OpenAPI 文档

```bash
# 浏览器访问或 curl
curl http://localhost:8080/swagger-ui.html
curl http://localhost:8080/v3/api-docs
```

验证 bc-sample 的 API 在 Swagger UI 中按 `Tag: bc-sample` 分组展示。

### 4.5 国际化 (i18n)

```bash
# 中文响应
curl -H "Accept-Language: zh-CN" http://localhost:8080/api/sample/hello

# 英文响应
curl -H "Accept-Language: en-US" http://localhost:8080/api/sample/hello
```

### 4.6 可观测性 (Actuator)

```bash
# 健康检查
curl http://localhost:8080/actuator/health
# 预期: {"status":"UP"}

# 应用信息
curl http://localhost:8080/actuator/info

# 指标
curl http://localhost:8080/actuator/metrics
```

### 4.7 安全基线

```bash
# XSS 防护 — 发送含 script 标签的请求
curl -X POST http://localhost:8080/api/sample/echo \
  -H "Content-Type: application/json" \
  -d '{"name":"<script>alert(1)</script>"}'

# 预期: 响应中 script 标签被转义
```

---

## 5. 验证 SPI 扩展点

### 5.1 自定义异常处理

在 bc-sample 中实现 `ExceptionHandlerSpi`，验证自定义异常被正确拦截：

```java
@Component
public class SampleExceptionHandler implements ExceptionHandlerSpi {
    @Override
    public ApiResponse<?> handle(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return ApiResponse.error(30101, "BC 自定义: " + e.getMessage());
        }
        return null;
    }
}
```

```bash
# 触发 IllegalArgumentException
curl http://localhost:8080/api/sample/error-test
```

**预期**: 响应 code=30101，格式符合统一响应体。

---

## 6. 验证 Flyway 数据库迁移

```bash
# 查看启动日志 Flyway 输出
# 预期: 日志中包含 "Successfully applied X migration(s)"
```

```bash
# 验证迁移结果（直接查询数据库）
docker exec -it metaforge-postgres psql -U metaforge -d metaforge \
  -c "SELECT version, description, script FROM flyway_schema_history;"
```

**预期输出**: 至少包含 `bc_sample_ddl` 和 `bc_sample_init` 两条迁移记录。

---

## 7. 验证 Maven Enforcer 构建校验

### 7.1 禁止 BC 覆盖版本属性

在 bc-sample 的 `<properties>` 中添加 `<spring-boot.version>3.4.0</spring-boot.version>`，执行 `mvn validate`。

**预期**: 构建失败，错误消息指明禁止覆盖 `spring-boot.version`。

### 7.2 禁止依赖 metaforge-boot

在 bc-sample 的 `pom.xml` 中添加对 `metaforge-boot` 的依赖声明，执行 `mvn validate`。

**预期**: 构建失败，bannedDependencies 规则触发。

---

## 8. 验证测试基座

### 8.1 单元测试 (BaseUnitTest)

```bash
mvn test -pl bc-sample
```

**预期**: 单测试方法执行耗时 < 100ms，不启动 Spring 上下文。

### 8.2 集成测试 (BaseIntegrationTest + TestContainers)

```bash
mvn test -pl bc-sample -Dtest="*IntegrationTest"
```

**预期**: TestContainers 自动启动 PostgreSQL 容器，测试执行数据库读写正常，测试后容器自动销毁。首次执行含镜像拉取耗时 < 30s。

---

## 9. 验证业务 BC 接入流程

按 [build-system-integration.md](./contracts/build-system-integration.md) 创建新 BC 模块：

1. 在 `metaforge-parent/` 根目录创建 `bc-demo/` 目录（含 pom.xml）
2. POM 继承 `metaforge-parent`，依赖 `metaforge-framework`
3. 在 `metaforge-boot/pom.xml` 中添加 `<dependency>` 注册 `bc-demo`
4. 重启应用验证 `bc-demo` 的 Bean 被自动扫描装载

**验证**: 访问 `bc-demo` 的任意端点，响应格式符合统一规范，TraceId 正常。

---

## 10. 验证 common 层依赖边界

```bash
# 检查 common 的 compile scope 依赖
mvn dependency:list -pl metaforge-common -DincludeScope=compile

# 预期输出仅包含:
# com.fasterxml.jackson.core:jackson-core
# com.fasterxml.jackson.core:jackson-databind
# com.fasterxml.jackson.core:jackson-annotations
# org.slf4j:slf4j-api
# (无 spring-*, javax.servlet.*, org.hibernate.*, com.github.benmanes.caffeine.*)
```

---

## 验收检查清单

- [ ] PostgreSQL Docker 容器正常启动
- [ ] `mvn clean install` 全量构建通过
- [ ] 应用启动成功，日志含 TraceId + 虚拟线程标识
- [ ] REST 响应格式符合 `rest-api-contract.md` 定义（code/message/data/traceId）
- [ ] Swagger UI 可访问，API 按 Tag 分组
- [ ] Actuator health/metrics 端点正常
- [ ] XSS 防护生效
- [ ] 缓存读写正常
- [ ] i18n 中英文切换正常
- [ ] 参数校验异常返回统一格式
- [ ] SPI 扩展自定义异常处理器生效
- [ ] Flyway 迁移脚本正确执行
- [ ] Maven Enforcer 禁止版本覆盖/禁止依赖 boot
- [ ] BaseUnitTest 执行 < 100ms
- [ ] BaseIntegrationTest 含 TestContainers 执行通过
- [ ] common 层仅含 Jackson + SLF4J，无框架依赖
- [ ] 新建 BC 可 4 步接入并启动成功
