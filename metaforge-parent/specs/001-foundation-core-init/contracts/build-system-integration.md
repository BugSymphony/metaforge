# Build System Integration: 构建系统集成规范

本文档定义业务 BC 接入 MetaForge Maven 构建系统的完整规范，包括 POM 模板、注册规则、依赖声明约束和构建校验配置。

---

## 1. BC POM 标准模板

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- ===== 1. 继承 metaforge-parent ===== -->
    <parent>
        <groupId>com.metaforge</groupId>
        <artifactId>metaforge-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <!-- ===== 2. GAV 声明 ===== -->
    <!-- artifactId 无强制命名约定，BC 自行决定 -->
    <artifactId>bc-xxxx</artifactId>
    <name>Business Context: XXXX</name>
    <description>业务 BC 描述</description>

    <!-- ===== 3. 依赖声明 ===== -->
    <!-- 规则: 仅声明 metaforge-framework → 禁止声明 <dependencyManagement> → 禁止声明版本属性 -->
    <dependencies>
        <!-- foundation-core 框架工具层 (传递包含 common + Spring/JPA/Web/Cache) -->
        <dependency>
            <groupId>com.metaforge</groupId>
            <artifactId>metaforge-framework</artifactId>
        </dependency>

        <!-- ===== 允许的框架依赖白名单 ===== -->
        <!-- 当 metaforge-framework 未传递满足时，可直接声明以下白名单依赖 -->
        <!-- 所有版本由 metaforge-parent BOM 统一管控，禁止显式写 <version> -->

        <!-- 数据库 (如需直接使用 JDBC) -->
        <!-- <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency> -->

        <!-- 测试 (test scope) -->
        <dependency>
            <groupId>com.metaforge</groupId>
            <artifactId>metaforge-framework</artifactId>
            <type>test-jar</type>
            <scope>test</scope>
        </dependency>

        <!-- 仅 test scope 允许引入额外依赖 -->
    </dependencies>

    <!-- ===== 严禁: <dependencyManagement> 声明 ===== -->
    <!-- 禁止在 BC POM 中声明 <dependencyManagement> -->

    <!-- ===== 允许: 自定义 properties（仅限框架版本无关属性）===== -->
    <properties>
        <!-- ✅ 允许: 构建参数、自定义属性 -->
        <my-custom-build-arg>value</my-custom-build-arg>

        <!-- ❌ 禁止: 覆盖父 POM 已定义的版本属性 -->
        <!-- <spring-boot.version>3.4.3</spring-boot.version> -->
    </properties>
    <!-- ===== 禁止再次声明版本号，统一从 parent 继承 ===== -->
</project>
```

---

## 2. BC 注册规则（metaforge-boot/pom.xml）

### 2.1 注册声明

在 `metaforge-boot/pom.xml` 中添加 BC 为 `<dependency>`：

```xml
<!-- metaforge-boot/pom.xml -->
<dependencies>
    <!-- ===== foundation-core 内部模块 ===== -->
    <dependency>
        <groupId>com.metaforge</groupId>
        <artifactId>metaforge-server</artifactId>
    </dependency>

    <!-- ===== 业务 BC 模块注册 ===== -->
    <!-- ⬇️ 新增 BC 在此注册 ⬇️ -->
    <dependency>
        <groupId>com.metaforge</groupId>
        <artifactId>bc-sample</artifactId>
        <!-- 默认 scope: compile（参与编译期类型检查，打包到最终产物） -->
    </dependency>

    <!-- <dependency> -->
    <!--     <groupId>com.metaforge</groupId> -->
    <!--     <artifactId>bc-user</artifactId> -->
    <!-- </dependency> -->
</dependencies>
```

### 2.2 注册规则

| 规则 | 说明 |
|------|------|
| Scope | 默认 `compile`（Maven 默认值，无需显式声明） |
| 排除 | 不得使用 `<exclusions>` 排除 BC 的传递依赖 |
| 顺序 | 无强制顺序要求 |

---

## 3. 依赖声明规则

### 3.1 白名单（允许直接声明）

以下依赖可以在 BC 的 `pom.xml` 中直接声明（版本 BOM 管控，禁止写 version）：

| GroupId | ArtifactId | Scope | 说明 |
|---------|-----------|-------|------|
| `com.metaforge` | `metaforge-framework` | compile | 框架工具层（唯一必需依赖） |
| `org.springframework.boot` | `spring-boot-starter-data-jpa` | compile | JPA（如需） |
| `org.springframework.boot` | `spring-boot-starter-validation` | compile | 校验（单独使用） |
| `com.metaforge` | `metaforge-framework` | test | 测试基类（test-jar） |
| `org.springframework.boot` | `spring-boot-starter-test` | test | Spring Boot 测试 |
| `org.testcontainers` | `junit-jupiter` | test | TestContainers（如需自定义） |

### 3.2 黑名单（禁止直接/传递依赖）

| GroupId | ArtifactId | 原因 |
|---------|-----------|------|
| `com.metaforge` | `metaforge-boot` | 启动模块，禁止被依赖（Enforcer 反向依赖规则） |
| `com.metaforge` | `metaforge-server` | 平台级能力模块，BC 不应直接依赖 |
| `org.springframework.boot` | `spring-boot-starter-web` | 已由 metaforge-framework 传递提供 |

### 3.3 BOM 统一管控

所有依赖版本由 `metaforge-parent` 的 `<dependencyManagement>` 统一管理。BC 禁止：
- 使用 `<version>` 标签显式声明版本号
- 通过 `<properties>` 覆盖 BOM 管理的版本属性
- 声明 `<dependencyManagement>` 段落

---

## 4. Maven Enforcer 构建校验规则

在 `metaforge-parent/pom.xml` 中配置（一次性，所有下游 BC 继承生效）：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-enforcer-plugin</artifactId>
            <version>3.5.0</version>
            <executions>
                <execution>
                    <id>enforce-build-rules</id>
                    <goals><goal>enforce</goal></goals>
                    <configuration>
                        <rules>
                            <!-- 1. 版本收敛 -->
                            <requireUpperBoundDeps />

                            <!-- 2. 禁止声明 dependencyManagement -->
                            <banDependencyManagementScope/>
                            <!-- 但仅对 BC 模块生效：common/framework/server/boot/BC -->
                            <bannedDependencies>
                                <excludes>
                                    <!-- 3. 禁止任何模块依赖 metaforge-boot -->
                                    <exclude>com.metaforge:metaforge-boot</exclude>
                                </excludes>
                            </bannedDependencies>

                            <!-- 4. 禁止 BC 覆盖关键版本属性 -->
                            <requireProperty>
                                <property>spring-boot.version</property>
                                <message>禁止覆盖 spring-boot.version，版本由 metaforge-parent BOM 统一管理</message>
                            </requireProperty>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**校验触发时机**: `mvn validate`（Maven default lifecycle 首个阶段自动执行）

**CI 失败条件**: 任何 Enforcer 规则触发 → 构建立即失败

---

## 5. flatten-maven-plugin 配置

```xml
<!-- metaforge-parent/pom.xml -->
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>flatten-maven-plugin</artifactId>
            <version>1.6.0</version>
            <configuration>
                <updatePomFile>true</updatePomFile>
                <flattenMode>oss</flattenMode>
            </configuration>
            <executions>
                <execution>
                    <id>flatten</id>
                    <phase>process-resources</phase>
                    <goals><goal>flatten</goal></goals>
                </execution>
                <execution>
                    <id>flatten-clean</id>
                    <phase>clean</phase>
                    <goals><goal>clean</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**效果**: `mvn deploy` 时 `.flattened-pom.xml` 中所有 `${revision}` 变量已解析为实际版本号，CI/CD 友好。

---

## 6. 多模块完整 reactor 结构

```xml
<!-- metaforge-parent/pom.xml → <modules> -->
<modules>
    <!-- foundation-core 内部模块 -->
    <module>metaforge-common</module>
    <module>metaforge-framework</module>
    <module>metaforge-server</module>
    <module>metaforge-boot</module>

    <!-- 示例模块 -->
    <module>bc-sample</module>

    <!-- ===== 业务 BC 模块（平铺在 metaforge-parent 根目录） ===== -->
    <!-- 新增 BC 在此追加 <module> -->
</modules>
```

**构建命令**:
```bash
# 全量编译
mvn clean install -pl metaforge-boot -am

# 仅编译单个 BC
mvn clean install -pl bc-sample -am

# 启动应用
mvn spring-boot:run -pl metaforge-boot
```
