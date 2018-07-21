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

        private const val testToken1 = "test-token-1"
        private const val testToken2 = "test-token-2"
        private const val testSecret1 = "test-secret-1"
        private const val testSecret2 = "test-secret-2"
    }

    @BeforeEach
    internal fun prepareTables() {
        val flyway = Flyway()
        flyway.dataSource = h2DatabaseTestExtension.dataSource
        flyway.migrate()
    }

    private val temporaryCredentialStore by lazy {
        TemporaryCredentialStoreImpl(createTransactionManager(h2DatabaseTestExtension.dataSource))
    }

    @Nested
    internal inner class SaveTemporaryCredential {
        @Test
        internal fun first() {
            // Act
            runBlocking {
                temporaryCredentialStore.saveTemporaryCredential(TemporaryCredential(testToken1, testSecret1))
            }

            // Assert
            val temporaryCredential = runBlocking {
                temporaryCredentialStore.findTemporaryCredential(testToken1)
            }
            Assertions.assertEquals(TemporaryCredential(testToken1, testSecret1), temporaryCredential)
        }

        @Test
        internal fun sameToken() {
            // Arrange
            runBlocking {
                temporaryCredentialStore.saveTemporaryCredential(TemporaryCredential(testToken1, testSecret1))
            }

            // Act
            runBlocking {
                temporaryCredentialStore.saveTemporaryCredential(TemporaryCredential(testToken1, testSecret2))
            }

            // Assert
            val temporaryCredential = runBlocking {
                temporaryCredentialStore.findTemporaryCredential(testToken1)
            }
            Assertions.assertEquals(TemporaryCredential(testToken1, testSecret2), temporaryCredential)
        }
    }

    @Nested
    internal inner class FindTemporaryCredential {
        @Test
        internal fun notSaved() {
            // Arrange
            runBlocking {
                temporaryCredentialStore.saveTemporaryCredential(TemporaryCredential(testToken1, testSecret1))
            }

            // Act
            val temporaryCredential = runBlocking {
                temporaryCredentialStore.findTemporaryCredential(testToken2)
            }

            // Assert
            Assertions.assertNull(temporaryCredential)
        }

        @Test
        internal fun saved() {
            // Arrange
            runBlocking {
                temporaryCredentialStore.saveTemporaryCredential(TemporaryCredential(testToken1, testSecret1))
            }

            // Act
            val temporaryCredential = runBlocking {
                temporaryCredentialStore.findTemporaryCredential(testToken1)
            }

            // Assert
            Assertions.assertEquals(TemporaryCredential(testToken1, testSecret1), temporaryCredential)
        }
    }

}
