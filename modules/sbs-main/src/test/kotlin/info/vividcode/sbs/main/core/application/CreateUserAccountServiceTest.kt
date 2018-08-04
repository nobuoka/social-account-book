package info.vividcode.sbs.main.core.application

import info.vividcode.orm.where
import info.vividcode.sbs.main.H2DatabaseTestExtension
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.core.domain.infrastructure.UserTuple
import info.vividcode.sbs.main.core.domain.infrastructure.from
import info.vividcode.sbs.main.infrastructure.database.createTransactionManager
import kotlinx.coroutines.experimental.runBlocking
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class CreateUserAccountServiceTest {

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

    private val testTarget by lazy { CreateUserAccountService(txManager) }

    @Test
    internal fun simple(): Unit = runBlocking {
        val testAccountLabel = "Test Account"
        val testActor = createUser("test-user")

        // Act
        val account = testTarget.createUserAccount(testActor, testAccountLabel)

        // Assert
        Assertions.assertEquals(testAccountLabel, account.label)
    }

    private fun createUser(userName: String): User = runBlocking {
        txManager.withTransaction { tx ->
            tx.withOrmContext {
                val userId = users.insert(UserTuple.Content(userName))
                requireNotNull(users.select(where { UserTuple::id eq userId }).toSet().firstOrNull())
            }
        }.let(User.Companion::from)
    }

}
