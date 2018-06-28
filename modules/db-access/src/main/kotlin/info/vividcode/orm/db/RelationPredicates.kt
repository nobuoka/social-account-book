package info.vividcode.orm.db

import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.TupleClassRegistry
import java.sql.PreparedStatement

class WhereClause(val whereClauseString: String, val valueSetterList: List<PreparedStatement.(Int) -> Unit>)

fun <T : Any> RelationPredicate<T>.toSqlWhereClause(mappingInfoRegistry: TupleClassRegistry): WhereClause =
    when (this) {
        is RelationPredicate.Eq<T, *> -> {
            val columnName = mappingInfoRegistry.getTupleClass(this.type).findAttributeNameFromProperty(this.property)
            WhereClause(
                "\"$columnName\" = ?",
                listOf(valueSetter(this@toSqlWhereClause.value))
            )
        }
        is RelationPredicate.Converter<T, *> -> this.condition.toSqlWhereClause(mappingInfoRegistry)
    }

private fun <T : Any> valueSetter(value: T): PreparedStatement.(Int) -> Unit {
    return when (value) {
        is String -> { index -> setString(index, value) }
        is Long -> { index -> setLong(index, value) }
        is Int -> { index -> setInt(index, value) }
        else -> throw RuntimeException("Unknown value type (value : `$value`, its type : `${value::class}`)")
    }
}
