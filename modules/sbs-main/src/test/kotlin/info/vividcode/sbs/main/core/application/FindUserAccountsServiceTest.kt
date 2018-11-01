package info.vividcode.sbs.main.core.application

import info.vividcode.orm.where
import info.vividcode.sbs.main.H2DatabaseTestExtension
import info.vividcode.sbs.main.core.domain.Account
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.core.domain.createAccount
import info.vividcode.sbs.main.core.domain.createUserAccountBook
import info.vividcode.sbs.main.core.domain.infrastructure.UserTuple
import info.vividcode.sbs.main.core.domain.infrastructure.from
import info.vividcode.sbs.main.infrastructure.database.createTransactionManager
import kotlinx.coroutines.experimental.runBlocking
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class FindUserAccountsServiceTest {

    companion object {
        @JvmField
        @RegisterExtension
        val h2DatabaseTestExtension = H2DatabaseTestExtension()
    }

    @BeforeEach
    internal fun prepareTables() {
        val flyway = Flyway()
        flyway.dataSource = h2DatabaseTestExtension.dataSource
        flyway.migrate()
    }

    private val txManager by lazy { createTransactionManager(h2DatabaseTestExtension.dataSource) }

    private val testTarget by lazy { FindUserAccountsService(txManager) }

    @Test
    internal fun simple(): Unit = runBlocking {
        val testTargetUser = createUser("test-user-1")
        createUserAccounts(testTargetUser, listOf("Test Mega Bank", "Cash stash"))
        val testUser2 = createUser("test-user-2")
        createUserAccounts(testUser2, listOf("Test City Bank", "Wallet"))

        // Act
        val accounts = testTarget.findUserAccounts(testTargetUser)

        // Assert
        Assertions.assertEquals(setOf("Test Mega Bank", "Cash stash"), accounts.map { it.label }.toSet())
    }

    @Test
    internal fun simple_empty(): Unit = runBlocking {
        val testTargetUser = createUser("test-user-1")
        val testUser2 = createUser("test-user-2")
        createUserAccounts(testUser2, listOf("Test City Bank", "Wallet"))

        // Act
        val accounts = testTarget.findUserAccounts(testTargetUser)

        // Assert
        Assertions.assertEquals(emptyList<Account>(), accounts)
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
            val defaultAccountBook = createUserAccountBook(user, "default")
            labels.forEach {
                createAccount(defaultAccountBook, it)
            }
        }
    }

}
