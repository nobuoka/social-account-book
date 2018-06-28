package info.vividcode.orm.db

import info.vividcode.orm.BareRelation
import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.where
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class DbBareRelationTest {

    private data class TestTuple(val id: Long)
    private interface TestRelation1 : BareRelation<TestTuple>

    private interface TestUnsupported {
        fun callUnsupported(): Int
    }

    private interface TestRelation2 : BareRelation<TestTuple>, TestUnsupported

    @Test
    fun createAndObjectMethods() {
        val relation = DbBareRelation.create("test", TestRelation1::class, TestTuple::class, TupleClassRegistry())

        Assertions.assertTrue(relation == relation)
        Assertions.assertDoesNotThrow { relation.hashCode() }
        Assertions.assertDoesNotThrow { relation.toString() }
    }

    @Test
    fun select() {
        val relation = DbBareRelation.create("test", TestRelation1::class, TestTuple::class, TupleClassRegistry())

        val operatedRelation = (relation as TestRelation1).select(where { TestTuple::id eq 1 }) as OperatedRelation<*>

        Assertions.assertEquals(
            "SELECT * FROM \"test\" WHERE \"id\" = ?",
            operatedRelation.sqlCommand.sqlString
        )
        Assertions.assertEquals(TestTuple::class, operatedRelation.sqlResultInfo.tupleType)
    }

    @Test
    fun selectAll() {
        val relation = DbBareRelation.create("test", TestRelation1::class, TestTuple::class, TupleClassRegistry())

        val operatedRelation = relation.selectAll() as OperatedRelation<*>

        Assertions.assertEquals(
            "SELECT FROM \"test\"",
            operatedRelation.sqlCommand.sqlString
        )
        Assertions.assertEquals(TestTuple::class, operatedRelation.sqlResultInfo.tupleType)
    }

    @Test
    fun callUnsupportedMethod() {
        val relation = DbBareRelation.create("test", TestRelation2::class, TestTuple::class, TupleClassRegistry())

        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            // Act
            (relation as TestRelation2).callUnsupported()
        }

        Assertions.assertEquals(
            "The method `public abstract int info.vividcode.orm.db.DbBareRelationTest\$TestUnsupported.callUnsupported()`" +
                    " is unknown. Only methods declared in `Object` class or in `DbBareRelation` class are supported.",
            exception.message
        )
    }

}
