# ============================================================================
# MetaForge 认知服务 —— 快速测试镜像（多阶段自包含构建）
#
# 构建: docker compose build（或 docker build -f Dockerfile ..）
# 运行: 由 docker-compose.yml 编排（postgres + boot + seed 初始化）
#
# 阶段 1 (build): Maven 构建整个 metaforge-parent（含 boot 及全部依赖模块）
# 阶段 2 (runtime): JRE 运行 boot，安装 psql 用于应用 seed
# ============================================================================

# ---------- 阶段 1: 构建 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /repo

# 拷贝 metaforge-parent 全部源码（多模块 reactor：boot + agent-cognition + graph + metadata + compute-engine 等）
COPY metaforge-parent/ /repo/

# 跳过测试打包全部模块（boot 的 fat jar 依赖 reactor 内其他模块）
RUN mvn -q -DskipTests install

# ---------- 阶段 2: 运行 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# boot fat jar
COPY --from=build /repo/metaforge-boot/target/metaforge-boot-1.0.0-SNAPSHOT.jar /app/app.jar

# psql：用于 boot 启动后应用 seed
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client curl \
    && rm -rf /var/lib/apt/lists/*

# seed + 启动编排脚本
COPY docker/init-seed.sh /app/init-seed.sh
COPY docker/seed/ /app/seed/
RUN chmod +x /app/init-seed.sh

EXPOSE 8080
ENTRYPOINT ["/app/init-seed.sh"]
