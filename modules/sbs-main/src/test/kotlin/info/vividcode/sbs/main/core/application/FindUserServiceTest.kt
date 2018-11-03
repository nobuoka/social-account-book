package info.vividcode.sbs.main.core.application

import info.vividcode.sbs.main.H2DatabaseTestExtension
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.core.domain.infrastructure.UserTuple
import info.vividcode.sbs.main.infrastructure.database.createTransactionManager
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class FindUserServiceTest {

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

    private val testTarget by lazy { FindUserService.create(txManager) }

    @Test
    internal fun notFound() {
        val testName = "Test Name"

        // Arrange
        val userId = runBlocking {
            txManager.withTransaction { tx ->
                tx.withOrmContext {
                    users.insert(UserTuple.Content(testName))
                }
            }
        }

        // Act
        val user = runBlocking {
            testTarget.findUser(userId + 1)
        }

        // Assert
        Assertions.assertNull(user)
    }

    @Test
    internal fun found() {
        val testName = "Test Name"

        // Arrange
        val userId = runBlocking {
            txManager.withTransaction { tx ->
                tx.withOrmContext {
                    users.insert(UserTuple.Content(testName))
                }
            }
        }

        // Act
        val user = runBlocking {
            testTarget.findUser(userId)
        }

        // Assert
        Assertions.assertEquals(User(userId, testName), user)
    }

}
