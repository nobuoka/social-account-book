package info.vividcode.orm.db

import info.vividcode.orm.Relation
import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.SimpleRestrictedRelation
import info.vividcode.orm.TupleClassRegistry
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.KClass

open class OperatedRelation<T : Any>(
    private val sqlString: String,
    private val sqlValueSetterList: List<PreparedStatement.(Int) -> Unit>,
    private val returnType: KClass<T>,
    private val tupleClassRegistry: TupleClassRegistry
) : Relation<T> {

    class SimpleRestricted<T : Any>(
        sqlString: String, sqlValueSetterList: List<PreparedStatement.(Int) -> Unit>,
        returnType: KClass<T>, tupleClassRegistry: TupleClassRegistry
    ) : OperatedRelation<T>(sqlString, sqlValueSetterList, returnType, tupleClassRegistry),
        SimpleRestrictedRelation<T>

    override fun select(predicate: RelationPredicate<T>): Relation<T> =
        create(predicate, "($sqlString)", sqlValueSetterList, returnType, tupleClassRegistry)

    fun forUpdate() = OperatedRelation(
        "$sqlString FOR UPDATE",
        sqlValueSetterList,
        returnType,
        tupleClassRegistry
    )

    fun toSet(connection: Connection): Set<T> =
        executeQuery(connection, sqlString, sqlValueSetterList)
            .let { retrieveResult(it, returnType, tupleClassRegistry) }

    companion object {
        fun <T : Any> create(
            predicate: RelationPredicate<T>,
            relationSqlString: String,
            relationSqlValueSetterList: List<PreparedStatement.(Int) -> Unit>,
            returnType: KClass<T>,
            tupleClassRegistry: TupleClassRegistry
        ): OperatedRelation<T> {
            val clause = predicate.toSqlWhereClause(tupleClassRegistry)

            val whereClauseOrEmpty = clause.let { "WHERE ${it.whereClauseString}" }
            val newSqlString = "SELECT * FROM $relationSqlString $whereClauseOrEmpty"
            val newSqlValueSetCallableList = clause.let { relationSqlValueSetterList + it.valueSetProcess }
            return OperatedRelation(
                newSqlString,
                newSqlValueSetCallableList,
                returnType,
                tupleClassRegistry
            )
        }

        fun <T : Any> createSimpleRestricted(
            predicate: RelationPredicate<T>,
            relationSqlString: String,
            relationSqlValueSetterList: List<PreparedStatement.(Int) -> Unit>,
            returnType: KClass<T>,
            tupleClassRegistry: TupleClassRegistry
        ): SimpleRestricted<T> {
            val clause = predicate.toSqlWhereClause(tupleClassRegistry)

            val whereClauseOrEmpty = clause.let { "WHERE ${it.whereClauseString}" }
            val newSqlString = "SELECT * FROM $relationSqlString $whereClauseOrEmpty"
            val newSqlValueSetCallableList = clause.let { relationSqlValueSetterList + it.valueSetProcess }
            return SimpleRestricted(newSqlString, newSqlValueSetCallableList, returnType, tupleClassRegistry)
        }
    }

}
