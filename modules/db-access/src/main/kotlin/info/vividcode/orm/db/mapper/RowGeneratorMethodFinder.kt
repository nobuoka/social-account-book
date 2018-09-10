package info.vividcode.orm.db.mapper

import info.vividcode.orm.AttributeName
import java.util.Optional
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * 指定の型のオブジェクトを生成するための [RowGeneratorMethod] を取得する処理を保持するクラス。
 *
 * 実装はスレッドセーフになるように気を付けること。
 */
interface RowGeneratorMethodFinder {

    /**
     * 指定の型を生成するための [RowGeneratorMethod] を探して、見つかればそれを返す。 見つからない場合は空を返す。
     *
     * @param type 指定の型。
     * @return 見つかった [RowGeneratorMethod] オブジェクトを含むか、見つからなかった場合は空の [Optional]。
     */
    fun findGeneratorMethod(type: KClass<*>): RowGeneratorMethod?

    companion object {

        /**
         * [FromDb] アノテーションが付けられたメソッドかコンストラクタを返す。
         * 該当するものが複数存在する場合はエラーを送出する。
         */
        val DEFAULT: RowGeneratorMethodFinder = object : RowGeneratorMethodFinder {
            /**
             * [AttributeName] アノテーションが付けられている場合はその値を。 ない場合はパラメータ名を返す。
             * ただし、デフォルトの Java コンパイラはソースコード上のパラメータ名を保持しないようにコンパイルするので注意すること。
             */
            private val columnNameFinder: ColumnNameFinder = object : ColumnNameFinder {
                override fun findExpectedColumnName(parameter: KParameter): String = run {
                    val columnName = parameter.findAnnotation<AttributeName>()
                    if (columnName != null) columnName.name else parameter.name ?: throw RuntimeException()
                }
            }

            override fun findGeneratorMethod(type: KClass<*>): RowGeneratorMethod? {
                //val hasGeneratorMethod = { target -> target.getAnnotation(FromDb::class.java!!) != null }

                return object : RowGeneratorMethod {
                    val constructor = type.primaryConstructor!!

                    override val parameters: List<KParameter> = constructor.parameters

                    override fun invoke(args: Array<Any?>) = constructor.call(*args)

                }

                /*
                val found = Stream.concat<Executable>(
                    Arrays.stream<Constructor<*>>(type.constructors).filter(hasGeneratorMethod),
                    Arrays.stream<Method>(type.declaredMethods).filter(hasGeneratorMethod)
                ).collect<List<Executable>, Any>(Collectors.toList())

                if (found.size >= 2) {
                    throw RuntimeException("Target type has multiple `FromDb`-annotated methods (target type : $type)")
                } else if (found.isEmpty()) {
                    return Optional.empty()
                } else {
                    val executable = found[0]
                    return Optional.of(RowGeneratorMethod.of(executable, type, columnNameFinder))
                }
                */
            }
        }
    }

}