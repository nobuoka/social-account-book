package info.vividcode.sbs.main.core.application

import info.vividcode.orm.where
import info.vividcode.sbs.main.application.test.TestTransactionManagerExtension
import info.vividcode.sbs.main.application.test.createTestAppStorage
import info.vividcode.sbs.main.core.domain.AccountBook
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.core.domain.createUserAccount
import info.vividcode.sbs.main.core.domain.createUserAccountBook
import info.vividcode.sbs.main.core.domain.infrastructure.UserTuple
import info.vividcode.sbs.main.core.domain.infrastructure.from
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class FindUserAccountsFunctionTest {

    private val txManager by lazy { testTransactionManagerExtension.txManager }

    @RegisterExtension
    @JvmField
    internal val testTransactionManagerExtension = TestTransactionManagerExtension(::createTestAppStorage)

    private val findUserAccountsBook by lazy { FindUserAccountBooksFunction(txManager) }

    @Test
    internal fun simple(): Unit = runBlocking {
        val testTargetUser = createUser("test-user-1")
        createUserAccounts(testTargetUser, listOf("Test Mega Bank", "Cash stash"))
        val testUser2 = createUser("test-user-2")
        createUserAccounts(testUser2, listOf("Test City Bank", "Wallet"))

        val testAccountBooks = txManager.withTransaction { tx ->
            tx.withOrmContext {
                setOf(
                        createUserAccountBook(testTargetUser, "Book1"),
                        createUserAccountBook(testTargetUser, "Book2")
                )
            }
        }

        run {
            // Act
            val accountBooks = findUserAccountsBook(testTargetUser)

            // Assert
            Assertions.assertEquals(testAccountBooks, accountBooks.toSet())
        }

        run {
            // Act
            val accountBooks = findUserAccountsBook(testUser2)

            // Assert
            Assertions.assertEquals(emptySet<AccountBook>(), accountBooks.toSet())
        }
    }

    private fun createUser(userName: String): User = runBlocking {
        txManager.withTransaction { tx ->
            tx.withOrmContext {
                val userId = users.insert(UserTuple.Content(userName))
                requireNotNull(users.select(where { UserTuple::id eq userId }).toSet().firstOrNull())
            }
        }.let(User.Companion::from)
    }

    private suspend fun createUserAccounts(user: User, labels: List<String>) {
        txManager.withOrmContext {
            labels.forEach {
                createUserAccount(user, it)
            }
        }
    }

}
