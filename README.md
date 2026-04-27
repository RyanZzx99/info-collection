# Info Collection

Java/Spring Boot 信息收集系统，支持：

- 按 key 创建独立收集链接
- 表单录入 AP 信息
- 自然语言规则识别并填入字段
- 按 Excel 模板导入 `.xlsx`
- 后台按 key 导出 `.xlsx`

## 本地运行

先准备 MySQL 数据库，例如：

```sql
CREATE DATABASE IF NOT EXISTS info_collection_dev
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

设置环境变量：

```bash
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=info_collection_dev
DB_USER=info_collection_dev_user
DB_PASSWORD=你的DEV密码
ADMIN_USER=admin
ADMIN_PASSWORD=ChangeMe2026#A
```

启动：

```bash
mvn spring-boot:run
```

访问：

```text
http://localhost:8080/admin
```

默认后台账号来自 `ADMIN_USER` / `ADMIN_PASSWORD`。

### 本地通过 SSH 隧道连接云 MySQL

如果 MobaXterm 已经把本地 `3307` 转发到云服务器 `127.0.0.1:3306`，可以使用 `local` 配置：

```bash
DB_PASSWORD=你的DEV密码 mvn spring-boot:run -Dspring-boot.run.profiles=local
```

IntelliJ IDEA 中：

- `有效配置文件` 填 `local`
- `环境变量` 至少填 `DB_PASSWORD=你的DEV密码`
- 后台账号默认是 `admin`
- 后台密码默认是 `ChangeMe2026#A`

## DeepSeek AI 自然语言识别

不配置 `DEEPSEEK_API_KEY` 时，系统会自动使用内置规则识别。

启用 DeepSeek：

```bash
DEEPSEEK_API_KEY=你的DeepSeekKey
DEEPSEEK_MODEL=deepseek-v4-flash
```

IntelliJ IDEA 本地测试时，在 `环境变量` 中追加：

```text
DEEPSEEK_API_KEY=你的DeepSeekKey
```

可选配置：

```bash
DEEPSEEK_ENABLED=true
DEEPSEEK_API_URL=https://api.deepseek.com/chat/completions
DEEPSEEK_MODEL=deepseek-v4-flash
```

前台自然语言识别会优先调用 DeepSeek；如果 DeepSeek 未配置、超时或返回异常，会回退到规则识别。

## 生产环境变量

```bash
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=info_collection_prod
DB_USER=info_collection_prod_user
DB_PASSWORD=你的PROD密码
ADMIN_USER=admin
ADMIN_PASSWORD=请换成强密码
SERVER_PORT=8080
```
