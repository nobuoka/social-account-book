package info.vividcode.orm.db

import info.vividcode.orm.AttributeName
import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.where
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.sql.PreparedStatement

internal class RelationPredicateTest {

    private val tupleClassRegistry = TupleClassRegistry()

    private data class TestTuple(val id: Long, val content: Content) {
        data class Content(
            val test1: String,
            @AttributeName("test_2")
            val test2: Int
        )
    }

    @Test
    fun whereOf() {
        val predicate = info.vividcode.orm.whereOf(TestTuple::content) {
            TestTuple.Content::test2 eq 100
        }

        val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
        Assertions.assertEquals("\"test_2\" = ?", sqlWhereClause.whereClauseString)
        Assertions.assertEquals(1, sqlWhereClause.valueSetterList.size)

        val preparedStatement = Mockito.mock(PreparedStatement::class.java)
        sqlWhereClause.valueSetterList[0].invoke(preparedStatement, 1)
        Mockito.verify(preparedStatement, Mockito.times(1)).setInt(1, 100)
        Mockito.verifyNoMoreInteractions(preparedStatement)
    }

    @Test
    fun eq_int() {
        val predicate = where {
            TestTuple.Content::test2 eq 100
        }

        val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
        Assertions.assertEquals("\"test_2\" = ?", sqlWhereClause.whereClauseString)
        Assertions.assertEquals(1, sqlWhereClause.valueSetterList.size)

        val preparedStatement = Mockito.mock(PreparedStatement::class.java)
        sqlWhereClause.valueSetterList[0].invoke(preparedStatement, 1)
        Mockito.verify(preparedStatement, Mockito.times(1)).setInt(1, 100)
        Mockito.verifyNoMoreInteractions(preparedStatement)
    }

    @Test
    fun eq_long() {
        val predicate = where {
            TestTuple::id eq 100
        }

        val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
        Assertions.assertEquals("\"id\" = ?", sqlWhereClause.whereClauseString)
        Assertions.assertEquals(1, sqlWhereClause.valueSetterList.size)

        val preparedStatement = Mockito.mock(PreparedStatement::class.java)
        sqlWhereClause.valueSetterList[0].invoke(preparedStatement, 1)
        Mockito.verify(preparedStatement, Mockito.times(1)).setLong(1, 100)
        Mockito.verifyNoMoreInteractions(preparedStatement)
    }

    @Test
    fun eq_string() {
        val predicate = where {
            TestTuple.Content::test1 eq "Hello world"
        }

        val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
        Assertions.assertEquals("\"test1\" = ?", sqlWhereClause.whereClauseString)
        Assertions.assertEquals(1, sqlWhereClause.valueSetterList.size)

        val preparedStatement = Mockito.mock(PreparedStatement::class.java)
        sqlWhereClause.valueSetterList[0].invoke(preparedStatement, 1)
        Mockito.verify(preparedStatement, Mockito.times(1)).setString(1, "Hello world")
        Mockito.verifyNoMoreInteractions(preparedStatement)
    }

    @Nested
    internal inner class InCondition {
        private val sqlConditionAlwaysFalse = "1 <> 1"

        @Test
        internal fun int_singleElement() {
            val predicate = where { p(TestTuple.Content::test2) `in` listOf(100) }

            val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
            Assertions.assertEquals("\"test_2\" IN (?)", sqlWhereClause.whereClauseString)
            Assertions.assertEquals(1, sqlWhereClause.valueSetterList.size)

            val preparedStatement = Mockito.mock(PreparedStatement::class.java)
            invokeValueSetterList(preparedStatement, sqlWhereClause.valueSetterList)
            Mockito.verify(preparedStatement, Mockito.times(1)).setInt(1, 100)
            Mockito.verifyNoMoreInteractions(preparedStatement)
        }

        @Test
        internal fun int_multipleElements() {
            val predicate = where { p(TestTuple.Content::test2) `in` listOf(100, 200, 300) }

            val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
            Assertions.assertEquals("\"test_2\" IN (?,?,?)", sqlWhereClause.whereClauseString)
            Assertions.assertEquals(3, sqlWhereClause.valueSetterList.size)

            val preparedStatement = Mockito.mock(PreparedStatement::class.java)
            invokeValueSetterList(preparedStatement, sqlWhereClause.valueSetterList)
            Mockito.verify(preparedStatement, Mockito.times(1)).setInt(1, 100)
            Mockito.verify(preparedStatement, Mockito.times(1)).setInt(2, 200)
            Mockito.verify(preparedStatement, Mockito.times(1)).setInt(3, 300)
            Mockito.verifyNoMoreInteractions(preparedStatement)
        }

        @Test
        internal fun int_empty() {
            val predicate = where { p(TestTuple.Content::test2) `in` emptyList() }

            val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
            Assertions.assertEquals(sqlConditionAlwaysFalse, sqlWhereClause.whereClauseString)
            Assertions.assertEquals(0, sqlWhereClause.valueSetterList.size)

            val preparedStatement = Mockito.mock(PreparedStatement::class.java)
            invokeValueSetterList(preparedStatement, sqlWhereClause.valueSetterList)
            Mockito.verifyNoMoreInteractions(preparedStatement)
        }

        @Test
        internal fun long_singleElement() {
            val predicate = where { p(TestTuple::id) `in` listOf(100L) }

            val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
            Assertions.assertEquals("\"id\" IN (?)", sqlWhereClause.whereClauseString)
            Assertions.assertEquals(1, sqlWhereClause.valueSetterList.size)

            val preparedStatement = Mockito.mock(PreparedStatement::class.java)
            invokeValueSetterList(preparedStatement, sqlWhereClause.valueSetterList)
            Mockito.verify(preparedStatement, Mockito.times(1)).setLong(1, 100L)
            Mockito.verifyNoMoreInteractions(preparedStatement)
        }

        @Test
        internal fun long_multipleElements() {
            val predicate = where { p(TestTuple::id) `in` listOf(100L, 200L, 300L) }

            val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
            Assertions.assertEquals("\"id\" IN (?,?,?)", sqlWhereClause.whereClauseString)
            Assertions.assertEquals(3, sqlWhereClause.valueSetterList.size)

            val preparedStatement = Mockito.mock(PreparedStatement::class.java)
            invokeValueSetterList(preparedStatement, sqlWhereClause.valueSetterList)
            Mockito.verify(preparedStatement, Mockito.times(1)).setLong(1, 100)
            Mockito.verify(preparedStatement, Mockito.times(1)).setLong(2, 200)
            Mockito.verify(preparedStatement, Mockito.times(1)).setLong(3, 300)
            Mockito.verifyNoMoreInteractions(preparedStatement)
        }

        @Test
        internal fun long_empty() {
            val predicate = where { p(TestTuple::id) `in` emptyList() }

            val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
            Assertions.assertEquals(sqlConditionAlwaysFalse, sqlWhereClause.whereClauseString)
            Assertions.assertEquals(0, sqlWhereClause.valueSetterList.size)

            val preparedStatement = Mockito.mock(PreparedStatement::class.java)
            invokeValueSetterList(preparedStatement, sqlWhereClause.valueSetterList)
            Mockito.verifyNoMoreInteractions(preparedStatement)
        }

        @Test
        internal fun string_singleElement() {
            val predicate = where { p(TestTuple.Content::test1) `in` listOf("A") }

            val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
            Assertions.assertEquals("\"test1\" IN (?)", sqlWhereClause.whereClauseString)
            Assertions.assertEquals(1, sqlWhereClause.valueSetterList.size)

            val preparedStatement = Mockito.mock(PreparedStatement::class.java)
            invokeValueSetterList(preparedStatement, sqlWhereClause.valueSetterList)
            Mockito.verify(preparedStatement, Mockito.times(1)).setString(1, "A")
            Mockito.verifyNoMoreInteractions(preparedStatement)
        }

        @Test
        internal fun string_multipleElements() {
            val predicate = where { p(TestTuple.Content::test1) `in` listOf("A", "B", "C") }

            val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
            Assertions.assertEquals("\"test1\" IN (?,?,?)", sqlWhereClause.whereClauseString)
            Assertions.assertEquals(3, sqlWhereClause.valueSetterList.size)

            val preparedStatement = Mockito.mock(PreparedStatement::class.java)
            invokeValueSetterList(preparedStatement, sqlWhereClause.valueSetterList)
            Mockito.verify(preparedStatement, Mockito.times(1)).setString(1, "A")
            Mockito.verify(preparedStatement, Mockito.times(1)).setString(2, "B")
            Mockito.verify(preparedStatement, Mockito.times(1)).setString(3, "C")
            Mockito.verifyNoMoreInteractions(preparedStatement)
        }

        @Test
        internal fun string_empty() {
            val predicate = where { p(TestTuple.Content::test1) `in` emptyList() }

            val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
            Assertions.assertEquals(sqlConditionAlwaysFalse, sqlWhereClause.whereClauseString)
            Assertions.assertEquals(0, sqlWhereClause.valueSetterList.size)

            val preparedStatement = Mockito.mock(PreparedStatement::class.java)
            invokeValueSetterList(preparedStatement, sqlWhereClause.valueSetterList)
            Mockito.verifyNoMoreInteractions(preparedStatement)
        }
    }

    @Test
    fun isNull() {
        val predicate = where {
            TestTuple.Content::test1.isNull
        }

        val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
        Assertions.assertEquals("\"test1\" IS NULL", sqlWhereClause.whereClauseString)
        Assertions.assertEquals(0, sqlWhereClause.valueSetterList.size)
    }

    @Test
    internal fun andOperator() {
        val predicate = where {
            (TestTuple::id eq 1L) and
                    of(TestTuple::content) { TestTuple.Content::test1.isNull } and
                    of(TestTuple::content) { TestTuple.Content::test2 eq 20 }
        }

        val sqlWhereClause = predicate.toSqlWhereClause(tupleClassRegistry)
        Assertions.assertEquals("(\"id\" = ?) AND (\"test1\" IS NULL) AND (\"test_2\" = ?)", sqlWhereClause.whereClauseString)

        val preparedStatement = Mockito.mock(PreparedStatement::class.java)
        invokeValueSetterList(preparedStatement, sqlWhereClause.valueSetterList)
        Mockito.verify(preparedStatement, Mockito.times(1)).setLong(1, 1L)
        Mockito.verify(preparedStatement, Mockito.times(1)).setInt(2, 20)
        Mockito.verifyNoMoreInteractions(preparedStatement)
    }

    private fun invokeValueSetterList(
        preparedStatement: PreparedStatement, valueSetterList: List<PreparedStatement.(Int) -> Unit>
    ) {
        valueSetterList.forEachIndexed { index, function -> function.invoke(preparedStatement, index + 1) }
    }

}
