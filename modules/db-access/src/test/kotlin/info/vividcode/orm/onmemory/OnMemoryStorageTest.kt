package info.vividcode.orm.onmemory

import info.vividcode.orm.TupleClassRegistry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.lang.RuntimeException

internal class OnMemoryStorageTest {

    @Nested
    internal inner class StandardTest {
        @Test
        internal fun test() {
            val storage = OnMemoryStorage()
            val tupleClassRegistry = TupleClassRegistry()

            data class Tuple(val id: Long)
            storage.registerRelation("test", Tuple::class, emptySet())

            Assertions.assertEquals(mapOf("test" to emptySet<Tuple>()), storage.currentState)

            storage.createConnection().insert("test", Tuple(2), false, tupleClassRegistry)

            Assertions.assertEquals(mapOf("test" to setOf(Tuple(2))), storage.currentState)
        }
    }

    @Nested
    internal inner class ErrorTest {
        @Test
        internal fun unknownRelation() {
            val storage = OnMemoryStorage()
            val tupleClassRegistry = TupleClassRegistry()

            data class Tuple(val id: Long)

            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                storage.createConnection().insert("test", Tuple(2), false, tupleClassRegistry)
            }

            Assertions.assertEquals("Unknown relation (test)", exception.message)
        }

        @Test
        internal fun insertNullForNotNullable() {
            val storage = OnMemoryStorage()
            val tupleClassRegistry = TupleClassRegistry()

            data class Tuple(val id: Long)
            storage.registerRelation("test", Tuple::class, emptySet())

            data class Inserted(val id: Long?)
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                storage.createConnection().insert("test", Inserted(null), false, tupleClassRegistry)
            }

            Assertions.assertEquals("Not nullable (attribute name : id)", exception.message)
        }

        @Test
        internal fun insertNotAcceptableType() {
            val storage = OnMemoryStorage()
            val tupleClassRegistry = TupleClassRegistry()

            data class Tuple(val id: Long)
            storage.registerRelation("test", Tuple::class, emptySet())

            data class Inserted(val id: String)
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                storage.createConnection().insert("test", Inserted("test"), false, tupleClassRegistry)
            }

            Assertions.assertEquals(
                    "Unexpected class (attribute name : id, expected class : class kotlin.Long, actual class : class kotlin.String)",
                    exception.message
            )
        }
    }

}
