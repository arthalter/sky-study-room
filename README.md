# Sky Study Room

Sky Study Room 是一个自习室预约系统，后端基于 Spring Boot，前端为轻量级用户端/管理端静态页面，使用 MySQL 持久化数据，并预留 Redis 能力。项目同时提供 Docker Compose 部署配置。

## 项目结构

```text
sky-study-room
├── sky-server                 # Spring Boot 后端服务
└── sky-study-room-deploy      # Docker Compose、Nginx、前端页面、MySQL 初始化 SQL
```

## 环境要求

- JDK 17
- Maven 3.9+
- Docker 和 Docker Compose

检查本机 Java/Maven 环境：

```bash
cd sky-server
mvn -version
```

Maven 输出中应显示 Java 17。

## 后端构建

```bash
cd sky-server
mvn clean test
mvn package
```

打包命令会生成：

```text
sky-server/target/sky-server-1.0-SNAPSHOT.jar
```

## 本地启动

先用 Docker 启动 MySQL、Redis 和 Nginx，然后在本机运行后端服务：

```bash
cd sky-study-room-deploy
docker compose -f docker-compose.dev.yml up -d
```

再启动后端：

```bash
cd ../sky-server
JWT_SECRET=sky-study-room-dev-secret-key-2026 mvn spring-boot:run
```

默认本地后端配置位于 `sky-server/src/main/resources/application.yml`：

- 后端端口：`8080`
- MySQL 地址：`jdbc:mysql://localhost:3306/sky_study`
- MySQL 用户名：`root`
- MySQL 密码：`root`
- Redis 主机：`localhost`
- Redis 端口：`6379`
- JWT 开发密钥：`sky-study-room-dev-secret-key-2026`

开发模式下，Nginx 容器会把 `/api/` 代理到 `http://host.docker.internal:8080`。

## Docker 启动

先构建后端 jar：

```bash
cd sky-server
mvn package
```

启动完整服务栈：

```bash
cd ../sky-study-room-deploy
docker compose up -d --build
```

完整服务栈包含：

- `sky-mysql`：MySQL 8.4
- `sky-redis`：Redis 7.4
- `sky-server`：Spring Boot 后端服务
- `sky-nginx`：静态前端和 API 代理

停止服务：

```bash
docker compose down
```

同时删除 MySQL 和 Redis 持久化数据：

```bash
docker compose down -v
```

## 访问地址

完整 Docker 启动后可访问：

- 用户端：`http://localhost:8088/`
- 管理端：`http://localhost:8088/admin/`
- 后端 API：`http://localhost:8080/api`
- Nginx API 代理：`http://localhost:8088/api`
- API 测试接口：`http://localhost:8080/api/test`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`

## 默认账号

MySQL 初始化脚本位于 `sky-study-room-deploy/mysql/init/001-init.sql`。

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `123456` |
| 普通用户 | `student` | `123456` |

初始化数据中的密码以 MD5 哈希形式存储。

## 常见问题

### Maven 使用了错误的 Java 版本

如果编译时报 Lombok 生成的方法或 `log` 字段找不到，先确认 Maven 使用的是 Java 17：

```bash
cd sky-server
mvn -version
```

期望输出中包含 Java `17.x`。项目已在 `sky-server/pom.xml` 中配置 `maven-compiler-plugin` 和 Lombok 注解处理。

### 端口被占用

默认端口如下：

- 后端服务：`8080`
- Nginx 前端入口：`8088`
- MySQL 容器内部端口：`3306`
- Redis 容器内部端口：`6379`

如果端口冲突，可以修改 Docker Compose 端口映射，或停止占用端口的进程。

### MySQL 数据没有重新初始化

MySQL 初始化 SQL 只会在数据卷首次创建时执行。如果需要重新按初始化脚本创建数据库：

```bash
cd sky-study-room-deploy
docker compose down -v
docker compose up -d --build
```

注意：这会删除已有的 MySQL 和 Redis 持久化数据。

### 后端无法连接 MySQL 或 Redis

本地启动模式下，确认 `docker-compose.dev.yml` 中的服务已经启动并健康：

```bash
cd sky-study-room-deploy
docker compose -f docker-compose.dev.yml ps
```

完整 Docker 启动模式下，后端会使用 `docker-compose.yml` 中的服务名 `mysql` 和 `redis` 连接依赖服务。

### 登录时报 JWT 密钥长度不足

项目使用 HS256 签发 JWT，密钥长度至少需要 32 字节。本地开发可以通过环境变量指定：

```bash
JWT_SECRET=sky-study-room-dev-secret-key-2026 mvn spring-boot:run
```

如果不显式指定，`application.yml` 会使用同样满足长度要求的开发默认值。生产环境应替换为独立的强随机密钥。

### 前端 API 请求失败

本地后端开发模式使用：

```bash
docker compose -f docker-compose.dev.yml up -d
```

该 Nginx 配置会把 `/api/` 代理到宿主机 `8080` 端口上的后端服务。

完整 Docker 启动模式使用：

```bash
docker compose up -d --build
```

该 Nginx 配置会把 `/api/` 代理到 `sky-server` 容器。
