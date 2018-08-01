package info.vividcode.sbs.main.presentation.up

import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.presentation.withHtmlDoctype
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.CharArrayWriter
import java.nio.charset.StandardCharsets

internal class UserPrivateHomeHtmlTest {

    @Test
    internal fun normal(): Unit = runBlocking {
        val written = CharArrayWriter().use { writer ->
            withHtmlDoctype(userPrivateHomeHtml(User(1001, "test-user"), emptyList(), "/logout", "/accounts")).invoke(writer)
            writer.toString()
        }

        val expectedHtmlUrl =
            this::class.java.classLoader.getResource("sbs/presentation/test/html/up/user-private-home.html")
        val expectedHtmlString = expectedHtmlUrl.openStream().use { it.readBytes().toString(StandardCharsets.UTF_8) }
        Assertions.assertEquals(expectedHtmlString, written)
    }

}
