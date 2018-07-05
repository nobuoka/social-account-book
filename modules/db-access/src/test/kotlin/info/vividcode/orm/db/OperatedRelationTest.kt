package info.vividcode.orm.db

import info.vividcode.orm.BareRelation
import info.vividcode.orm.RelationName
import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.where
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class OperatedRelationTest {

    private val tupleClassRegistry = TupleClassRegistry()
    private val bareRelationRegistry = DbBareRelationRegistry(tupleClassRegistry)

    data class MyTuple(val value: String)

    @RelationName("test")
    interface MyRelation : BareRelation<MyTuple>

    @Test
    fun select() {
        val relation = bareRelationRegistry.getRelationAsRelationType(MyRelation::class)

        val operatedRelation1 = relation.select(where { MyTuple::value eq "hello" })
        if (operatedRelation1 !is OperatedRelation.SimpleRestricted<*>) {
            throw AssertionError("Unexpected type : ${operatedRelation1::class.qualifiedName}")
        }

        Assertions.assertEquals(
            "SELECT * FROM \"test\" WHERE \"value\" = ?",
            operatedRelation1.sqlCommand.sqlString
        )

        val operatedRelation2 = operatedRelation1.select(where { MyTuple::value eq "good" })
        if (operatedRelation2 !is OperatedRelation<*>) {
            throw AssertionError("Unexpected type : ${operatedRelation2::class.qualifiedName}")
        }

        Assertions.assertEquals(
            "SELECT * FROM (SELECT * FROM \"test\" WHERE \"value\" = ?) WHERE \"value\" = ?",
            operatedRelation2.sqlCommand.sqlString
        )
    }

}
