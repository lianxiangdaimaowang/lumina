# Lumina 服务端

## 最近更新

### 添加了首页支持 (2024-07-06)

1. 添加了对 `http://localhost:8081` 请求的支持，返回一个基本的 HTML 页面
2. 创建了静态资源目录 `src/main/resources/static` 和首页文件 `index.html`
3. 添加了 WebMvcConfig 配置类以支持静态资源和视图控制器
4. 新增了 `/api/server/info` 端点，提供服务器状态和可用 API 信息

## 系统要求

- Java 11 或更高版本
- MySQL 8.0 或更高版本

## 配置

应用程序配置位于 `src/main/resources/application.yml` 中。

## 部署

### 构建项目

```bash
./mvnw clean package
```

### 运行项目

```bash
java -jar target/lumina-0.0.1-SNAPSHOT.jar
```

### 环境变量

- `LUMINA_DB_PASSWORD`: 数据库密码
- `JWT_SECRET`: JWT 令牌密钥

## API 端点

- `/` - 首页 HTML
- `/api/server/info` - 服务器信息
- `/api/health` - 健康检查
- `/api/auth/*` - 认证相关端点 