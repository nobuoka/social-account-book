package info.vividcode.orm.db

import java.sql.ResultSet
import java.sql.SQLException
import kotlin.reflect.KType

interface ColumnValueMapper {

    /**
     * Get DB value in specified column on current row in [ResultSet] and map it to specified Kotlin type.
     *
     * Implementations must be thread safe.
     *
     * @param resultSet Target result set.
     * @param columnIndex Target column index.
     * @param expectedType Expected return type.
     * @return Mapped value.
     * @throws SQLException when a database access error occurs.
     */
    @Throws(SQLException::class)
    fun mapColumnValue(resultSet: ResultSet, columnIndex: Int, expectedType: KType): Any?

}
