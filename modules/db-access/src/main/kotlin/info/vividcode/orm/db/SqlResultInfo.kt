package info.vividcode.orm.db

import info.vividcode.orm.TupleClassRegistry
import kotlin.reflect.KClass

class SqlResultInfo<T : Any>(
    val tupleType: KClass<T>,
    val tupleClassRegistry: TupleClassRegistry
)
