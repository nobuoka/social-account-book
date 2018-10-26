package info.vividcode.orm.common

import info.vividcode.orm.*
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal abstract class TransactionManagerSpec(
        private val transactionManagerGetter: () -> TransactionManager<OrmContextProvider<AppOrmContext>>
) {

    private val transactionManager by lazy { transactionManagerGetter() }

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

    @Test
    fun commit() {
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
