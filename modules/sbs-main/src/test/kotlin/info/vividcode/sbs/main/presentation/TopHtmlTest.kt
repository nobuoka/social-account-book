package info.vividcode.sbs.main.presentation

import info.vividcode.sbs.main.core.domain.User
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Test
import java.io.CharArrayWriter
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

internal class TopHtmlTest {

    @Test
    internal fun withoutActor(): Unit = runBlocking {
        val written = CharArrayWriter().use { writer ->
            withHtmlDoctype(topHtml(null, "/login", "/logout")).invoke(writer)
            writer.toString()
        }

        val expectedHtmlUrl =
            this::class.java.classLoader.getResource("sbs/presentation/test/html/top-without-actor.html")
        val expectedHtmlString = expectedHtmlUrl.openStream().use { it.readBytes().toString(StandardCharsets.UTF_8) }
        assertEquals(expectedHtmlString, written)
    }

    @Test
    internal fun withActor(): Unit = runBlocking {
        val written = CharArrayWriter().use { writer ->
            withHtmlDoctype(topHtml(User(1001, "test-user"), "/login", "/logout")).invoke(writer)
            writer.toString()
        }

        val expectedHtmlUrl =
            this::class.java.classLoader.getResource("sbs/presentation/test/html/top-with-actor.html")
        val expectedHtmlString = expectedHtmlUrl.openStream().use { it.readBytes().toString(StandardCharsets.UTF_8) }
        assertEquals(expectedHtmlString, written)
    }

}
