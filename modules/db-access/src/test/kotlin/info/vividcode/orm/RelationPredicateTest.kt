package info.vividcode.orm

import info.vividcode.orm.db.toSqlWhereClause
import org.junit.jupiter.api.Assertions
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
        val predicate = whereOf(TestTuple::content) {
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

}
