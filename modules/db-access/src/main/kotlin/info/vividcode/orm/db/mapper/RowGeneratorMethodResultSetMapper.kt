package info.vividcode.orm.db.mapper

import info.vividcode.orm.db.mapper.internal.RowGeneratorMethodResultSetMapperFactory
import info.vividcode.orm.db.SimpleColumnValueMapper
import java.sql.ResultSet
import kotlin.reflect.KClass

interface ResultSetMapper<T> {

    fun map(r: ResultSet): T

    interface Factory {
        fun accept(type: KClass<*>): Boolean

        fun <T : Any> mapperFor(type: KClass<T>): ResultSetMapper<T>

        companion object :
            RowGeneratorMethodResultSetMapperFactory(RowGeneratorMethodFinder.DEFAULT,
                SimpleColumnValueMapper.create()
            )
    }

}
