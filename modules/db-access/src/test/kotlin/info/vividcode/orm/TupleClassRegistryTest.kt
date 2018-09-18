package info.vividcode.orm

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.jvm.jvmErasure

internal class TupleClassRegistryTest {

    private val tupleClassRegistry = TupleClassRegistry()

    private data class TestTuple(
        val test: String,
        @AttributeName("foo")
        val test2: Int,
        val content: Content
    ) {
        data class Content(
            val test3: String
        )
    }

    private interface TestInterface

    @Test
    fun getTupleClass() {
        // Act
        val tupleClass = tupleClassRegistry.getTupleClass(TestTuple::class)

        Assertions.assertEquals(TestTuple::class, tupleClass.constructor.returnType.jvmErasure)
        Assertions.assertEquals(3, tupleClass.members.size)

        tupleClass.members[0].let {
            Assertions.assertEquals("test", it.memberName)
            Assertions.assertEquals(TupleClassMember.CounterpartToSingleAttribute::class, it::class)
            Assertions.assertEquals("test", (it as TupleClassMember.CounterpartToSingleAttribute).attributeName)
        }

        tupleClass.members[1].let {
            Assertions.assertEquals("test2", it.memberName)
            Assertions.assertEquals(TupleClassMember.CounterpartToSingleAttribute::class, it::class)
            Assertions.assertEquals("foo", (it as TupleClassMember.CounterpartToSingleAttribute).attributeName)
        }

        tupleClass.members[2].let {
            Assertions.assertEquals("content", it.memberName)
            Assertions.assertEquals(TupleClassMember.CounterpartToMultipleAttributes::class, it::class)
            val contentClass = tupleClassRegistry.getTupleClass(TestTuple.Content::class)
            Assertions.assertEquals(
                contentClass,
                (it as TupleClassMember.CounterpartToMultipleAttributes<*, *>).subAttributeValues
            )
        }
    }

    @Test
    fun getTupleClass_noPrimaryConstructor() {
        Assertions.assertThrows(RuntimeException::class.java) {
            // Act
            tupleClassRegistry.getTupleClass(TestInterface::class)
        }.also {
            Assertions.assertEquals("The `TestInterface` class not have primary constructor.", it.message)
        }
    }

    @Nested
    internal inner class CreateAttributesMapTest {
        @Test
        fun normal() {
            data class TestTuple(val value1: String, val value2: Int)
            val testValue = TestTuple("Test", 2)

            val attributesMap = TupleClassRegistry.createAttributesMap(testValue)

            Assertions.assertEquals(mapOf("value1" to "Test", "value2" to 2), attributesMap)
        }

        @Test
        fun withoutProperty() {
            class TestTuple(value1: String)
            val testValue = TestTuple("Test")

            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                TupleClassRegistry.createAttributesMap(testValue)
            }

            Assertions.assertEquals("There is no member property which is named `value1`", exception.message)
        }
    }

}
