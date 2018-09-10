package info.vividcode.orm.db.mapper

import info.vividcode.orm.AttributeName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.Types

internal class ResultSetMapperTest {

    data class TestObject(
        @AttributeName("name") val name: String,
        val content: Content
    ) {
        data class Content(
            @AttributeName("test_type") val testType: Int
        )
    }

    @Test
    fun accept() {
        val accepted = ResultSetMapper.Factory.accept(TestObject::class)

        Assertions.assertEquals(true, accepted)
    }

    @Test
    fun mapperFor() {
        val mapper = ResultSetMapper.Factory.mapperFor(TestObject::class)
        val resultSet: ResultSet =
            Mockito.mock(ResultSet::class.java) { throw RuntimeException("Unexpected method call ($it)") }
        val resultSetMetaData: ResultSetMetaData =
            Mockito.mock(ResultSetMetaData::class.java) { throw RuntimeException("Unexpected method call ($it)") }

        listOf(
            1 to Pair("name", Types.VARCHAR),
            2 to Pair("test_type", Types.INTEGER)
        ).forEach { (columnIndex, columnInfo) ->
            Mockito.doReturn(columnIndex).`when`(resultSet).findColumn(Mockito.eq(columnInfo.first))
            Mockito.doReturn(columnInfo.second).`when`(resultSetMetaData).getColumnType(Mockito.eq(columnIndex))
        }
        Mockito.doReturn(resultSetMetaData).`when`(resultSet).metaData
        Mockito.doReturn("Kayo").`when`(resultSet).getString(Mockito.eq(1))
        Mockito.doReturn(23).`when`(resultSet).getInt(Mockito.eq(2))
        Mockito.doReturn(false).`when`(resultSet).wasNull()

        // Act
        val mapped = mapper.map(resultSet)

        // Assert
        Assertions.assertEquals(TestObject("Kayo", TestObject.Content(23)), mapped)

        Mockito.verify(resultSet, Mockito.times(1)).findColumn(Mockito.eq("name"))
        Mockito.verify(resultSet, Mockito.times(1)).findColumn(Mockito.eq("test_type"))
        Mockito.verify(resultSet, Mockito.times(2)).metaData
        Mockito.verify(resultSetMetaData, Mockito.times(1)).getColumnType(Mockito.eq(1))
        Mockito.verify(resultSetMetaData, Mockito.times(1)).getColumnType(Mockito.eq(2))

        Mockito.verify(resultSet, Mockito.times(1)).getString(Mockito.eq(1))
        Mockito.verify(resultSet, Mockito.times(1)).getInt(Mockito.eq(2))
        Mockito.verify(resultSet, Mockito.times(2)).wasNull()
        Mockito.verifyNoMoreInteractions(resultSet, resultSetMetaData)
    }

}
