package info.vividcode.orm.db

import info.vividcode.orm.*
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

fun <T : Any> createJdbcOrmContext(ormContextInterface: KClass<T>, connection: Connection): T = run {
    val tupleClassRegistry = TupleClassRegistry.Default
    Proxy.newProxyInstance(
        ormContextInterface.java.classLoader,
        arrayOf(ormContextInterface.java),
        OrmContextInvocationHandler(tupleClassRegistry, DbBareRelationRegistry(tupleClassRegistry), connection)
    ).let(ormContextInterface::cast)
}

fun executeQuery(
    connection: Connection,
    sqlCommand: SqlCommand
): ResultSet {
    val s = connection.prepareStatement(sqlCommand.sqlString)
    sqlCommand.sqlValueSetterList.forEachIndexed { index, function -> function(s, index + 1) }
    return s.executeQuery()
}

fun <T : Any> retrieveResult(
    resultSet: ResultSet,
    sqlResultInfo: SqlResultInfo<T>
): Set<T> {
    return resultSet.use { rs ->
        val mappingInfo = sqlResultInfo.tupleClassRegistry.getTupleClass(sqlResultInfo.tupleType)
        val result = mutableSetOf<T>()
        while (rs.next()) {
            val tuple = mapTuple(mappingInfo, SimpleColumnValueMapper.create(), rs)
            result.add(tuple)
        }
        result
    }
}

fun insert(
    connection: Connection,
    relationName: String,
    insertedValue: Any,
    tupleClassRegistry: TupleClassRegistry,
    returnType: KType,
    returnGeneratedKeys: Boolean
): Any {
    val pairs = tupleClassRegistry.withTupleClass(insertedValue, TupleClass<*>::createSqlColumnNameAndValuePairs)

    val columnNames = pairs.map { it.first }
    val values = pairs.map { it.second }
    val s = connection.prepareStatement(
        "INSERT INTO \"$relationName\"" +
                " (${columnNames.joinToString(", ") { "\"$it\"" }})" +
                " VALUES (${values.joinToString(", ") { "?" }})",
        if (returnGeneratedKeys) PreparedStatement.RETURN_GENERATED_KEYS else 0
    )
    values.forEachIndexed { index, any ->
        s.setValueAny(index + 1, any)
    }

    s.executeUpdate()

    val id = if (returnGeneratedKeys) {
        s.generatedKeys.let {
            it.next()
            it.getLong(1)
        }
    } else {
        0L
    }

    return when (returnType.jvmErasure) {
        Long::class -> id
        else -> throw RuntimeException()
    }
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

private fun <T : Any> TupleClass<T>.createSqlColumnNameAndValuePairs(targetValue: T): List<Pair<Any, Any?>> {
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

fun PreparedStatement.setValueAny(index: Int, value: Any?) {
    when (value) {
        is String -> this.setObject(index, value)
        is Int -> this.setInt(index, value)
        is Long -> this.setLong(index, value)
        else -> throw RuntimeException("Unexpected type (value: $value)")
    }
}
