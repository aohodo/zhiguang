# zhiguang

知光前后端项目，开发环境通过 Docker Compose 启动。MySQL、Redis 和 Elasticsearch 的持久化数据保存在本项目的 `.docker-data` 目录中。

## 本地启动

1. 复制 `.env.example` 为 `.env`，填写模型及 OSS 配置。
2. 在 `backend/zhiguang_be-main/src/main/resources/keys` 下准备匹配的 RSA 私钥 `private.pem` 和公钥 `public.pem`。私钥不会提交到 Git。
3. 启动服务：

   ```bash
   docker compose up -d --build
   ```

4. 访问前端：<http://localhost:5173>。

停止服务：

```bash
docker compose down
```
