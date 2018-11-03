package info.vividcode.sbs.main.core.domain

import info.vividcode.sbs.main.application.test.TestTransactionManagerExtension
import info.vividcode.sbs.main.application.test.createTestAppStorage
import info.vividcode.sbs.main.infrastructure.database.AppOrmContext
import kotlinx.coroutines.runBlocking
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
            val testData = TestData.create(this)
            val accountBooks = findAccountBooksOfUser(testData.userNotHavingAccountBook)
            Assertions.assertEquals(emptySet<AccountBook>(), accountBooks)
        }

        @Test
        internal fun normal() = withContext {
            val testData = TestData.create(this)
            val accountBooks = findAccountBooksOfUser(testData.user)
            Assertions.assertEquals(setOf(testData.accountBook1, testData.accountBook2), accountBooks)
        }

        @Test
        internal fun normal_idSpecified_empty() = withContext {
            val testData = TestData.create(this)
            val accountBooks = findAccountBooksOfUser(testData.user, emptySet())
            Assertions.assertEquals(emptySet<AccountBook>(), accountBooks)
        }

        @Test
        internal fun normal_idSpecified_mine() = withContext {
            val testData = TestData.create(this)
            val accountBooks = findAccountBooksOfUser(testData.user, setOf(testData.accountBook1.id))
            Assertions.assertEquals(setOf(testData.accountBook1), accountBooks)
        }

        @Test
        internal fun normal_idSpecified_otherUsers() = withContext {
            val testData = TestData.create(this)
            val accountBooks = findAccountBooksOfUser(testData.user, setOf(testData.accountBookOfOtherUser.id))
            Assertions.assertEquals(emptySet<AccountBook>(), accountBooks)
        }
    }

    private class TestData(
            val user: User,
            val accountBook1: AccountBook,
            val accountBook2: AccountBook,
            val userNotHavingAccountBook: User,
            val accountBookOfOtherUser: AccountBook
    ) {
        companion object {
            fun create(context: AppOrmContext) = with (context) {
                val user = createUser("Test User")
                val userNotHavingAccountBook = createUser("Test User 2")
                val otherUser = createUser("Test User 3")
                TestData(
                        user = user,
                        accountBook1 = createUserAccountBook(user, "Test Bank 1"),
                        accountBook2 = createUserAccountBook(user, "Test Bank 2"),
                        userNotHavingAccountBook = userNotHavingAccountBook,
                        accountBookOfOtherUser = createUserAccountBook(otherUser, "Other User's Bank")
                )
            }
        }
    }

    private fun withContext(test: AppOrmContext.() -> Unit): Unit = runBlocking {
        txManager.withTransaction { tx -> tx.withOrmContext(test) }
    }

}
