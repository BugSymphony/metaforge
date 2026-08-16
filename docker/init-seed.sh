#!/usr/bin/env bash
# ============================================================================
# MetaForge boot 启动编排
#   1. 等待 PostgreSQL 就绪
#   2. 后台启动 java -jar（boot 启动自动执行 V4 Flyway 迁移）
#   3. 等待 boot 健康检查通过
#   4. 依次应用 seed（基础 agent 库 + 4 个示例域）
#   5. 保持 boot 前台运行
# ============================================================================

set -e

DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-metaforge}"
DB_PASSWORD="${DB_PASSWORD:-metaforge}"
DB_NAME="${DB_NAME:-metaforge}"
BOOT_URL="${BOOT_URL:-http://localhost:8080}"
SEED_DIR="${SEED_DIR:-/app/seed}"

echo "=============================================="
echo "  MetaForge 认知服务启动编排"
echo "  数据库: $DB_HOST:$DB_PORT/$DB_NAME"
echo "  boot:   $BOOT_URL"
echo "=============================================="

# ---------- 1. 等待 PostgreSQL ----------
echo "[1/5] 等待 PostgreSQL 就绪..."
until PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" >/dev/null 2>&1; do
  echo "  PostgreSQL 未就绪，2 秒后重试..."
  sleep 2
done
echo "  PostgreSQL 就绪 ✓"

# ---------- 2. 后台启动 boot ----------
echo "[2/5] 启动 metaforge-boot（自动执行 V4 Flyway 迁移）..."
java -jar /app/app.jar &
BOOT_PID=$!
echo "  boot PID=$BOOT_PID"

# ---------- 3. 等待 boot 健康 ----------
echo "[3/5] 等待 boot 健康检查..."
until curl -sf "$BOOT_URL/actuator/health" >/dev/null 2>&1; do
  if ! kill -0 "$BOOT_PID" >/dev/null 2>&1; then
    echo "  [错误] boot 进程已退出，启动失败。查看日志: docker logs metaforge-boot"
    exit 1
  fi
  echo "  boot 未就绪，2 秒后重试..."
  sleep 2
done
echo "  boot 就绪 ✓"

# ---------- 4. 应用 seed ----------
echo "[4/5] 应用 seed 数据..."
for f in "$SEED_DIR"/*.sql; do
  [ -f "$f" ] || continue
  echo "  应用 $(basename "$f") ..."
  if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -f "$f" >/dev/null 2>&1; then
    echo "    ✓ $(basename "$f")"
  else
    echo "    ⚠ $(basename "$f") 失败（跳过）"
  fi
done
echo "  seed 应用完成 ✓"

# ---------- 5. 保持前台 ----------
echo "[5/5] 服务运行中。认知接口: POST $BOOT_URL/api/v1/cognition/{templateId}"
wait "$BOOT_PID"
