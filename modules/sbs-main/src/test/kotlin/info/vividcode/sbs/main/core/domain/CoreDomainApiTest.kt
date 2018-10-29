package info.vividcode.sbs.main.core.domain

import info.vividcode.sbs.main.application.test.TestTransactionManagerExtension
import info.vividcode.sbs.main.application.test.createTestAppStorage
import info.vividcode.sbs.main.infrastructure.database.AppOrmContext
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class CoreDomainApiTest {

    private val txManager by lazy { testTransactionManagerExtension.txManager }

    @RegisterExtension
    @JvmField
    internal val testTransactionManagerExtension = TestTransactionManagerExtension(::createTestAppStorage)

    @Nested
    internal inner class FindAccountBookOfUserTest {
        @Test
        internal fun normal_notFound() = withContext {
            val user = createUser("Test User")

            val accountBooks = findAccountBooksOfUser(user)

            Assertions.assertEquals(emptySet<AccountBook>(), accountBooks)
        }

        @Test
        internal fun normal_found() = withContext {
            val user = createUser("Test User")
            val accountBook1 = createUserAccountBook(user, "Test Bank 1")
            val accountBook2 = createUserAccountBook(user, "Test Bank 2")

            val accountBooks = findAccountBooksOfUser(user)

            Assertions.assertEquals(setOf(accountBook1, accountBook2), accountBooks)
        }

        @Test
        internal fun normal_otherUser() = withContext {
            val user = createUser("Test User")
            val accountBook1 = createUserAccountBook(user, "Test Bank 1")
            val accountBook2 = createUserAccountBook(user, "Test Bank 2")
            val otherUser = createUser("Test User 2")
            createUserAccountBook(otherUser, "Other User's Bank")

            val accountBooks = findAccountBooksOfUser(user)

            Assertions.assertEquals(setOf(accountBook1, accountBook2), accountBooks)
        }

        @Test
        internal fun normal_idSpecified_empty() = withContext {
            val user = createUser("Test User")
            createUserAccountBook(user, "Test Bank 1")
            createUserAccountBook(user, "Test Bank 2")
            val otherUser = createUser("Test User 2")
            createUserAccountBook(otherUser, "Other User's Bank")

            val accountBooks = findAccountBooksOfUser(user, emptySet())

            Assertions.assertEquals(emptySet<AccountBook>(), accountBooks)
        }

        @Test
        internal fun normal_idSpecified_mine() = withContext {
            val user = createUser("Test User")
            val accountBook1 = createUserAccountBook(user, "Test Bank 1")
            createUserAccountBook(user, "Test Bank 2")
            val otherUser = createUser("Test User 2")
            createUserAccountBook(otherUser, "Other User's Bank")

            val accountBooks = findAccountBooksOfUser(user, setOf(accountBook1.id))

            Assertions.assertEquals(setOf(accountBook1), accountBooks)
        }

        @Test
        internal fun normal_idSpecified_otherUsers() = withContext {
            val user = createUser("Test User")
            createUserAccountBook(user, "Test Bank 1")
            createUserAccountBook(user, "Test Bank 2")
            val otherUser = createUser("Test User 2")
            val accountBookOfOtherUser = createUserAccountBook(otherUser, "Other User's Bank")

            val accountBooks = findAccountBooksOfUser(user, setOf(accountBookOfOtherUser.id))

            Assertions.assertEquals(emptySet<AccountBook>(), accountBooks)
        }
    }

    private fun withContext(test: AppOrmContext.() -> Unit): Unit = runBlocking {
        txManager.withTransaction { tx -> tx.withOrmContext(test) }
    }

}
