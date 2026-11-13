# LinkGet 解析服务（可选）

App 自带抖音 / B 站本地解析，不依赖本服务。需要 1080P、更多站点或统一维护解析规则时，再部署这个后端：

```bash
cd server
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app:app --host 0.0.0.0 --port 8000
```

接口：

```http
POST /api/parse
Content-Type: application/json

{"url":"https://www.bilibili.com/video/BV..."}
```

返回结构与 PRD 一致：`code / msg / data{title,author,cover,resource_list[]}`。
在 App 的「我的 → 解析服务」填入服务地址（例如 `http://192.168.1.10:8000`），下次解析会优先走服务，失败自动回退本机解析。

注意：

- B 站 1080P 需要登录 cookie；可在服务器上给 yt-dlp 配置 cookies（`cookiesfrombrowser` 或 `cookiefile`），不要提交到仓库。
- 服务只做解析，不下发代理流量；直链仍由手机下载。部分平台直链有防盗链/时效，需尽快下载。
- 仅用于个人备份，遵守平台条款与著作权法律。
