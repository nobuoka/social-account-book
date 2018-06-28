package info.vividcode.orm.db

import java.math.BigDecimal
import java.sql.*
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

class SimpleColumnValueMapper private constructor(
    private val typeSpecificMappers: Map<KClass<*>, ResultTypeSpecificMapper<*>>
) : ColumnValueMapper {

    @Throws(SQLException::class)
    override fun mapColumnValue(resultSet: ResultSet, columnIndex: Int, expectedType: KType): Any? {
        val jvmErasureType = expectedType.jvmErasure
        val mapper = typeSpecificMappers[jvmErasureType]
        if (mapper != null) {
            return mapper.get(resultSet, columnIndex, expectedType.isMarkedNullable)
        }
        if (jvmErasureType.java.isEnum) {
            val expectedTypeEnum = jvmErasureType.java
            return mapColumnValueToEnumValue(
                resultSet, columnIndex, expectedTypeEnum, expectedTypeEnum.enumConstants, expectedType.isMarkedNullable
            )
        }
        throw RuntimeException("Specified type not supported (type : $expectedType)")
    }

    companion object {
        fun create(): SimpleColumnValueMapper {
            val map = mapOf(
                // -- Boolean --
                Boolean::class to mapper(
                    ResultSet::getBoolean,
                    setOf(Types.BOOLEAN, Types.BIT, Types.TINYINT, Types.SMALLINT, Types.INTEGER)
                ),
                // -- Number --
                Short::class to mapper(ResultSet::getShort, setOf(Types.SMALLINT)),
                Int::class to mapper(ResultSet::getInt, setOf(Types.SMALLINT, Types.INTEGER)),
                Long::class to mapper(ResultSet::getLong, setOf(Types.SMALLINT, Types.INTEGER, Types.BIGINT)),
                // SQL `FLOAT` should be mapped to Kotlin `Double`.
                Double::class to mapper(ResultSet::getDouble, setOf(Types.FLOAT, Types.DOUBLE)),
                Double::class to mapper(ResultSet::getDouble, setOf(Types.FLOAT, Types.DOUBLE)),
                BigDecimal::class to mapper(ResultSet::getBigDecimal, setOf(Types.DECIMAL)),
                // -- Text --
                String::class to mapper(
                    ResultSet::getString,
                    setOf(Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.CLOB)
                ),
                // -- Date and Time --
                Instant::class to mapper(
                    createResultSetGetter(ResultSet::getTimestamp, Timestamp::toInstant),
                    setOf(Types.TIMESTAMP)
                ),
                LocalDate::class to mapper(
                    createResultSetGetter(ResultSet::getDate, Date::toLocalDate),
                    setOf(Types.DATE)
                ),
                YearMonth::class to mapper(
                    createResultSetGetter(ResultSet::getString, YearMonth::parse),
                    setOf(Types.VARCHAR)
                )
            )
            return SimpleColumnValueMapper(ConcurrentHashMap(map))
        }

        private inline fun <T, U> createResultSetGetter(
            crossinline f: ResultSet.(Int) -> U?,
            crossinline g: (U) -> T
        ): (ResultSet, Int) -> T? = { resultSet, columnIndex ->
            f(resultSet, columnIndex)?.let(g)
        }

        private fun <T : Any> mapper(resultSetGetter: (ResultSet, Int) -> T?, acceptableColumnTypes: Set<Int>) =
            ResultTypeSpecificMapper(resultSetGetter, acceptableColumnTypes)

        private val stringTypeMapperForEnum: ResultTypeSpecificMapper<String> =
            ResultTypeSpecificMapper(
                ResultSet::getString, setOf(Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.CLOB, Types.OTHER)
            )

        @Throws(SQLException::class)
        private fun <T> mapColumnValueToEnumValue(
            resultSet: ResultSet,
            i: Int,
            expectedType: Class<*>,
            enumConstants: Array<T>,
            nullable: Boolean
        ): T? = stringTypeMapperForEnum.get(resultSet, i, nullable)?.let { value ->
            enumConstants.find { (it as Enum<*>).name == value }
                    ?: throw RuntimeException(
                        "Enum `${expectedType.name}` doesn't have value `$value`. " +
                                "(enum values : ${enumConstants.joinToString { "`$it`" }})"
                    )
        }

        private class ResultTypeSpecificMapper<T>(
            private val resultSetGetter: (ResultSet, Int) -> T?,
            private val acceptableColumnTypes: Set<Int>
        ) {
            @Throws(SQLException::class)
            fun get(resultSet: ResultSet, columnIndex: Int, nullable: Boolean): T? {
                val columnType = resultSet.metaData.getColumnType(columnIndex)
                if (!acceptableColumnTypes.contains(columnType)) {
                    throw RuntimeException(
                        "DB type is not acceptable " +
                                "(column type: $columnType, acceptable column types: $acceptableColumnTypes)"
                    )
                }

                val b = resultSetGetter(resultSet, columnIndex)
                return if (resultSet.wasNull()) {
                    if (nullable) {
                        null
                    } else {
                        throw RuntimeException(
                            "Although JVM type is defined as non-null, DB value is NULL " +
                                    "(column index: $columnIndex)"
                        )
                    }
                } else {
                    b
                }
            }
        }
    }

}
