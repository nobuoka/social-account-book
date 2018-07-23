package info.vividcode.sbs.main.auth.application

import info.vividcode.orm.where
import info.vividcode.sbs.main.ApplicationInternalException
import info.vividcode.sbs.main.H2DatabaseTestExtension
import info.vividcode.sbs.main.auth.domain.infrastructure.LoginSessionTuple
import info.vividcode.sbs.main.auth.domain.infrastructure.TwitterUserConnectionTuple
import info.vividcode.sbs.main.auth.domain.infrastructure.TwitterUserTuple
import info.vividcode.sbs.main.core.application.CreateUserService
import info.vividcode.sbs.main.core.application.FindUserService
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.infrastructure.database.createTransactionManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.experimental.runBlocking
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class CreateNewSessionServiceTest {

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
    private val mockCreateUserService = mockk<CreateUserService>()

    private val testTarget by lazy { CreateNewSessionService(txManager, mockFindUserService, mockCreateUserService) }

    @Test
    internal fun twitterAccountNotConnectedYet(): Unit = runBlocking {
        val testScreenName = "test_user"

        // Arrange
        coEvery { mockCreateUserService.createUser(testScreenName) } returns User(1001, testScreenName)

        // Act
        val sessionId = testTarget.createNewSessionByTwitterLogin(2001, testScreenName)

        // Assert
        val sessionTuple = txManager.withTransaction { tx ->
            tx.withOrmContext {
                loginSessions.select(where { LoginSessionTuple::id eq sessionId.value }).toSet().firstOrNull()
            }
        }
        Assertions.assertNotNull(sessionTuple)
        Assertions.assertEquals(sessionId.value, sessionTuple?.id)
    }

    @Test
    internal fun twitterAccountAlreadyConnected(): Unit = runBlocking {
        val testScreenName = "test_user"
        val testUserId = 1001L
        val testTwitterId = 2001L

        // Arrange
        txManager.withTransaction { tx ->
            tx.withOrmContext {
                twitterUsers.insert(TwitterUserTuple(testTwitterId, testScreenName))
                twitterUserConnectionsRelation.insert(TwitterUserConnectionTuple(testUserId, testTwitterId))
            }
        }
        coEvery { mockFindUserService.findUser(testUserId) } returns User(testUserId, testScreenName)

        // Act
        val sessionId = testTarget.createNewSessionByTwitterLogin(testTwitterId, testScreenName)

        // Assert
        val sessionTuple = txManager.withTransaction { tx ->
            tx.withOrmContext {
                loginSessions.select(where { LoginSessionTuple::id eq sessionId.value }).toSet().firstOrNull()
            }
        }
        Assertions.assertNotNull(sessionTuple)
        Assertions.assertEquals(sessionId.value, sessionTuple?.id)
    }

    @Test
    internal fun twitterAccountAlreadyConnected_dataInconsistency(): Unit = runBlocking {
        val testScreenName = "test_user"
        val testUserId = 1001L
        val testTwitterId = 2001L

        // Arrange
        txManager.withTransaction { tx ->
            tx.withOrmContext {
                twitterUsers.insert(TwitterUserTuple(testTwitterId, testScreenName))
                twitterUserConnectionsRelation.insert(TwitterUserConnectionTuple(testUserId, testTwitterId))
            }
        }
        coEvery { mockFindUserService.findUser(testUserId) } returns null

        // Act
        val exception = Assertions.assertThrows(ApplicationInternalException.DataInconsistency::class.java) {
            runBlocking {
                testTarget.createNewSessionByTwitterLogin(testTwitterId, testScreenName)
            }
        }

        // Assert
        Assertions.assertEquals("Data inconsistency (user[id = 1001] not found)", exception.message)
    }

}
