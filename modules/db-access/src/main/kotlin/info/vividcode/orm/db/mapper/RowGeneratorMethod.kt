package info.vividcode.orm.db.mapper

import kotlin.reflect.KParameter

interface RowGeneratorMethod {

    val parameters: List<KParameter>

    operator fun invoke(args: Array<Any?>): Any?

}
