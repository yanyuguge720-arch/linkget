package com.linkget.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.linkget.ui.theme.Muted

enum class LegalKind(val title: String) {
    PRIVACY("隐私政策"),
    DISCLAIMER("免责声明")
}

@Composable
fun LegalDialog(kind: LegalKind, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color(0xFF0F172A))
                }
                Text(
                    text = kind.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 14.dp)
                )
            }
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (kind) {
                        LegalKind.PRIVACY -> PRIVACY_TEXT
                        LegalKind.DISCLAIMER -> DISCLAIMER_TEXT
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 23.sp,
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 12.dp)
                )
            }
            Text(
                text = "生效日期：2026 年 11 月 12 日",
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
            )
        }
    }
}

private const val PRIVACY_TEXT = """链取隐私政策

链取是一款本地优先的多平台媒体解析与下载工具。我们尽量不收集、不上传你的个人数据。

一、我们处理的信息

1. 你主动粘贴的分享链接：默认仅在本机解析视频、音频、图集。若你开启「实况图云端补全」，且本机接口拿不到实况动态，链接会发送到第三方解析服务 video.zacao.top 用于补全；关闭该开关后不会发送。
2. 解析结果与下载记录：保存在本机数据库中，用于展示结果、历史和本地文件。
3. 设置项：保存目录、默认清晰度、并发数、仅 Wi-Fi、剪贴板识别等，保存在本机。
4. B 站登录 Cookie：只有你在「B 站 1080P 登录」中主动登录后才会保存，用于请求 1080P 播放地址；Cookie 仅保存在本机应用私有目录，不会上传。
5. 缓存与临时文件：预览缩略图、分片下载临时文件，保存在本机，可在系统设置中清理。

二、权限说明

1. 网络权限：访问你粘贴的链接及对应平台的媒体 CDN，完成解析和下载。
2. 通知权限：显示下载进度和完成提醒，可随时在系统设置中关闭。
3. 媒体库权限：把下载完成的文件写入 Movies、Pictures、Music 等公共目录，便于相册和播放器识别。
4. 剪贴板：只有在你打开或切回链取、且「剪贴板自动识别」开启时，才读取一次剪贴板中的文本，用于识别分享链接；不会在后台持续读取。

三、数据存储与删除

1. 解析历史、下载任务、设置保存在本机；卸载应用会删除应用私有数据。
2. 下载完成的媒体文件保存在你设置的公共媒体目录，卸载应用不会自动删除，请在「我的 → 本地文件」或系统文件管理中手动删除。
3. B 站 Cookie 可在「我的 → B 站 1080P 登录 → 退出登录」中清除。

四、第三方平台

链取会直接访问抖音、哔哩哔哩、小红书、快手、今日头条等平台的公开页面或接口。你的网络请求会按平台规则发送到对应平台，平台可能按自己的隐私政策处理请求信息。链取不控制第三方平台的数据处理行为。

「实况图云端补全」是可关闭的开关，默认开启。开启时，仅当本地接口拿不到实况动态，才会把当前分享链接发送到 video.zacao.top 以获取实况视频地址；该服务由第三方运营，请阅读其隐私政策。

五、未成年人

链取不面向未成年人单独提供服务。未成年人应在监护人指导下使用，并遵守相关平台和法律法规的要求。

六、政策更新

我们可能随功能变化更新本政策。更新后的政策会在应用内展示，继续使用即表示你接受更新后的内容。

七、联系我们

如对本政策有疑问，可通过 QQ 群 35369972 联系开发者。"""

private const val DISCLAIMER_TEXT = """链取免责声明

一、用途限制

链取仅供个人学习、研究和自用备份。你不得将本工具用于商业用途、批量抓取、二次分发、售卖或传播侵权内容。

二、版权声明

通过链取下载的视频、音频、图片、封面、音乐等媒体资源，版权归原平台、原作者或相关权利人所有。你应当在下载后尽快用于个人备份，不得去除权利标识，不得侵犯著作权、肖像权、隐私权等合法权益。

三、禁止行为

你不得使用链取下载、传播以下内容：

1. 未获得授权的付费内容、会员内容或受 DRM 保护的内容；
2. 违法、暴力、色情、赌博、诈骗、侵害他人隐私或其他违反法律法规的内容；
3. 侵犯他人著作权、商标权、肖像权、名誉权等合法权益的内容；
4. 平台明确禁止下载、保存或传播的内容。

四、平台条款

使用链取访问第三方平台时，你仍需遵守对应平台的用户协议、robots 协议、版权规则和账号规则。链取不绕过付费墙、DRM 或平台访问控制。

五、可用性

链取依赖第三方平台公开页面和接口，平台改版、风控、地域限制或网络波动都可能导致解析失败。我们不保证任何链接都能解析成功，也不保证下载地址长期有效。

六、责任限制

因使用或无法使用链取造成的任何直接或间接损失，由你自行承担。开发者不对下载内容的合法性、完整性、可用性作任何明示或默示担保。

七、侵权处理

如果你是权利人，认为链取被用于侵犯你的合法权益，请通过 QQ 群 35369972 联系开发者，并提供权属证明和具体链接，我们会尽快处理。

使用链取即表示你已阅读并同意本声明。"""