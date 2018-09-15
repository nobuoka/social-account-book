package info.vividcode.orm.db

import info.vividcode.orm.Relation
import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.SimpleRestrictedRelation
import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.common.OperatedRelationImplementation
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.KClass

open class OperatedRelation<T : Any>(
    val sqlCommand: SqlCommand,
    val sqlResultInfo: SqlResultInfo<T>
) : OperatedRelationImplementation<Connection, T> {

    class SimpleRestricted<T : Any> internal constructor(
        sqlCommand: SqlCommand,
        sqlResultInfo: SqlResultInfo<T>
    ) : OperatedRelation<T>(sqlCommand, sqlResultInfo), SimpleRestrictedRelation<T>

    override fun select(predicate: RelationPredicate<T>): Relation<T> =
        create(
            predicate,
            "(${sqlCommand.sqlString})",
            sqlCommand.sqlValueSetterList,
            sqlResultInfo.tupleType,
            sqlResultInfo.tupleClassRegistry
        )

    override fun forUpdate() = OperatedRelation(
        SqlCommand("${sqlCommand.sqlString} FOR UPDATE", sqlCommand.sqlValueSetterList),
        sqlResultInfo
    )

    override fun toSet(connection: Connection): Set<T> =
        executeQuery(connection, sqlCommand).let { retrieveResult(it, sqlResultInfo) }

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
            val newSqlValueSetCallableList = clause.let { relationSqlValueSetterList + it.valueSetterList }
            return OperatedRelation(
                SqlCommand(newSqlString, newSqlValueSetCallableList),
                SqlResultInfo(returnType, tupleClassRegistry)
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
            val newSqlValueSetCallableList = clause.let { relationSqlValueSetterList + it.valueSetterList }
            return SimpleRestricted(
                SqlCommand(newSqlString, newSqlValueSetCallableList),
                SqlResultInfo(returnType, tupleClassRegistry)
            )
        }
    }

}
