package info.vividcode.orm.common

import info.vividcode.orm.BareRelation
import info.vividcode.orm.Relation
import info.vividcode.orm.TupleClassRegistry
import kotlin.reflect.KClass

internal interface BareRelationImplementation<C, T : Any> : BareRelation<T> {
    val tupleType: KClass<T>
    val relationName: String
    val tupleClassRegistry: TupleClassRegistry

    fun selectAll(): OperatedRelationImplementation<C, T>
}

internal interface OperatedRelationImplementation<C, T : Any> : Relation<T> {
    fun forUpdate(): OperatedRelationImplementation<C, T>
    fun toSet(connection: C): Set<Any>
}
