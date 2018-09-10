package info.vividcode.orm.deprecated

import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.db.SimpleColumnValueMapper
import info.vividcode.orm.db.mapTuple
import info.vividcode.orm.db.toSqlWhereClause
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun <T : Any> createJdbcRelation(targetClass: KClass<T>, relationName: String, connection: Connection): T =
    Proxy.newProxyInstance(
        targetClass.java.classLoader,
        arrayOf(targetClass.java),
        RelationAccessInvocationHandler(targetClass, relationName, connection)
    ) as T

class RelationAbstraction<T : Any>(
    private val sqlString: String,
    private val sqlValueSetterList: List<PreparedStatement.(Int) -> Unit>,
    private val returnType: KClass<T>,
    private val tupleClassRegistry: TupleClassRegistry
) {
    fun select(predicate: RelationPredicate<T>): RelationAbstraction<T> =
        select(returnType, predicate, sqlString, sqlValueSetterList, tupleClassRegistry)

    fun toSet(connection: Connection): Set<T> =
        executeQueryDeprecated(connection, sqlString, sqlValueSetterList, returnType, tupleClassRegistry)
}

fun <T : Any> executeQueryDeprecated(
    connection: Connection,
    sqlString: String,
    sqlValueSetterList: List<PreparedStatement.(Int) -> Unit>?,
    tupleType: KClass<T>,
    tupleClassRegistry: TupleClassRegistry
): Set<T> {
    val s = connection.prepareStatement(sqlString)
    sqlValueSetterList?.forEachIndexed { index, function -> function(s, index + 1) }
    val resultSet = s.executeQuery()
    return resultSet.use { rs ->
        val mappingInfo = tupleClassRegistry.getTupleClass(tupleType)
        val result = mutableSetOf<T>()
        while (rs.next()) {
            val tuple = mapTuple(mappingInfo, SimpleColumnValueMapper.create(), rs)
            result.add(tuple)
        }
        result
    }
}

fun insert(relationName: String, connection: Connection, member: KCallable<*>, args: Array<out Any>?): Any =
    info.vividcode.orm.db.insert(
        connection,
        relationName,
        args?.get(0)!!,
        TupleClassRegistry.Default,
        member.returnType,
        true
    )

fun <T : Any> select(
    returnType: KClass<T>,
    searchCondition: RelationPredicate<T>?,
    relationSqlString: String,
    relationSqlValueSetterList: List<PreparedStatement.(Int) -> Unit>,
    tupleClassRegistry: TupleClassRegistry
): RelationAbstraction<T> {
    val clause = searchCondition?.toSqlWhereClause(tupleClassRegistry)

    val whereClauseOrEmpty = clause?.let { "WHERE ${it.whereClauseString}" } ?: ""
    val newSqlString = "SELECT * FROM $relationSqlString $whereClauseOrEmpty"
    val newSqlValueSetCallableList =
        clause?.let { relationSqlValueSetterList + it.valueSetterList } ?: relationSqlValueSetterList
    return RelationAbstraction(newSqlString, newSqlValueSetCallableList, returnType, tupleClassRegistry)
}
