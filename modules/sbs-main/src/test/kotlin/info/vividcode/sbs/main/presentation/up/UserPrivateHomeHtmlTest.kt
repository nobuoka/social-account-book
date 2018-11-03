package info.vividcode.sbs.main.presentation.up

import info.vividcode.sbs.main.core.domain.Account
import info.vividcode.sbs.main.core.domain.AccountBook
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.presentation.withHtmlDoctype
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.CharArrayWriter
import java.nio.charset.StandardCharsets

internal class UserPrivateHomeHtmlTest {

    @Test
    internal fun normal_accountsExist(): Unit = runBlocking {
        val accountBooks = listOf(AccountBook(1, "Test Bank"))
        val written = CharArrayWriter().use { writer ->
            withHtmlDoctype(userPrivateHomeHtml(
                    User(1001, "test-user"), accountBooks,
                    logoutPath = "/logout",
                    userAccountBooksPath = "/account-books",
                    userPrivateAccountBookPathGenerator = { accountBook -> "/account-books/${accountBook.id}" }
            )).invoke(writer)
            writer.toString()
        }

        val expectedHtmlUrl =
            this::class.java.classLoader.getResource("sbs/presentation/test/html/up/user-private-home.html")
        val expectedHtmlString = expectedHtmlUrl.openStream().use { it.readBytes().toString(StandardCharsets.UTF_8) }
        Assertions.assertEquals(expectedHtmlString, written)
    }

    @Test
    internal fun normal_noAccount(): Unit = runBlocking {
        val accountBooks = emptyList<AccountBook>()
        val written = CharArrayWriter().use { writer ->
            withHtmlDoctype(userPrivateHomeHtml(
                    User(1001, "test-user"), accountBooks,
                    logoutPath = "/logout",
                    userAccountBooksPath = "/account-books",
                    userPrivateAccountBookPathGenerator = { accountBook -> "/account-books/${accountBook.id}" }
            )).invoke(writer)
            writer.toString()
        }

        val expectedHtmlUrl =
            this::class.java.classLoader.getResource("sbs/presentation/test/html/up/user-private-home-no-account.html")
        val expectedHtmlString = expectedHtmlUrl.openStream().use { it.readBytes().toString(StandardCharsets.UTF_8) }
        Assertions.assertEquals(expectedHtmlString, written)
    }

}
