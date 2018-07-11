package info.vividcode.orm.db

import info.vividcode.orm.BareRelation
import info.vividcode.orm.RelationName
import info.vividcode.orm.TupleClassRegistry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class DbBareRelationRegistryTest {

    private val testTupleClassRegistry = TupleClassRegistry()
    private val dbBareRelationRegistry = DbBareRelationRegistry(testTupleClassRegistry)

    private data class TestTuple(val id: String)

    @RelationName("foo")
    private interface TestRelation : BareRelation<TestTuple>

    private interface NoNameRelation : BareRelation<TestTuple>

    @Nested
    inner class RelationAsRelationType : DDD() {
        override fun <T : BareRelation<*>> act(targetRelationClass: KClass<T>): Any =
            dbBareRelationRegistry.getRelationAsRelationType(targetRelationClass)
    }

    @Nested
    inner class RelationAsBareRelationType : DDD() {
        override fun <T : BareRelation<*>> act(targetRelationClass: KClass<T>): Any =
            dbBareRelationRegistry.getRelationAsBareRelationType(targetRelationClass)
    }

    abstract inner class DDD() {
        abstract fun <T: BareRelation<*>> act(targetRelationClass: KClass<T>): Any

        @Test
        fun normalCase() {
            val relation = act(TestRelation::class)

            Assertions.assertTrue(relation is TestRelation)
            if (relation !is DbBareRelation<*>) throw AssertionError("$relation is not ${DbBareRelation::class.simpleName}")

            Assertions.assertEquals("foo", relation.relationName)
            Assertions.assertEquals(TestTuple::class, relation.tupleType)
            Assertions.assertSame(testTupleClassRegistry, relation.tupleClassRegistry)
        }

        @Test
        fun relationNameNotAnnotated() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                act(NoNameRelation::class)
            }

            Assertions.assertEquals(
                "`NoNameRelation` must be annotated with `@RelationName` annotation",
                exception.message
            )
        }
    }

}
