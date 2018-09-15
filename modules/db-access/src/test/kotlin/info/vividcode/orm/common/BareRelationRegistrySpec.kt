package info.vividcode.orm.common

import info.vividcode.orm.BareRelation
import info.vividcode.orm.RelationName
import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.db.DbBareRelation
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal abstract class BareRelationRegistrySpec<T : Any>(
        bareRelationRegistryFactory: (TupleClassRegistry) -> BareRelationRegistry<T>
) {

    private val testTupleClassRegistry = TupleClassRegistry()
    private val bareRelationRegistry = bareRelationRegistryFactory(testTupleClassRegistry)

    private data class TestTuple(val id: String)

    @RelationName("foo")
    private interface TestRelation : BareRelation<TestTuple>

    private interface NoNameRelation : BareRelation<TestTuple>

    @Nested
    inner class RelationAsRelationType : AbstractTest() {
        override fun <T : BareRelation<*>> act(targetRelationClass: KClass<T>): Any =
            bareRelationRegistry.getRelationAsRelationType(targetRelationClass)
    }

    @Nested
    inner class RelationAsBareRelationType : AbstractTest() {
        override fun <T : BareRelation<*>> act(targetRelationClass: KClass<T>): Any =
            bareRelationRegistry.getRelationAsBareRelationType(targetRelationClass)
    }

    abstract inner class AbstractTest {
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