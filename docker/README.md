# MetaForge 认知服务 —— Docker 快速测试环境

一键启动 MetaForge 认知服务端（6 模板）+ PostgreSQL，自动初始化数据（基础 agent 库 + 4 个示例域），让用户可以快速测试认知接口。

## 前置条件

- Docker + Docker Compose（v2）

## 快速开始

```bash
cd /data/ext/source-8/metaforge/docker

# 1. 构建 + 启动（首次构建较慢：Maven 下载依赖 + 编译全部模块）
docker compose up --build -d

# 2. 等待就绪（boot 自动跑 V4 迁移 + 应用 seed，约 1-2 分钟）
docker compose logs -f metaforge-boot   # 看到 "[5/5] 服务运行中" 即完成

# 3. 验证
curl http://localhost:8080/actuator/health   # {"status":"UP"}
```

## 验证认知服务

```bash
# BRIEF 任务全景
curl -s -X POST http://localhost:8080/api/v1/cognition/BRIEF \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"entity_fqn":"metaforge:1.0.0.agent.Task_InventoryCheck"},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'

# FORECAST 变更影响
curl -s -X POST http://localhost:8080/api/v1/cognition/FORECAST \
  -H 'Content-Type: application/json' \
  -d '{
    "scope": {"bundles":["metaforge:1.0.0"],"packages":[],"domainGroups":[],"domains":[],"entitySchemas":[]},
    "params": {"entity_fqn":"metaforge:1.0.0.agent.Step_CheckInventory","change_type":"MODIFY"},
    "format":"JSON","cognitionDepth":"L3","agentArchetype":"EXECUTION","maxTokens":8000
  }'
```

> 返回 `"code": 200` 即成功。

## 用测试脚本验证（宿主机）

boot 端口已映射到宿主机的 `localhost:8080`，可直接跑认知测试脚本：

```bash
cd /data/ext/source-8/metaforge/test/cognition/scripts
./run-all-debug.sh            # 36 场景全回归
./test-forecast.sh            # FORECAST 模板测试
./test-errors.sh              # 错误码测试
```

## 用 opencode Agent 消费（可选）

若使用 opencode 认知消费插件（`.opencode/plugins/metaforge.ts`），它默认连 `http://localhost:8080`，启动后即可让 Agent 通过 `metaforge_cognition` / `metaforge_resolve` 工具消费认知服务。

## 数据说明（内置 seed）

| seed | 内容 |
|------|------|
| `01-agent-library.sql` | 基础 agent 库（履约/库存/支付域 + 决策步骤 + 起点子任务 + 流程关系全覆盖） |
| `02-medical.sql` | 医疗处方审核域（业务对象结构 L3-L5 示例） |
| `03-industrial.sql` | 工业设备预测性维护域（决策嵌套 + 两级阈值） |
| `04-datacenter.sql` | 数据中心机柜巡检域（3 业务对象 + 决策嵌套） |
| `05-supply-chain.sql` | 供应链库存补货域（3 业务对象 + 供应商择优） |
| `06-java-diag.sql` | java-diag 独立 Bundle 元模型（九阶段诊断链路 EntitySchema/RelationSchema + 依赖声明） |
| `07-java-diag-scenarios.sql` | java-diag 6 大场景实例链（CPU高/FullGC/OOM/内存泄漏/线程池耗尽/ClassLoader泄漏 + 域树 + 关系索引） |

全部 seed 幂等（ON CONFLICT DO NOTHING），重复应用安全。注意：`07` 依赖 `06`，且必须按序应用（`init-seed.sh` 已按文件名排序自动处理）；若跳过应用层激活直接插 `relation_instance`，末尾已含 `entity_relation_index` 双向索引同步（compute-engine 图遍历依赖）。

## 常用命令

```bash
# 查看日志
docker compose logs -f metaforge-boot

# 停止（保留数据卷）
docker compose down

# 停止并清空数据（重置数据库）
docker compose down -v

# 单独重启 boot（数据保留）
docker compose restart metaforge-boot

# 进入容器
docker exec -it metaforge-boot bash
```

## 故障排查

| 现象 | 处理 |
|------|------|
| boot 启动失败 | `docker compose logs metaforge-boot` 看异常；确认 postgres 健康 |
| 端口占用 | 修改 docker-compose.yml 的 `ports`（如 `8081:8080`） |
| seed 未应用 | boot 日志显示 "[4/5] 应用 seed"；可 `docker compose exec metaforge-boot bash` 手动 `psql -f /app/seed/*.sql` |
| 首次构建慢 | 正常（Maven 拉取依赖）；之后增量快 |
