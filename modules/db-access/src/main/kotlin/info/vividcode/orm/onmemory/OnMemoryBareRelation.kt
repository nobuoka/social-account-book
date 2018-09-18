package info.vividcode.orm.onmemory

import info.vividcode.orm.BareRelation
import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.SimpleRestrictedRelation
import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.common.BareRelationFactory
import info.vividcode.orm.common.BareRelationImplementation
import info.vividcode.orm.common.DelegateInvocationHandler
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

internal interface OnMemoryBareRelation<T : Any> : BareRelationImplementation<OnMemoryStorage.Connection, T> {

    override fun selectAll(): OnMemoryOperatedRelation<T> = OnMemoryOperatedRelation { this.getList(relationName, tupleType) }

    companion object : BareRelationFactory<OnMemoryBareRelation<*>> {
        override fun <R : BareRelation<*>> create(
                relationName: String, relationClass: KClass<R>, tupleType: KClass<*>, tupleClassRegistry: TupleClassRegistry
        ): OnMemoryBareRelation<*> =
                Proxy.newProxyInstance(
                        this::class.java.classLoader,
                        arrayOf(relationClass.java, OnMemoryBareRelation::class.java),
                        DelegateInvocationHandler(Delegate(tupleType, relationName, tupleClassRegistry), OnMemoryBareRelation::class)
                ) as OnMemoryBareRelation<*>
    }

    private class Delegate<T : Any>(
            override val tupleType: KClass<T>,
            override val relationName: String,
            override val tupleClassRegistry: TupleClassRegistry
    ) : OnMemoryBareRelation<T> {
        override fun select(predicate: RelationPredicate<T>): SimpleRestrictedRelation<T> =
                OnMemoryOperatedRelation.createSimpleRestricted({ this.getList(relationName, tupleType) }, predicate)
    }

}
