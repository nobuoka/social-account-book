package info.vividcode.sbs.main.core.application

import info.vividcode.sbs.main.H2DatabaseTestExtension
import info.vividcode.sbs.main.core.domain.*
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

    private val findAccountsForAccountBooks by lazy { FindAccountsForAccountBooksFunction(txManager) }

    @Test
    internal fun simple(): Unit = runBlocking {
        val testData = TestData.create(txManager)

        // Act
        val accountMap = findAccountsForAccountBooks(testData.testUser1, setOf(testData.accountBook11.id, testData.accountBook12.id))

        // Assert
        Assertions.assertEquals(
                mapOf(
                        testData.accountBook11 to listOf(testData.accountBook11Account1, testData.accountBook11Account2),
                        testData.accountBook12 to listOf(testData.accountBook12Account1)
                ),
                accountMap
        )
    }

    @Test
    internal fun simple_empty(): Unit = runBlocking {
        val testData = TestData.create(txManager)

        // Act
        val accountsMap = findAccountsForAccountBooks(testData.testUser1, setOf(testData.accountBook13NotHavingAccount.id))

        // Assert
        Assertions.assertEquals(
                mapOf(
                        testData.accountBook13NotHavingAccount to emptyList<Account>()
                ),
                accountsMap
        )
    }

    private class TestData(
            val testUser1: User,
            val accountBook11: AccountBook,
            val accountBook11Account1: Account,
            val accountBook11Account2: Account,
            val accountBook12: AccountBook,
            val accountBook12Account1: Account,
            val accountBook13NotHavingAccount: AccountBook,
            val testUser2: User,
            val accountBook21: AccountBook,
            val accountBook21Account1: Account,
            val testUserNotHavingAccountBook: User
    ) {
        companion object {
            suspend fun create(txManager: CoreTxManager) = txManager.withOrmContext {
                val user1 = createUser("Test User 1")
                val user2 = createUser("Test User 2")
                val userNotHavingAccountBook = createUser("Test User 3")
                val accountBook11 = createUserAccountBook(user1, "Account Book 11")
                val accountBook12 = createUserAccountBook(user1, "Account Book 12")
                val accountBook13NotHavingAccount = createUserAccountBook(user1, "Account Book 13")
                val accountBook21 = createUserAccountBook(user2, "Account Book 21")
                TestData(
                        testUser1 = user1,
                        accountBook11 = accountBook11,
                        accountBook11Account1 = createAccount(accountBook11, "Account 11-1"),
                        accountBook11Account2 = createAccount(accountBook11, "Account 11-2"),
                        accountBook12 = accountBook12,
                        accountBook12Account1 = createAccount(accountBook12, "Account 12-1"),
                        accountBook13NotHavingAccount = accountBook13NotHavingAccount,
                        testUser2 = user2,
                        accountBook21 = accountBook21,
                        accountBook21Account1 = createAccount(accountBook21, "Account 21-1"),
                        testUserNotHavingAccountBook = userNotHavingAccountBook
                )
            }
        }
    }

}
