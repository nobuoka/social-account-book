package info.vividcode.sbs.main.auth.application

import info.vividcode.ktor.twitter.login.TemporaryCredential
import info.vividcode.sbs.main.H2DatabaseTestExtension
import info.vividcode.sbs.main.infrastructure.database.createTransactionManager
import kotlinx.coroutines.experimental.runBlocking
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class TemporaryCredentialStoreImplTest {

    companion object {
        @JvmField
        @RegisterExtension
        val h2DatabaseTestExtension = H2DatabaseTestExtension()
    }

    @BeforeEach
    fun prepareTables() {
        val flyway = Flyway()
        flyway.dataSource = h2DatabaseTestExtension.dataSource
        flyway.migrate()
    }

    private val temporaryCredentialStore by lazy {
        TemporaryCredentialStoreImpl(createTransactionManager(h2DatabaseTestExtension.dataSource))
    }

    @Nested
    inner class SaveTemporaryCredential {
        @Test
        fun first() {
            // Act
            runBlocking {
                temporaryCredentialStore.saveTemporaryCredential(TemporaryCredential("test-token", "test-secret"))
            }

            // Assert
            val temporaryCredential = runBlocking {
                temporaryCredentialStore.findTemporaryCredential("test-token")
            }
            Assertions.assertEquals(TemporaryCredential("test-token", "test-secret"), temporaryCredential)
        }

        @Test
        fun sameToken() {
            // Arrange
            runBlocking {
                temporaryCredentialStore.saveTemporaryCredential(TemporaryCredential("test-token", "test-secret"))
            }

            // Act
            runBlocking {
                temporaryCredentialStore.saveTemporaryCredential(TemporaryCredential("test-token", "test-secret-2"))
            }

            // Assert
            val temporaryCredential = runBlocking {
                temporaryCredentialStore.findTemporaryCredential("test-token")
            }
            Assertions.assertEquals(TemporaryCredential("test-token", "test-secret-2"), temporaryCredential)
        }
    }

    @Nested
    inner class FindTemporaryCredential {
        @Test
        fun notSaved() {
            // Arrange
            runBlocking {
                temporaryCredentialStore.saveTemporaryCredential(TemporaryCredential("test-token", "test-secret"))
            }

            // Act
            val temporaryCredential = runBlocking {
                temporaryCredentialStore.findTemporaryCredential("test-token-2")
            }

            // Assert
            Assertions.assertNull(temporaryCredential)
        }

        @Test
        fun saved() {
            // Arrange
            runBlocking {
                temporaryCredentialStore.saveTemporaryCredential(TemporaryCredential("test-token", "test-secret"))
            }

            // Act
            val temporaryCredential = runBlocking {
                temporaryCredentialStore.findTemporaryCredential("test-token")
            }

            // Assert
            Assertions.assertEquals(TemporaryCredential("test-token", "test-secret"), temporaryCredential)
        }
    }

}
