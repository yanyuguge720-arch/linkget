package com.linkget.data.parser

import java.io.IOException

/** Every failure mode the UI can explain to a human. */
enum class ParseError(val userMessage: String) {
    EMPTY_INPUT("先粘贴一条分享链接"),
    NO_LINK("没有找到链接，检查一下分享文案"),
    UNSUPPORTED_HOST("这不像是一条可以解析的分享链接"),
    UNSUPPORTED_PLATFORM("该平台暂不支持，目前支持抖音、B 站、小红书、快手和今日头条"),
    NETWORK("网络连接失败，请检查网络后重试"),
    LINK_EXPIRED("链接可能已失效，请回原平台重新分享"),
    CONTENT_UNAVAILABLE("这条内容暂时取不到，可能已删除或仅自己可见"),
    UNSUPPORTED_CONTENT("暂不支持这种内容类型"),
    PARSE_FAILED("解析失败，平台页面可能改版了，请稍后重试"),
    TIMEOUT("解析超时，请检查网络后重试"),
    NO_RESOURCES("没有解析到可下载的资源，换一条链接试试"),
    API_ERROR("解析服务暂时不可用，请稍后重试")
}

class ParseException(
    val reason: ParseError,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message ?: reason.userMessage, cause)

fun Throwable.readableMessage(): String = when (this) {
    is ParseException -> reason.userMessage
    is IOException -> ParseError.NETWORK.userMessage
    else -> "出了点问题，请稍后重试"
}
