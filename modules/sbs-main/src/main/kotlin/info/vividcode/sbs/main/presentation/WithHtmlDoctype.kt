package info.vividcode.sbs.main.presentation

import kotlinx.html.TagConsumer
import kotlinx.html.stream.appendHTML
import java.io.Writer

fun withHtmlDoctype(consume: suspend TagConsumer<*>.() -> Unit): suspend Writer.() -> Unit = {
    write("<!DOCTYPE html>\n")
    appendHTML().consume()
}

fun withHtmlDoctype(consume: TagConsumer<*>.() -> Unit): suspend Writer.() -> Unit = {
    write("<!DOCTYPE html>\n")
    appendHTML().consume()
}
