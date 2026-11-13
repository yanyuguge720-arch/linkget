package com.linkget.data.parser

/**
 * A single way of reading a Douyin page. Strategies are cheap, pure and replaceable: when the
 * platform changes its payload shape, add a new strategy instead of editing network code.
 */
interface ParseStrategy {
    val name: String

    /** Returns null when this strategy does not recognise the page. */
    fun parse(html: String, sourceUrl: String): Aweme?
}
