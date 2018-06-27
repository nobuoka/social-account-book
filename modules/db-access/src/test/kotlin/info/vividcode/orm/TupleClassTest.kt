package info.vividcode.orm

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class TupleClassTest {

    private val tupleClassRegistry = TupleClassRegistry.Default

    private data class TestTuple(
        @AttributeName("test_1")
        val testValue: String,
        val content: Content
    )

    private data class Content(
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

}
