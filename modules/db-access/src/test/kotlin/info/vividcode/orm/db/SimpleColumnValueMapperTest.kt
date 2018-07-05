package info.vividcode.orm.db

import org.junit.jupiter.api.*
import org.mockito.Mockito
import java.sql.*
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

internal class SimpleColumnValueMapperTest {

    private val columnValueMapper = SimpleColumnValueMapper.create()

    private val testColumnIndex = 1

    private val testResultSet: ResultSet =
        Mockito.mock(ResultSet::class.java) { throw RuntimeException("Unexpected method call ($it)") }
    private val testResultSetMetaData: ResultSetMetaData =
        Mockito.mock(ResultSetMetaData::class.java) { throw RuntimeException("Unexpected method call ($it)") }

    @BeforeEach
    fun setupResultSetMetaData() {
        Mockito.doReturn(testResultSetMetaData).`when`(testResultSet).metaData
    }

    @AfterEach
    fun verifyMocksNoMoreInteractions() {
        testResultSet.metaData
        Mockito.verify(testResultSet, Mockito.atLeastOnce()).metaData
        Mockito.verifyNoMoreInteractions(testResultSet, testResultSetMetaData)
    }

    open inner class MapColumnValueSpecificType<T : Any, R : Any>(
        private val sqlType: Int, private val sqlResultGetter: ResultSet.(Int) -> T,
        private val resultType: KClass<R>,
        private val resultValuePair: Pair<T, R>,
        private val resultValueDefaultOnNull: T?
    ) {
        @Test
        fun normal() {
            val result =
                withResultSetMock(
                    testColumnIndex,
                    sqlType,
                    sqlResultGetter,
                    columnValue = resultValuePair.first,
                    wasNull = false
                ) {
                    // Act
                    columnValueMapper.mapColumnValue(testResultSet, testColumnIndex, resultType.createType())
                }

            Assertions.assertEquals(resultValuePair.second, result)
        }

        @Test
        fun nullValue() {
            val result =
                withResultSetMock(
                    testColumnIndex,
                    sqlType,
                    sqlResultGetter,
                    columnValue = resultValueDefaultOnNull,
                    wasNull = true
                ) {
                    // Act
                    columnValueMapper.mapColumnValue(
                        testResultSet, testColumnIndex, resultType.createType(nullable = true)
                    )
                }

            Assertions.assertNull(result)
        }

        @Test
        fun nullNotAllowed() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withResultSetMock(
                    testColumnIndex,
                    sqlType,
                    sqlResultGetter,
                    columnValue = resultValueDefaultOnNull,
                    wasNull = true
                ) {
                    // Act
                    columnValueMapper.mapColumnValue(testResultSet, testColumnIndex, resultType.createType())
                }
            }

            Assertions.assertEquals(
                "Although JVM type is defined as non-null, DB value is NULL (column index: 1)",
                exception.message
            )
        }
    }

    @Nested
    inner class MapColumnValueBoolean :
        MapColumnValueSpecificType<Boolean, Boolean>(
            Types.BOOLEAN,
            ResultSet::getBoolean,
            Boolean::class,
            true to true,
            false
        )

    @Nested
    inner class MapColumnValueInt :
        MapColumnValueSpecificType<Int, Int>(Types.INTEGER, ResultSet::getInt, Int::class, 20 to 20, 0)

    @Nested
    inner class MapColumnValueLong :
        MapColumnValueSpecificType<Long, Long>(Types.BIGINT, ResultSet::getLong, Long::class, 20L to 20L, 0L)

    @Nested
    inner class MapColumnValueDouble :
        MapColumnValueSpecificType<Double, Double>(Types.FLOAT, ResultSet::getDouble, Double::class, 20.5 to 20.5, 0.0)

    @Nested
    inner class MapColumnValueText :
        MapColumnValueSpecificType<String, String>(
            Types.VARCHAR,
            ResultSet::getString,
            String::class,
            "This is test" to "This is test",
            null
        )

    @Nested
    inner class MapColumnValueTimestamp :
        MapColumnValueSpecificType<Timestamp, Instant>(
            Types.TIMESTAMP,
            ResultSet::getTimestamp,
            Instant::class,
            Timestamp.from(Instant.parse("2017-04-05T01:02:03Z")) to Instant.parse("2017-04-05T01:02:03Z"),
            null
        )

    @Nested
    inner class MapColumnValueDate :
        MapColumnValueSpecificType<Date, LocalDate>(
            Types.DATE,
            ResultSet::getDate,
            LocalDate::class,
            Date.valueOf(LocalDate.of(2017, 4, 5)) to LocalDate.of(2017, 4, 5),
            null
        )

    @Nested
    inner class MapColumnValueYearMonth :
        MapColumnValueSpecificType<String, YearMonth>(
            Types.VARCHAR,
            ResultSet::getString,
            YearMonth::class,
            "2017-04" to YearMonth.of(2017, 4),
            null
        )

    internal enum class TestEnum {
        FOO, BAR
    }

    @Nested
    inner class MapColumnValueEnum :
        MapColumnValueSpecificType<String, TestEnum>(
            Types.VARCHAR,
            ResultSet::getString,
            TestEnum::class,
            "FOO" to TestEnum.FOO,
            null
        )

    @Test
    fun notAcceptableDbType() {
        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            withResultSetMock(
                testColumnIndex,
                Types.INTEGER,
                ResultSet::getInt,
                columnValue = 100,
                wasNull = false
            ) {
                // Act
                columnValueMapper.mapColumnValue(testResultSet, testColumnIndex, String::class.createType())
            }
        }

        Assertions.assertEquals(
            "DB type is not acceptable (column type: 4, acceptable column types: [1, 12, -1, 2005])",
            exception.message
        )

        Mockito.verify(testResultSetMetaData, Mockito.times(1)).getColumnType(Mockito.eq(testColumnIndex))
    }

    @Test
    fun notSupportedEnumValue() {
        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            withResultSetMock(
                testColumnIndex,
                Types.VARCHAR,
                ResultSet::getString,
                columnValue = "TEST_VALUE",
                wasNull = false
            ) {
                // Act
                columnValueMapper.mapColumnValue(testResultSet, testColumnIndex, TestEnum::class.createType())
            }
        }

        Assertions.assertEquals(
            "Enum `info.vividcode.orm.db.SimpleColumnValueMapperTest\$TestEnum` doesn't " +
                    "have value `TEST_VALUE`. (enum values : `FOO`, `BAR`)",
            exception.message
        )
    }

    @Test
    fun notSupportedType() {
        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            columnValueMapper.mapColumnValue(testResultSet, testColumnIndex, Object::class.createType())
        }

        Assertions.assertEquals("Specified type not supported (type : kotlin.Any)", exception.message)
    }

    private inline fun <T, R> withResultSetMock(
        testColumnIndex: Int,
        sqlType: Int,
        crossinline sqlResultGetter: ResultSet.(Int) -> T,
        columnValue: T,
        wasNull: Boolean,
        runnable: () -> R?
    ): R? {
        Mockito.doReturn(sqlType).`when`(testResultSetMetaData).getColumnType(testColumnIndex)
        Mockito.doReturn(columnValue).`when`(testResultSet).sqlResultGetter(testColumnIndex)
        Mockito.doReturn(wasNull).`when`(testResultSet).wasNull()

        val resultOrNull = try {
            Pair(null, runnable())
        } catch (e: Throwable) {
            Pair(e, null)
        }

        try {
            Mockito.verify(testResultSet, Mockito.times(1)).sqlResultGetter(Mockito.eq(testColumnIndex))
            Mockito.verify(testResultSet, Mockito.times(1)).wasNull()
            Mockito.verify(testResultSetMetaData, Mockito.times(1)).getColumnType(testColumnIndex)
        } catch (e: Throwable) {
            resultOrNull.first?.let {
                it.addSuppressed(e)
                throw it
            } ?: throw e
        }

        resultOrNull.first?.let { throw it }
                ?: return resultOrNull.second
    }

}
