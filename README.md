# 链取（LinkGet）· 多平台音视频解析下载

> 仅限个人自用备份。下载内容的版权归原平台与著作权人所有，禁止商用、二次分发或传播侵权内容。

链取（LinkGet）是一个本地优先的 Android 链接解析工具：粘贴抖音 / B 站 / 小红书 / 快手 / 今日头条分享链接，解析视频、音频、图集，预览后勾选下载到手机媒体库。

## 当前版本

- 版本：0.8.5（versionCode 25）
- 名称：链取（LinkGet）；新图标为蓝色渐变 + 白色链接符号
- 安装包：GitHub Releases（或自行构建）
- 应用内「我的 → 隐私政策 / 免责声明」可查看全文；仓库内也有 [隐私政策](PRIVACY.md) 与 [免责声明](DISCLAIMER.md)
- 意见反馈：QQ 群 `35369972`
- 软件动态：应用内「我的 → 软件动态」查看抖音二维码，可保存到相册后扫码关注「岩羽朝歌」（抖音号 zhaogeyanyu）
- 实况图补全：部分抖音笔记的公开 slidesinfo 接口会被过滤；开启「实况图云端补全」后会用 video.zacao.top 补全实况短片，再合成为安卓动态照片。默认开启，可在设置关闭，关闭后不发送链接到第三方。
- 云端补全时保留本地解析出的背景音乐，不会因补全实况而丢失 BGM。
- 图集图片上限提高到 300；云端补全结果比本地短时按图片合并，不会因为云端少返回而丢本地图片。
- 抖音 slides/note 优先使用免签名详情接口 `api.amemv.com/aweme/v1/aweme/detail/`，一次返回完整 `images[]` 与每张图的 `video`；实测 33 图链接得到 图片33 / 实况33 / 音频00:20。
- 最低系统：Android 10（API 29）
- 技术栈：Kotlin + Jetpack Compose + Room + OkHttp + Media3 + MediaStore

> 为什么没有用 UniApp：本机没有 HBuilderX / UniApp 离线安卓 SDK，而 MVP 的后台下载、断点续传、MediaStore 保存、音视频流播放和 MP4 合并都更适合原生实现。产品交互与 PRD 保持一致。

## 已实现

- 4 个底部 Tab：解析、下载、历史、我的；横向滑动 + 毛玻璃悬浮导航
- 解析首页：链接输入、粘贴 / 清空、25 秒超时、错误提示、最近解析、平台说明
- 剪贴板检测：默认关闭；开启后回到前台检测并询问是否填入（Android 13 焦点兼容）
- 抖音：本地解析视频 / 图集 / 封面；实况图同时解析全部封面图片、独立“实况动态”MP4 短片和背景音乐；可兼容 `images[].video` 与顶层 `live_photo.image/live_photo.video` 两种结构；可从视频额外提取 M4A 原声
- B 站：新增 1080P —— 在「我的 → B 站 1080P 登录」用官方网页登录后，解析会带 Cookie；1080P DASH 自动下载音频并合并 MP4（真机实测 66.5MB 下载、合并后 75.2MB）
- 小红书：解析 `xhslink.com` / `xiaohongshu.com` 分享页的 `__INITIAL_STATE__`，支持视频与图文；需要带 `xsec_token` 的完整分享链接
- 快手：解析 `v.kuaishou.com` / 快手作品页 SSR 状态，支持短视频
- 今日头条：解析 `m.toutiao.com` / `toutiao.com` 视频页的 `RENDER_DATA`，用 `playAuthTokenV2` 调 VOD `GetPlayInfo`，支持 360P / 480P / 720P / 1080P（以视频实际提供为准）；真机实测 720P 6.4MB 下载成功
- B 站：720P / 480P / 360P 合并流；DASH 480P 等需要合并的清晰度自动下载音频并合成为 MP4；独立音频轨
- 结果页：按视频 / 音频 / 图片分区，清晰度、时长、大小、自动合并标记，勾选与全选；实况图有“抖音实况图”标识，下载时会把封面与动态短片合成为 **安卓 Motion Photo（MVIMG_..._MP.jpg）**，小米相册实测显示“动态照片”并可播放；也可单独下载原始 MP4
- 小红书、快手、今日头条在解析页有独立平台标识；请求会带对应 Referer
- 预览：Media3 内置播放器播放视频；音频有独立播放/暂停、进度和时间显示；图集大图左右滑动
- 下载：前台服务保活、并行数 1/2/3、进度 / 速度、暂停 / 继续 / 重试 / 删除、仅 Wi-Fi
- 保存：MediaStore 写入 Movies / Pictures / Music 下的 LinkGet 文件夹，无需存储权限
- 历史：本地 Room，点击重新解析，单条删除 / 一键清空
- 本地文件库：视频 / 音频 / 图片分类，打开、分享、重命名、删除
- 设置：保存文件夹、默认清晰度（1080P / 720P）、并行数、仅 Wi-Fi、剪贴板、解析服务地址
- 可选后端：`server/` 提供 FastAPI + yt-dlp 的 `POST /api/parse`，App 可配置地址并优先调用

## 构建

环境：JDK 17、Android SDK Platform 35、Build-Tools 34。

Android Studio 打开项目后会自动生成 `local.properties`。命令行构建：

```bash
./gradlew :app:assembleDebug
```

本机是 aarch64/proot 时，AAPT2 需要包装：

```bash
./gradlew -Pandroid.aapt2FromMavenOverride=/root/aapt2wrap/aapt2 :app:assembleDebug
```

当前仓库的 release 构建在 aarch64/proot 环境使用过 aapt2 包装，普通 x86_64 开发机不需要：

```bash
./gradlew :app:assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`。

## 测试

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

已覆盖：链接识别、PRD 接口响应映射、抖音页面策略、抖音实况图真实链接（50 图 + 49 个实况 + 背景音乐）、动态壁纸图集链接（31 图 + 01:09 背景音乐）、B 站真实解析（720P / 480P / 360P）、B 站音频真实下载。

## 已知限制

- 抖音 note / 单图作品如果本身只有一张图，仍会显示 1 张；实况图 slides 作品会显示完整图集。
- 小红书外链必须带有效的 `xsec_token`；不带 token 会返回空数据，这是平台限制。
- 快手页面结构/风控变化较快，建议粘贴完整分享文案；部分作品仍可能需要登录 Cookie。
- 今日头条主要支持视频页；纯图文文章会尝试提取正文图片，但不是所有文章都有可下载资源。
- 实况照片输出为安卓 Motion Photo，不是苹果 Live Photo 的 HEIC+MOV 配对格式；App 内已提示安卓/iOS 的差异。
- 已实测小米相册：文件显示“动态照片”标识，右上角有播放按钮。
- 只有原始数据中带 `live_photo` 或 `images[].video` 的作品才有动态片段；纯静态图片图集不会凭空生成动态。
- 普通视频没有独立 BGM 时，可用“同时提取视频原声”。
- B 站 1080P 需要登录 cookie；未配置后端 / cookie 时最高 720P。后端可配置 cookies 后返回 1080P。
- B 站视频 CDN 的 PCDN 节点可能 403；解析器已改用 `platform=html5` 的常规 CDN。
- 媒体直链有时效，解析后应尽快下载；403 / 过期时重新解析。
- 发布到应用商店前需替换签名、应用名、图标，补齐隐私政策与版权投诉渠道。

## 开源协议

本项目以 [MIT License](LICENSE) 开源。第三方组件说明见 [THIRD-PARTY.md](THIRD-PARTY.md)。

## 目录

```
app/                       Android 客户端
server/                    可选 FastAPI + yt-dlp 解析服务
app/src/main/java/com/linkget/
  data/parser/             链接识别、抖音 / B 站 / API 解析
  data/download/           下载队列、断点续传、MP4 合并、音频提取
  data/library/            MediaStore 本地文件库
  ui/                      Compose 页面与组件
```