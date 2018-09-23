package info.vividcode.orm

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class RelationPredicateBuilderTest {

    private data class TestTuple(
            val test1: String,
            val test2: Int
    )

    @Test
    internal fun andOperator() {
        val eqExpression1 = where { TestTuple::test1 eq "test1" }
        val eqExpression2 = where { TestTuple::test2 eq 1 }
        val eqExpression3 = where { TestTuple::test1 eq "test2" }
        val eqExpression4 = where { TestTuple::test2 eq 2 }
        val andExpression = where { (eqExpression1 and eqExpression2) and (eqExpression3 and eqExpression4) }

        Assertions.assertTrue(andExpression is RelationPredicate.And<*>)
        Assertions.assertEquals(
                listOf(eqExpression1, eqExpression2, eqExpression3, eqExpression4),
                (andExpression as RelationPredicate.And<*>).expressions
        )
    }

}
