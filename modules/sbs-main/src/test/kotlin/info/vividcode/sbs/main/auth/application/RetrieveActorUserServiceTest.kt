package info.vividcode.sbs.main.auth.application

import info.vividcode.sbs.main.H2DatabaseTestExtension
import info.vividcode.sbs.main.auth.domain.SessionId
import info.vividcode.sbs.main.auth.domain.infrastructure.LoginSessionTuple
import info.vividcode.sbs.main.core.application.FindUserService
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.infrastructure.database.createTransactionManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.experimental.runBlocking
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class RetrieveActorUserServiceTest {

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

    private val mockFindUserService = mockk<FindUserService>()

    private val testTarget by lazy { RetrieveActorUserService(txManager, mockFindUserService) }

    @Test
    internal fun sessionNotExist(): Unit = runBlocking {
        val targetSessionId = 9999L

        // Arrange
        val savedSessionIds = txManager.withTransaction { tx ->
            tx.withOrmContext {
                setOf(
                    loginSessions.insert(LoginSessionTuple.Content(1001)),
                    loginSessions.insert(LoginSessionTuple.Content(1002))
                )
            }
        }

        // Assume
        assertFalse(savedSessionIds.contains(targetSessionId))

        // Act
        val actorUserOrNull = testTarget.retrieveActorUserOrNull(SessionId(targetSessionId))

        // Assert
        assertNull(actorUserOrNull)
    }

    @Test
    internal fun sessionExist(): Unit = runBlocking {
        val testUserId = 1001L
        val testUserDisplayName = "test-user"

        // Arrange
        val savedSessionIds = txManager.withTransaction { tx ->
            tx.withOrmContext {
                listOf(
                    loginSessions.insert(LoginSessionTuple.Content(testUserId)),
                    loginSessions.insert(LoginSessionTuple.Content(1002))
                )
            }
        }
        coEvery { mockFindUserService.findUser(testUserId) } returns User(testUserId, testUserDisplayName)
        val targetSessionId = savedSessionIds.first()

        // Act
        val actorUserOrNull = testTarget.retrieveActorUserOrNull(SessionId(targetSessionId))

        // Assert
        assertEquals(User(testUserId, testUserDisplayName), actorUserOrNull)
    }

}
