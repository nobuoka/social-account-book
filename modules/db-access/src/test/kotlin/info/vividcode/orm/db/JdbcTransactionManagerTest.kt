package info.vividcode.orm.db

import info.vividcode.orm.*
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import kotlinx.coroutines.experimental.runBlocking
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import javax.sql.DataSource

internal class JdbcTransactionManagerTest {

    data class FooTuple(
        @AttributeName("id") val id: Long,
        val content: Content
    ) {
        data class Content(
            @AttributeName("value1") val testValue1: String,
            @AttributeName("value2") val testValue2: Int
        )
    }

    @RelationName("foo")
    interface FooRelation : BareRelation<FooTuple>

    interface AppOrmContext : OrmQueryContext {
        val fooRelation: FooRelation

        @Insert
        fun FooRelation.insert(foo: FooTuple)
    }

    private lateinit var dataSource: DataSource
    private lateinit var connection: Connection

    @BeforeEach
    fun prepareDb() {
        dataSource = JdbcDataSource().apply {
            setUrl("jdbc:h2:mem:test;TRACE_LEVEL_FILE=4")
        }
        connection = dataSource.connection
        connection.prepareStatement("""CREATE TABLE "foo" ( "id" BIGINT, "value1" TEXT, "value2" INTEGER )""")
            .executeUpdate()
    }

    @AfterEach
    fun finalizeDb() {
        if (this::connection.isInitialized) {
            connection.close()
        }
    }

    @Test
    fun commit() {
        val jdbcCoroutineDispatcher = newFixedThreadPoolContext(4, "JdbcCoroutineContext")
        val transactionManager: TransactionManager<OrmContextProvider<AppOrmContext>> =
            JdbcTransactionManager(
                JdbcOrmContexts.createProviderFactoryFor(AppOrmContext::class, jdbcCoroutineDispatcher),
                dataSource
            )

        runBlocking {
            transactionManager.withTransaction { tx ->
                tx.withOrmContext {
                    fooRelation.insert(FooTuple(10, FooTuple.Content("Hello", 100)))
                }
            }
        }

        runBlocking {
            val selected = transactionManager.withTransaction { tx ->
                tx.withOrmContext {
                    fooRelation.select(where { FooTuple::id eq 10 }).toSet()
                }
            }
            Assertions.assertEquals(
                setOf(FooTuple(10, FooTuple.Content("Hello", 100))),
                selected
            )
        }
    }

    @Test
    fun rollback() {
        val jdbcCoroutineDispatcher = newFixedThreadPoolContext(4, "JdbcCoroutineContext")
        val transactionManager: TransactionManager<OrmContextProvider<AppOrmContext>> =
            JdbcTransactionManager(
                JdbcOrmContexts.createProviderFactoryFor(AppOrmContext::class, jdbcCoroutineDispatcher),
                dataSource
            )

        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            runBlocking {
                transactionManager.withTransaction { tx ->
                    tx.withOrmContext {
                        fooRelation.insert(FooTuple(10, FooTuple.Content("Hello", 100)))
                    }
                    throw RuntimeException("Test exception")
                }
            }
        }
        Assertions.assertEquals("Test exception", exception.message)

        val selected = runBlocking {
            transactionManager.withTransaction { tx ->
                tx.withOrmContext {
                    fooRelation.select(where { FooTuple::id eq 10 }).toSet()
                }
            }
        }
        Assertions.assertEquals(emptySet<FooTuple>(), selected)
    }

}
