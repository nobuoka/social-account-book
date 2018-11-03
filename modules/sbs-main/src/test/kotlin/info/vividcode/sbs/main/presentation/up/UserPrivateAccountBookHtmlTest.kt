package info.vividcode.sbs.main.presentation.up

import info.vividcode.sbs.main.core.domain.Account
import info.vividcode.sbs.main.core.domain.AccountBook
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.presentation.withHtmlDoctype
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.CharArrayWriter
import java.nio.charset.StandardCharsets

internal class UserPrivateAccountBookHtmlTest {

    @Test
    internal fun normal_accountsExist(): Unit = runBlocking {
        val accountBook = AccountBook(1, "Test account book")
        val account = Account(2, "Test account")
        val written = CharArrayWriter().use { writer ->
            withHtmlDoctype(userPrivateAccountBookHtml(
                    User(1001, "test-user"), accountBook, listOf(account),
                    logoutPath = "/logout",
                    userAccountsPath = "/accounts"
            )).invoke(writer)
            writer.toString()
        }

        val expectedHtmlUrl =
                this::class.java.classLoader.getResource("sbs/presentation/test/html/up/account-book-detail.html")
        val expectedHtmlString = expectedHtmlUrl.openStream().use { it.readBytes().toString(StandardCharsets.UTF_8) }
        assertEquals(expectedHtmlString, written)
    }

    @Test
    internal fun normal_noAccount(): Unit = runBlocking {
        val accountBook = AccountBook(1, "Test account book")
        val written = CharArrayWriter().use { writer ->
            withHtmlDoctype(userPrivateAccountBookHtml(
                    User(1001, "test-user"), accountBook, emptyList(),
                    logoutPath = "/logout",
                    userAccountsPath = "/accounts"
            )).invoke(writer)
            writer.toString()
        }

        val expectedHtmlUrl =
                this::class.java.classLoader.getResource("sbs/presentation/test/html/up/account-book-detail-no-account.html")
        val expectedHtmlString = expectedHtmlUrl.openStream().use { it.readBytes().toString(StandardCharsets.UTF_8) }
        assertEquals(expectedHtmlString, written)
    }

}
