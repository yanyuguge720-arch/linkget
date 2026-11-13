# 第三方组件与许可证

链取使用的主要开源组件：

| 组件 | 许可证 | 用途 |
|---|---|---|
| AndroidX / Jetpack Compose / Media3 / Room | Apache-2.0 | Android UI、媒体播放、数据库 |
| Kotlin / Kotlin Coroutines | Apache-2.0 | 开发语言与协程 |
| OkHttp | Apache-2.0 | 网络请求 |
| FastAPI / Uvicorn / yt-dlp（可选 server） | MIT / BSD / Unlicense | 可选解析服务 |

本项目不包含 LAME MP3 编码器；音频原声默认保存为 M4A（复制 AAC 音轨）。

第三方平台（抖音、哔哩哔哩、小红书、快手、今日头条）的页面、接口与媒体内容归各自平台及权利人所有。本项目仅用于个人备份，不绕过付费墙、DRM 或访问控制。

完整依赖版本见 `gradle` 配置与 `server/requirements.txt`。
