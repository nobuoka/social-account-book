package info.vividcode.orm.onmemory

import info.vividcode.orm.AttributeName
import info.vividcode.orm.where
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class RelationPredicateTest {

    private data class TestTuple(val id: Long, val content: Content) {
        data class Content(
                val test1: String?,
                @AttributeName("test_2")
                val test2: Int?
        )
    }

    @Nested
    internal inner class CheckTest {
        @Test
        internal fun eq_string() {
            val predicate = where {
                TestTuple.Content::test1 eq "test1"
            }

            Assertions.assertFalse(predicate.check(TestTuple.Content("test-1", 20)))
            Assertions.assertTrue(predicate.check(TestTuple.Content("test1", 20)))
        }

        @Test
        internal fun eq_int() {
            val predicate = where {
                TestTuple.Content::test2 eq 100
            }

            Assertions.assertFalse(predicate.check(TestTuple.Content("test1", 20)))
            Assertions.assertTrue(predicate.check(TestTuple.Content("test1", 100)))
        }

        @Test
        internal fun in_int() {
            val predicate = where {
                p(TestTuple.Content::test2) `in` listOf(50, 100)
            }

            Assertions.assertFalse(predicate.check(TestTuple.Content("test1", 20)))
            Assertions.assertTrue(predicate.check(TestTuple.Content("test1", 100)))
        }

        @Test
        internal fun in_int_empty() {
            val predicate = where {
                p(TestTuple.Content::test2) `in` emptyList()
            }

            Assertions.assertFalse(predicate.check(TestTuple.Content("test1", 20)))
        }

        @Test
        internal fun isNull() {
            val predicate = where {
                TestTuple.Content::test1.isNull
            }

            Assertions.assertFalse(predicate.check(TestTuple.Content("test1", 20)))
            Assertions.assertTrue(predicate.check(TestTuple.Content(null, 20)))
        }

        @Test
        internal fun converter() {
            val predicate = where {
                of(TestTuple::content) {
                    TestTuple.Content::test1 eq "test1"
                }
            }

            Assertions.assertFalse(predicate.check(TestTuple(1L, TestTuple.Content(null, 20))))
            Assertions.assertFalse(predicate.check(TestTuple(1L, TestTuple.Content("test-1", 20))))
            Assertions.assertTrue(predicate.check(TestTuple(1L, TestTuple.Content("test1", 20))))
        }
    }

}
