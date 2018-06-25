package info.vividcode.orm.db

import info.vividcode.orm.*
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

@Suppress("UNCHECKED_CAST")
fun <T : Any> createJdbcRelation(targetClass: KClass<T>, relationName: String, connection: Connection): T =
    Proxy.newProxyInstance(
        targetClass.java.classLoader,
        arrayOf(targetClass.java),
        RelationAccessInvocationHandler(targetClass, relationName, connection)
    ) as T

sealed class RelationImpl<T : Any> : Relation<T> {

    //abstract fun toEmbeddedSqlString(): String

    class Simple<T : Any>(
        private val sqlString: String,
        private val sqlValueSetProcesses: List<PreparedStatement.(Int) -> Unit>,
        private val connection: Connection,
        private val returnType: KClass<T>,
        private val tupleClassRegistry: TupleClassRegistry
    ) : RelationImpl<T>() {
        override fun select(predicate: RelationPredicate<T>): Relation<T> {
            val clause = predicate.toSqlWhereClause(tupleClassRegistry)

            val whereClauseOrEmpty = clause.let { "WHERE ${it.whereClauseString}" }
            val sqlString = "SELECT * FROM ($sqlString) $whereClauseOrEmpty"
            val sqlValueSetProcesses = this.sqlValueSetProcesses + clause.valueSetProcess
            return RelationImpl.Simple(sqlString, sqlValueSetProcesses, connection, returnType, tupleClassRegistry)
        }

        override fun toSet(): Set<T> {
            return executeQuery(sqlString, sqlValueSetProcesses, connection, returnType, tupleClassRegistry) as Set<T>
        }
    }

}

fun <T : Any> select(
    relationName: String,
    connection: Connection,
    returnType: KClass<T>,
    searchCondition: RelationPredicate<T>?
): Relation<*> {
    val tupleClassRegistry = TupleClassRegistry.Default
    val clause = searchCondition?.toSqlWhereClause(tupleClassRegistry)

    val whereClauseOrEmpty = clause?.let { "WHERE ${it.whereClauseString}" } ?: ""
    val sqlString = "SELECT * FROM \"$relationName\" $whereClauseOrEmpty"
    val sqlValueSetProcesses = clause?.valueSetProcess
    return RelationImpl.Simple(sqlString, sqlValueSetProcesses ?: emptyList(), connection, returnType, tupleClassRegistry)
}

fun <T : Any> executeQuery(
    sqlString: String,
    sqlValueSetProcesses: List<PreparedStatement.(Int) -> Unit>?,
    connection: Connection,
    returnType: KClass<T>,
    tupleClassRegistry: TupleClassRegistry
): Set<T> {
    val s = connection.prepareStatement(sqlString)
    sqlValueSetProcesses?.forEachIndexed { index, function -> function(s, index + 1) }
    val resultSet = s.executeQuery()
    return resultSet.use { rs ->
        //        when (returnType.jvmErasure) {
//            Relation::class -> run {
        val tupleType = returnType //.arguments.first().type!!.jvmErasure //.first().upperBounds.first().jvmErasure
        val mappingInfo = tupleClassRegistry.getTupleClass(tupleType)
        val result = mutableSetOf<Any>()
        //if (ResultSetMapper.Factory.accept(tupleType)) {
            //val mapper = ResultSetMapper.Factory.mapperFor(tupleType)
            while (rs.next()) {
                //mapper.map(rs)
                val tuple = mapTuple(mappingInfo, DefaultColumnValueMapper.create(), rs)
                result.add(tuple)
            }
        //}
        result
//            }
//            else -> throw RuntimeException("Return type unexpected ($returnType)")
//        }
    } as Set<T>
}

fun <T : Any> mapTuple(aClass: TupleClass<T>, columnValueMapper: ColumnValueMapper, resultSet: ResultSet): T {
    val args = aClass.members.map {
        when (it) {
            is TupleClassMember.CounterpartToSingleAttribute -> columnValueMapper.mapColumnValue(
                resultSet, resultSet.findColumn(it.attributeName), it.property.returnType
            )
            is TupleClassMember.CounterpartToMultipleAttributes<T, *> -> mapTuple(
                it.subAttributeValues,
                columnValueMapper,
                resultSet
            )
        }
    }
    return aClass.type.primaryConstructor?.call(*args.toTypedArray())!!
}

fun <T : Any> TupleClass<T>.createSqlColumnNameAndValuePairs(targetValue: T): List<Pair<Any, Any?>> {
    return this.members.flatMap {
        when (it) {
            is TupleClassMember.CounterpartToSingleAttribute -> listOf(it.attributeName to it.property(targetValue))
            is TupleClassMember.CounterpartToMultipleAttributes<T, *> -> it.createSqlColumnNameAndValuePairsInternal(
                targetValue
            )
        }
    }
}

private fun <T : Any, R : Any> TupleClassMember.CounterpartToMultipleAttributes<T, R>.createSqlColumnNameAndValuePairsInternal(
    targetValue: T
): List<Pair<Any, Any?>> {
    val converted = this.property(targetValue)
    return this.subAttributeValues.createSqlColumnNameAndValuePairs(converted)
}

fun insert(relationName: String, connection: Connection, member: KCallable<*>, args: Array<out Any>?): Any {
    val tupleClassRegistry = TupleClassRegistry.Default

    //val targetType = member.valueParameters.first().type.jvmErasure
    //val properties = targetType.declaredMemberProperties
    //val columnNames = properties.map { it.findAnnotation<AttributeName>()?.name ?: it.name }.map { "\"$it\"" }
    //val values = properties.map { p -> p.get(targetValue) }

    val targetValue = args?.get(0)!!
    val pairs = tupleClassRegistry.withTupleClass(targetValue, TupleClass<*>::createSqlColumnNameAndValuePairs)
    val columnNames = pairs.map { it.first }
    val values = pairs.map { it.second }
    println("==== $pairs ====")

    val s = connection.prepareStatement(
        "INSERT INTO \"$relationName\"" +
                " (${columnNames.joinToString(", ") { "\"$it\"" }})" +
                " VALUES (${values.joinToString(", ") { "?" }})",
        PreparedStatement.RETURN_GENERATED_KEYS
    )
    values.forEachIndexed { index, any ->
        s.setValueAny(index + 1, any)
    }
    s.executeUpdate()
    val id = s.generatedKeys.let {
        it.next()
        it.getLong(1)
    }

    return when (member.returnType.jvmErasure) {
        Long::class -> id
        else -> throw RuntimeException()
    }
}

fun PreparedStatement.setValueAny(index: Int, value: Any?) {
    when (value) {
        is String -> this.setObject(index, value)
        is Int -> this.setInt(index, value)
        is Long -> this.setLong(index, value)
        else -> throw RuntimeException("Unexpected type (value: $value)")
    }
}
