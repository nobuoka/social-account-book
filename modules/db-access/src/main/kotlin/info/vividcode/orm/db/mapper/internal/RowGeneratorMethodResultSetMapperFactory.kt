package info.vividcode.orm.db.mapper.internal

import info.vividcode.orm.db.ColumnValueMapper
import info.vividcode.orm.db.mapper.MapperTree
import info.vividcode.orm.db.mapper.ResultSetMapper
import info.vividcode.orm.db.mapper.RowGeneratorMethodFinder
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

open class RowGeneratorMethodResultSetMapperFactory(
    private val generatorMethodFinder: RowGeneratorMethodFinder,
    private val columnValueMapper: ColumnValueMapper
) : ResultSetMapper.Factory {

    private val mappers: ConcurrentHashMap<KClass<*>, ResultSetMapper<*>> = ConcurrentHashMap()

    override fun accept(type: KClass<*>): Boolean =
        generatorMethodFinder.findGeneratorMethod(type) !== null

    override fun <T : Any> mapperFor(type: KClass<T>): ResultSetMapper<T> =
        mappers.getOrPut(type) {
            RowGeneratorMethodResultSetMapper(
                MapperTree.createTree(
                    type,
                    generatorMethodFinder,
                    columnValueMapper
                )
            )
        } as ResultSetMapper<T>

}
