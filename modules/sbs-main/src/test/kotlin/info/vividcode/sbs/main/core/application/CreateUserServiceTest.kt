package info.vividcode.sbs.main.core.application

import info.vividcode.sbs.main.H2DatabaseTestExtension
import info.vividcode.sbs.main.core.domain.infrastructure.UserTuple
import info.vividcode.sbs.main.infrastructure.database.createTransactionManager
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class CreateUserServiceTest {

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

    private val testTarget by lazy { CreateUserService.create(txManager) }

    @Test
    internal fun simple() {
        val testName = "Test Name"

        // Act
        val user = runBlocking {
            testTarget.createUser(testName)
        }

        // Assert
        Assertions.assertEquals(testName, user.displayName)

        val users = runBlocking {
            txManager.withTransaction { tx ->
                tx.withOrmContext { users.toSet() }
            }
        }
        val expected = setOf(UserTuple(user.id, UserTuple.Content(testName)))
        Assertions.assertEquals(expected, users)
    }

    @Test
    internal fun sameDisplayName() {
        val testName = "Test Name"

        // Arrange
        val previouslyCreated = runBlocking {
            testTarget.createUser(testName)
        }

        // Act
        val user = runBlocking {
            testTarget.createUser(testName)
        }

        // Assert
        Assertions.assertEquals(testName, user.displayName)
        Assertions.assertNotEquals(previouslyCreated.id, user.id)

        val users = runBlocking {
            txManager.withTransaction { tx ->
                tx.withOrmContext { users.toSet() }
            }
        }
        val expected = setOf(
            UserTuple(user.id, UserTuple.Content(testName)),
            UserTuple(previouslyCreated.id, UserTuple.Content(testName))
        )
        Assertions.assertEquals(expected, users)
    }

}
