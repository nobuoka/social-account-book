package info.vividcode.orm.common

import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.TupleClassRegistry
import kotlin.reflect.KType

internal interface RelvarUpdater {

    fun insert(
            relationName: String,
            insertedValue: Any,
            tupleClassRegistry: TupleClassRegistry,
            returnType: KType,
            returnGeneratedKeys: Boolean
    ): Any

    fun update(
            relationName: String,
            updateValue: Any,
            predicate: RelationPredicate<*>,
            tupleClassRegistry: TupleClassRegistry,
            returnType: KType
    ): Any

    fun delete(
            relationName: String,
            predicate: RelationPredicate<*>,
            tupleClassRegistry: TupleClassRegistry,
            returnType: KType
    ): Any

}
