---
id: foundation-core-build-system-integration
protocol: Library API
version: 1.0.0
owner: foundation-core
description: Maven build system integration specification including BC POM template, module registration rules, dependency constraints, and Enforcer configuration
type: foundation
---

## 1. BC POM Standard Template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- ===== 1. Inherit metaforge-parent ===== -->
    <parent>
        <groupId>com.metaforge</groupId>
        <artifactId>metaforge-parent</artifactId>
        <version>${revision}</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <!-- ===== 2. GAV Declaration ===== -->
    <artifactId>bc-xxxx</artifactId>
    <name>Business Context: XXXX</name>
    <description>Business BC description</description>

    <!-- ===== 3. Dependencies ===== -->
    <dependencies>
        <!-- foundation-core framework layer (transitively provides common + Spring/JPA/Web/Cache) -->
        <dependency>
            <groupId>com.metaforge</groupId>
            <artifactId>metaforge-framework</artifactId>
        </dependency>

        <!-- Whitelist framework dependencies (version managed by BOM, no <version> tag) -->
        <dependency>
            <groupId>com.metaforge</groupId>
            <artifactId>metaforge-framework</artifactId>
            <type>test-jar</type>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- ===== Forbidden: <dependencyManagement> ===== -->

    <!-- ===== Allowed: custom properties (non-version attributes only) ===== -->
    <properties>
        <my-custom-build-arg>value</my-custom-build-arg>
    </properties>
</project>
```

---

## 2. BC Registration Rules (metaforge-boot/pom.xml)

### Registration Declaration

Add BC as `<dependency>` in `metaforge-boot/pom.xml`:

```xml
<dependencies>
    <!-- foundation-core internal modules -->
    <dependency>
        <groupId>com.metaforge</groupId>
        <artifactId>metaforge-server</artifactId>
    </dependency>

    <!-- Business BC module registration -->
    <dependency>
        <groupId>com.metaforge</groupId>
        <artifactId>bc-sample</artifactId>
    </dependency>
</dependencies>
```

### Registration Rules

| Rule | Description |
|------|-------------|
| Scope | Default `compile` |
| Exclusions | Must not use `<exclusions>` on BC transitive dependencies |
| Order | No ordering requirement |

---

## 3. Dependency Declaration Rules

### Whitelist (allowed direct declaration)

| GroupId | ArtifactId | Scope | Description |
|---------|-----------|-------|-------------|
| `com.metaforge` | `metaforge-framework` | compile | Framework layer (only required dependency) |
| `org.springframework.boot` | `spring-boot-starter-data-jpa` | compile | JPA (if needed) |
| `org.springframework.boot` | `spring-boot-starter-validation` | compile | Validation (standalone) |
| `com.metaforge` | `metaforge-framework` | test | Test base classes (test-jar) |
| `org.springframework.boot` | `spring-boot-starter-test` | test | Spring Boot test |
| `org.testcontainers` | `junit-jupiter` | test | TestContainers (if custom) |

### Blacklist (forbidden direct/transitive dependencies)

| GroupId | ArtifactId | Reason |
|---------|-----------|--------|
| `com.metaforge` | `metaforge-boot` | Boot module, must not be depended upon |
| `com.metaforge` | `metaforge-server` | Platform capability module, BCs must not depend directly |
| `org.springframework.boot` | `spring-boot-starter-web` | Transitively provided by metaforge-framework |

### BOM Unified Management

All dependency versions managed by `metaforge-parent` `<dependencyManagement>`. BCs must not:
- Use `<version>` tag to declare versions
- Override BOM-managed version properties via `<properties>`
- Declare `<dependencyManagement>` section

---

## 4. Maven Enforcer Build Validation Rules

Configured in `metaforge-parent/pom.xml` (inherited by all downstream BCs):

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
                            <!-- 1. Version convergence -->
                            <requireUpperBoundDeps />

                            <!-- 2. Forbid dependencyManagement in BCs -->
                            <banDependencyManagementScope/>

                            <!-- 3. Forbid dependency on metaforge-boot -->
                            <bannedDependencies>
                                <excludes>
                                    <exclude>com.metaforge:metaforge-boot</exclude>
                                </excludes>
                            </bannedDependencies>

                            <!-- 4. Forbid BC overriding key version properties -->
                            <requireProperty>
                                <property>spring-boot.version</property>
                                <message>禁止覆盖 spring-boot.version</message>
                            </requireProperty>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Trigger**: `mvn validate` (auto-executes in Maven default lifecycle)
**CI failure**: Any Enforcer rule violation causes immediate build failure.

---

## 5. flatten-maven-plugin Configuration

```xml
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
```

**Effect**: All `${revision}` variables resolved to actual versions in `.flattened-pom.xml` during `mvn deploy`.

---

## 6. Complete Reactor Structure

```xml
<!-- metaforge-parent/pom.xml -> <modules> -->
<modules>
    <!-- foundation-core internal modules -->
    <module>metaforge-common</module>
    <module>metaforge-framework</module>
    <module>metaforge-server</module>
    <module>metaforge-boot</module>

    <!-- Sample module -->
    <module>bc-sample</module>

    <!-- Business BC modules (flat under metaforge-parent/) -->
</modules>
```

**Build commands**:
```bash
# Full compilation
mvn clean install -pl metaforge-boot -am

# Single BC compilation
mvn clean install -pl bc-sample -am

# Start application
mvn spring-boot:run -pl metaforge-boot
```
