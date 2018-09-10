package info.vividcode.orm.db.mapper

import info.vividcode.orm.AttributeName
import java.lang.reflect.InvocationTargetException
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.reflect.KClass
import kotlin.reflect.full.defaultType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

/**
 * 木構造で表される、DB のカラムから Java オブジェクトへの変換処理を担うオブジェクト。
 *
 * 単一のカラムの値を単一の Java オブジェクトに変換する [SingleValueMapper] と、複数のカラムの値を単一の Java
 * オブジェクトに変換する [MultipleValueStructMapper] とで構成される。
 *
 * [RowGeneratorMethodResultSetMapper] で使われる。
 *
 * @param <T> 最終的に生成されるオブジェクトの型。
</T> */
class MapperTree<T : Any> private constructor(private val rootNode: MultipleValueStructMapper<T>) {

    @Throws(SQLException::class)
    fun map(r: ResultSet): T {
        return rootNode.map(r)
    }

    private interface MapperTreeNode {
        @Throws(SQLException::class)
        fun map(r: ResultSet): Any?
    }

    /**
     * [ResultSet] の現在の行から指定のカラムの値を取り出して Java のオブジェクトに変換するクラス。
     */
    private class SingleValueMapper(
        /** 値を取り出す先のカラム名。  */
        private val expectedColumnName: String,
        /** 変換後の Java の型として期待される値。  */
        private val expectedArgType: KClass<*>,
        /** 値の取り出しと Java オブジェクトへの変換に使用されるオブジェクト。  */
        private val columnValueMapper: info.vividcode.orm.db.ColumnValueMapper
    ) : MapperTreeNode {

        @Throws(SQLException::class)
        override fun map(r: ResultSet): Any? {
            val columnIndex: Int = r.findColumn(expectedColumnName)
            try {
                return columnValueMapper.mapColumnValue(r, columnIndex, expectedArgType.starProjectedType)
            } catch (e: RuntimeException) {
                throw RuntimeException(
                    "O/R Mapping failed (" +
                            "expectedColumnName: " + expectedColumnName + ", " +
                            "expectedArgType: " + expectedArgType + ")", e
                )
            }
        }
    }

    private class MultipleValueStructMapper<T : Any>(
        private val generatorMethod: RowGeneratorMethod,
        private val generatedType: KClass<T>,
        private val argMappers: List<MapperTreeNode>
    ) : MapperTreeNode {

        /**
         * 行ごとに配列を生成する必要がないように、インスタンス変数として配列を保持しておく。
         *
         * マルチスレッド対応のため、[ThreadLocal] を使用している。
         * パフォーマンスについては議論があり、[ThreadLocal] 変数を使わずに毎回インスタンスを生成するようにしても良さそうである。
         *
         * @see [パフォーマンスについての議論](https://github.com/recruit-mp/kidsly-server/pull/872.discussion_r150430168)
         */
        private val threadLocalGeneratorMethodArgs = ThreadLocal<Array<Any?>>()

        @Throws(SQLException::class)
        override fun map(r: ResultSet): T {
            val args: Array<Any?> = threadLocalGeneratorMethodArgs.get()
                    ?: arrayOfNulls<Any?>(argMappers.size).also(threadLocalGeneratorMethodArgs::set)

            for (methodArgIndex in 0 until argMappers.size) {
                args[methodArgIndex] = argMappers.get(methodArgIndex).map(r)
            }

            try {
                return generatedType.java.cast(generatorMethod(args))
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e)
            } catch (e: InstantiationException) {
                throw RuntimeException(e)
            }

        }
    }

    companion object {

        fun <T : Any> createTree(
            type: KClass<T>,
            generatorMethodFinder: RowGeneratorMethodFinder,
            columnValueMapper: info.vividcode.orm.db.ColumnValueMapper
        ): MapperTree<T> {
            return MapperTree(createMultipleValueStructMapper(type, generatorMethodFinder, columnValueMapper))
        }

        private fun <T : Any> createMultipleValueStructMapper(
            type: KClass<T>,
            generatorMethodFinder: RowGeneratorMethodFinder,
            columnValueMapper: info.vividcode.orm.db.ColumnValueMapper
        ): MultipleValueStructMapper<T> {
            val m = generatorMethodFinder.findGeneratorMethod(type)
                    ?: throw RuntimeException("Method not found")
            val parameters = m.parameters
            // TODO : ImmutableList
            val argMappers = parameters.map { p ->
                if (p.type.jvmErasure.isData) {
                    //val method = generatorMethodFinder.findGeneratorMethod(p.type.jvmErasure)
                    createMultipleValueStructMapper(p.type.jvmErasure, generatorMethodFinder, columnValueMapper)
                } else {
                    val columnName = p.findAnnotation<AttributeName>()?.name ?: requireNotNull(p.name)
                    SingleValueMapper(columnName, p.type.jvmErasure, columnValueMapper)
                }
            }

            return MultipleValueStructMapper(m, type, argMappers)
        }
    }

}