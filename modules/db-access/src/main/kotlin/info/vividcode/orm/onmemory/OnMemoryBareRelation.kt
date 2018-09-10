package info.vividcode.orm.onmemory

import info.vividcode.orm.BareRelation
import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.SimpleRestrictedRelation
import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.db.BareRelationFactory
import info.vividcode.orm.db.OperatedRelation
import info.vividcode.orm.db.SqlCommand
import info.vividcode.orm.db.SqlResultInfo
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

interface OnMemoryBareRelation<T : Any> : BareRelation<T> {

    val tupleType: KClass<T>
    val relationName: String
    val tupleClassRegistry: TupleClassRegistry

    fun selectAll(): OperatedRelation<T> =
            OperatedRelation(
                    SqlCommand("SELECT * FROM \"$relationName\"", emptyList()),
                    SqlResultInfo(tupleType, tupleClassRegistry)
            )

    companion object : BareRelationFactory<OnMemoryBareRelation<*>> {
        override fun <R : BareRelation<*>> create(
                relationName: String, relationClass: KClass<R>, tupleType: KClass<*>, tupleClassRegistry: TupleClassRegistry
        ): OnMemoryBareRelation<*> =
                Proxy.newProxyInstance(
                        this::class.java.classLoader,
                        arrayOf(relationClass.java, OnMemoryBareRelation::class.java),
                        OnMemoryBareRelation.DelegateInvocationHandler(
                                OnMemoryBareRelation.Delegate(tupleType, relationName, tupleClassRegistry)
                        )
                ) as OnMemoryBareRelation<*>
    }

    private class Delegate<T : Any>(
            override val tupleType: KClass<T>,
            override val relationName: String,
            override val tupleClassRegistry: TupleClassRegistry
    ) : OnMemoryBareRelation<T> {
        override fun select(predicate: RelationPredicate<T>): SimpleRestrictedRelation<T> =
                OperatedRelation.createSimpleRestricted(
                        predicate, "\"$relationName\"", emptyList(), tupleType, tupleClassRegistry
                )
    }

    private class DelegateInvocationHandler<T : Any>(
            private val delegateObject: OnMemoryBareRelation<T>
    ) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any = when {
            method.declaringClass == Object::class.java -> when (method.name) {
                "equals" -> args?.get(0)?.let { target ->
                    Proxy.isProxyClass(target::class.java) && Proxy.getInvocationHandler(target).let {
                        it is DelegateInvocationHandler<*> && checkDelegateObjectEquality(it)
                    }
                } == true
                "hashCode" -> delegateObject.hashCode()
                "toString" -> delegateObject.toString()
                else -> throw RuntimeException(
                        "The method `$method` is unknown. " +
                                "Only `equals`, `hashCode` and `toString` methods are supported " +
                                "in the `Object`-declared methods scope."
                )
            }
            method.declaringClass.isAssignableFrom(OnMemoryBareRelation::class.java) ->
                method.invoke(delegateObject, *(args ?: emptyArray()))
            else -> throw RuntimeException(
                    "The method `$method` is unknown. " +
                            "Only methods declared in `Object` class or " +
                            "in `${OnMemoryBareRelation::class.simpleName}` class are supported."
            )
        }

        private fun checkDelegateObjectEquality(other: DelegateInvocationHandler<*>): Boolean =
                delegateObject == other.delegateObject
    }

}
