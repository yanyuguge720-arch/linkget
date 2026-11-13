package com.linkget.data.parser

import com.linkget.data.model.ParsedWork
import com.linkget.data.model.Platform
import com.linkget.data.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/** Chooses the backend API when configured, then the on-device parsers. */
class ParseCoordinator(
    private val settings: SettingsRepository,
    private val douyin: DouyinWorkParser,
    private val bilibili: BilibiliWorkParser,
    private val xiaohongshu: XiaohongshuWorkParser,
    private val kuaishou: KuaishouWorkParser,
    private val toutiao: ToutiaoWorkParser,
    private val api: ApiWorkParser
) {

    suspend fun parse(text: String): ParsedWork {
        if (text.isBlank()) throw ParseException(ParseError.EMPTY_INPUT)
        val url = LinkText.extractUrl(text) ?: throw ParseException(ParseError.NO_LINK)
        val platform = LinkText.platformOf(url)
        val apiBase = settings.state.value.apiBase.trim()

        var apiFailure: Throwable? = null
        if (apiBase.isNotBlank()) {
            try {
                return withTimeout(TIMEOUT_MS) { api.parse(text, apiBase) }
            } catch (e: TimeoutCancellationException) {
                apiFailure = ParseException(ParseError.TIMEOUT)
            } catch (e: ParseException) {
                apiFailure = e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                apiFailure = ParseException(ParseError.API_ERROR, e.message, e)
            }
        }

        if (platform == Platform.GENERIC) {
            throw apiFailure ?: ParseException(ParseError.UNSUPPORTED_PLATFORM)
        }

        try {
            return withTimeout(TIMEOUT_MS) {
                when (platform) {
                    Platform.DOUYIN -> douyin.parse(text)
                    Platform.BILIBILI -> bilibili.parse(text)
                    Platform.XIAOHONGSHU -> xiaohongshu.parse(text)
                    Platform.KUAISHOU -> kuaishou.parse(text)
                    Platform.TOUTIAO -> toutiao.parse(text)
                    else -> throw ParseException(ParseError.UNSUPPORTED_PLATFORM)
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw ParseException(ParseError.TIMEOUT)
        } catch (e: ParseException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ParseException(ParseError.PARSE_FAILED, e.message, e)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 25_000L
    }
}
