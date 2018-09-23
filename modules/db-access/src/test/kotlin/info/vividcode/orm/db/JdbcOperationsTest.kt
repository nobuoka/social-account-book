package info.vividcode.orm.db

import info.vividcode.orm.AttributeName
import info.vividcode.orm.KdbiRuntimeException
import info.vividcode.orm.TupleClassRegistry
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

internal class JdbcOperationsTest {

    internal data class FooTuple(
            @AttributeName("id") val id: Long,
            val content: Content
    ) {
        internal data class Content(
                @AttributeName("value1") val testValue1: String,
                @AttributeName("value2") val testValue2: Int
        )
    }

    private lateinit var dataSource: DataSource
    private lateinit var connection: Connection

    @BeforeEach
    internal fun prepareDb() {
        dataSource = JdbcDataSource().apply {
            setUrl("jdbc:h2:mem:test;TRACE_LEVEL_FILE=4")
        }
        connection = dataSource.connection
        connection.prepareStatement("""CREATE TABLE "foo" ( "id" BIGINT, "value1" TEXT, "value2" INTEGER )""")
                .executeUpdate()
    }

    @AfterEach
    internal fun finalizeDb() {
        if (this::connection.isInitialized) {
            connection.close()
        }
    }

    @Test
    internal fun insert_generatedKeys_exception() {
        val insertedValue = FooTuple(1L, FooTuple.Content("test1", 2))
        val tupleClassRegistry = TupleClassRegistry()

        val exception = Assertions.assertThrows(KdbiRuntimeException::class.java) {
            insert(connection, "foo", insertedValue, tupleClassRegistry, true)
        }

        Assertions.assertEquals("Failed to fetch generated keys.", exception.message)
        Assertions.assertTrue(exception.cause is SQLException) {
            "Cause is not a `${SQLException::class.simpleName}` (${exception.cause})"
        }
    }

}
