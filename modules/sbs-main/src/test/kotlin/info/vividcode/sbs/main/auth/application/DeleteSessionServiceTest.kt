package info.vividcode.sbs.main.auth.application

import info.vividcode.sbs.main.H2DatabaseTestExtension
import info.vividcode.sbs.main.auth.domain.SessionId
import info.vividcode.sbs.main.auth.domain.infrastructure.LoginSessionTuple
import info.vividcode.sbs.main.infrastructure.database.createTransactionManager
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class DeleteSessionServiceTest {

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

    private val testTarget by lazy { DeleteSessionService(txManager) }

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
        val deleted = testTarget.deleteSession(SessionId(targetSessionId))

        // Assert
        assertFalse(deleted)
        val sessions = txManager.withTransaction { tx ->
            tx.withOrmContext { loginSessions.toSet() }
        }
        assertEquals(savedSessionIds, sessions.map { it.id }.toSet())
    }

    @Test
    internal fun sessionExist(): Unit = runBlocking {
        // Arrange
        val savedSessionIds = txManager.withTransaction { tx ->
            tx.withOrmContext {
                setOf(
                    loginSessions.insert(LoginSessionTuple.Content(1001)),
                    loginSessions.insert(LoginSessionTuple.Content(1002))
                )
            }
        }
        val targetSessionId = savedSessionIds.first()

        // Act
        val deleted = testTarget.deleteSession(SessionId(targetSessionId))

        // Assert
        assertTrue(deleted)
        val sessions = txManager.withTransaction { tx ->
            tx.withOrmContext { loginSessions.toSet() }
        }
        assertEquals(savedSessionIds.filter { it != targetSessionId }.toSet(), sessions.map { it.id }.toSet())
    }

}
