package info.vividcode.orm.db

import info.vividcode.orm.*
import java.lang.Exception
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

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

fun createInsertSqlCommand(
    relationName: String,
    insertedValue: Any,
    tupleClassRegistry: TupleClassRegistry
): SqlCommand {
    val pairs = tupleClassRegistry.withTupleClass(insertedValue, TupleClass<*>::createSqlColumnNameAndValuePairs)

    val columnNames = pairs.map { it.first }
    val values = pairs.map { it.second }

    return SqlCommand(
        "INSERT INTO \"$relationName\"" +
                " (${columnNames.joinToString(", ") { "\"$it\"" }})" +
                " VALUES (${values.joinToString(", ") { "?" }})",
        values.map(::createSqlValueSetter)
    )
}

fun createUpdateSqlCommand(
    relationName: String,
    updateValue: Any,
    predicate: RelationPredicate<*>,
    tupleClassRegistry: TupleClassRegistry
): SqlCommand {
    val pairs = tupleClassRegistry.withTupleClass(updateValue, TupleClass<*>::createSqlColumnNameAndValuePairs)
    val columnNames = pairs.map { it.first }
    val updateValueSetterList = pairs.map { it.second }.map(::createSqlValueSetter)

    val clause = predicate.toSqlWhereClause(tupleClassRegistry)
    val whereClauseOrEmpty = clause.let { "WHERE ${it.whereClauseString}" }

    val sqlString = "UPDATE \"$relationName\"" +
            " SET ${columnNames.joinToString(", ") { "\"$it\" = ?" }}" +
            " $whereClauseOrEmpty"
    val sqlValueSetCallableList = clause.let { updateValueSetterList + it.valueSetterList }

    return SqlCommand(sqlString, sqlValueSetCallableList)
}

fun createDeleteSqlCommand(
    relationName: String,
    predicate: RelationPredicate<*>,
    tupleClassRegistry: TupleClassRegistry
): SqlCommand {
    val clause = predicate.toSqlWhereClause(tupleClassRegistry)
    val whereClauseOrEmpty = clause.let { "WHERE ${it.whereClauseString}" }

    val sqlString = "DELETE FROM \"$relationName\" $whereClauseOrEmpty"
    val sqlValueSetCallableList = clause.valueSetterList

    return SqlCommand(sqlString, sqlValueSetCallableList)
}

private fun createSqlValueSetter(value: Any?): PreparedStatement.(Int) -> Unit = {
    this.setValueAny(it, value)
}

fun insert(
        connection: Connection,
        relationName: String,
        insertedValue: Any,
        tupleClassRegistry: TupleClassRegistry,
        returnGeneratedKeys: Boolean
): Any {
    val insertCommand = createInsertSqlCommand(relationName, insertedValue, tupleClassRegistry)

    val s = connection.prepareStatement(
        insertCommand.sqlString,
        if (returnGeneratedKeys) PreparedStatement.RETURN_GENERATED_KEYS else 0
    )
    insertCommand.sqlValueSetterList.forEachIndexed { index, function -> function(s, index + 1) }

    s.executeUpdate()

    return if (returnGeneratedKeys) {
        listOf(s.generatedKeys.let {
            try {
                it.next()
                it.getLong(1)
            } catch (e: Exception) {
                throw KdbiRuntimeException("Failed to fetch generated keys.", e)
            }
        })
    } else {
        s.updateCount
    }
}

fun update(
        connection: Connection,
        relationName: String,
        updateValue: Any,
        predicate: RelationPredicate<*>,
        tupleClassRegistry: TupleClassRegistry
): Int {
    val updateCommand = createUpdateSqlCommand(relationName, updateValue, predicate, tupleClassRegistry)

    val s = connection.prepareStatement(updateCommand.sqlString)
    updateCommand.sqlValueSetterList.forEachIndexed { index, function -> function(s, index + 1) }

    s.executeUpdate()

    return s.updateCount
}

fun delete(
        connection: Connection,
        relationName: String,
        predicate: RelationPredicate<*>,
        tupleClassRegistry: TupleClassRegistry
): Int {
    val deleteCommand = createDeleteSqlCommand(relationName, predicate, tupleClassRegistry)

    val s = connection.prepareStatement(deleteCommand.sqlString)
    deleteCommand.sqlValueSetterList.forEachIndexed { index, function -> function(s, index + 1) }

    s.executeUpdate()

    return s.updateCount
}

fun <T : Any> mapTuple(aClass: TupleClass<T>, columnValueMapper: ColumnValueMapper, resultSet: ResultSet): T =
        aClass.createTuple { attributeName, returnType ->
            columnValueMapper.mapColumnValue(resultSet, resultSet.findColumn(attributeName), returnType)
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
