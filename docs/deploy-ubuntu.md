# Ubuntu 部署步骤

下面假设你已经在 MySQL 里创建了：

- `info_collection_dev`
- `info_collection_prod`
- `info_collection_dev_user`
- `info_collection_prod_user`

## 1. 安装 Java 21

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless
java -version
```

## 2. 创建应用目录

```bash
sudo mkdir -p /opt/info-collection
sudo chown -R ubuntu:ubuntu /opt/info-collection
```

如果你的登录用户不是 `ubuntu`，把上面的 `ubuntu:ubuntu` 换成你的用户名。

## 3. 上传 jar

把本地文件上传到服务器：

```text
target/info-collection-0.1.0.jar
```

服务器目标位置：

```text
/opt/info-collection/info-collection.jar
```

MobaXterm 左侧 SFTP 面板可以直接拖拽上传。

## 4. 创建生产环境变量

```bash
sudo nano /etc/info-collection.env
```

填入：

```bash
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=info_collection_prod
DB_USER=info_collection_prod_user
DB_PASSWORD=你的PROD数据库密码
ADMIN_USER=admin
ADMIN_PASSWORD=请换成后台强密码
SERVER_PORT=8080
DEFAULT_EXAM_YEAR=2026
DEEPSEEK_ENABLED=true
DEEPSEEK_API_KEY=你的DeepSeekKey
DEEPSEEK_API_URL=https://api.deepseek.com/chat/completions
DEEPSEEK_MODEL=deepseek-v4-flash
```

保存后限制权限：

```bash
sudo chmod 600 /etc/info-collection.env
```

## 5. 创建 systemd 服务

```bash
sudo nano /etc/systemd/system/info-collection.service
```

填入：

```ini
[Unit]
Description=Info Collection Spring Boot App
After=network.target mysql.service

[Service]
User=ubuntu
WorkingDirectory=/opt/info-collection
EnvironmentFile=/etc/info-collection.env
ExecStart=/usr/bin/java -jar /opt/info-collection/info-collection.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

如果你的服务器用户名不是 `ubuntu`，把 `User=ubuntu` 改掉。

## 6. 启动服务

```bash
sudo systemctl daemon-reload
sudo systemctl enable info-collection
sudo systemctl start info-collection
```

查看状态：

```bash
sudo systemctl status info-collection --no-pager
```

看日志：

```bash
journalctl -u info-collection -f
```

## 7. 访问后台

浏览器打开：

```text
http://你的服务器公网IP:8080/admin
```

登录账号密码来自：

```bash
ADMIN_USER
ADMIN_PASSWORD
```

## 8. 腾讯云安全组

如果你直接用 `8080` 访问，需要在腾讯云安全组放行 TCP `8080`。

生产环境更推荐用 Nginx 反代到 `8080`，只对外开放 `80/443`。
