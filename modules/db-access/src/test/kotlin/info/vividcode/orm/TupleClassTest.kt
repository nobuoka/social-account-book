package info.vividcode.orm

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TupleClassTest {

    private val tupleClassRegistry = TupleClassRegistry.Default

    internal data class TestTuple(
        @AttributeName("test_1")
        val testValue: String,
        val content: Content
    )

    internal data class Content(
        @AttributeName("test_2")
        val testValue2: Int
    )

    @Test
    fun findAttributeNameFromProperty() {
        val testTupleClass = tupleClassRegistry.getTupleClass(TestTuple::class)

        // Act
        val testValuePropertyAttributeName = testTupleClass.findAttributeNameFromProperty(TestTuple::testValue)

        Assertions.assertEquals("test_1", testValuePropertyAttributeName)
    }

    @Test
    fun findAttributeNameFromProperty_notFound() {
        val testTupleClass = tupleClassRegistry.getTupleClass(TestTuple::class)

        Assertions.assertThrows(RuntimeException::class.java) {
            // Act
            testTupleClass.findAttributeNameFromProperty(TestTuple::content)
        }.also {
            Assertions.assertEquals(it.message, "Attribute name corresponding to `content` property not found")
        }
    }

    @Nested
    internal inner class CreateTupleTest {
        @Test
        internal fun normal() {
            val testTupleClass = tupleClassRegistry.getTupleClass(TestTuple::class)

            val tuple = testTupleClass.createTuple { attributeName, _ ->
                mapOf(
                        "test_1" to "Test",
                        "test_2" to 2
                )[attributeName]
            }

            Assertions.assertEquals(TestTuple("Test", Content(2)), tuple)
        }

        @Test
        internal fun illegalArgumentException() {
            val testTupleClass = tupleClassRegistry.getTupleClass(TestTuple::class)

            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                testTupleClass.createTuple { _, _ -> null }
            }

            Assertions.assertEquals(
                    "Tuple creation failed (constructor : `fun <init>(kotlin.Int): info.vividcode.orm.TupleClassTest.Content`," +
                            " args : `[null]`)",
                    exception.message
            )
            Assertions.assertNotNull(exception.cause)
        }
    }

}
