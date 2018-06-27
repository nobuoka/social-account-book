package info.vividcode.orm

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

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

        Assertions.assertEquals(TestTuple::class, tupleClass.type)
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

}
