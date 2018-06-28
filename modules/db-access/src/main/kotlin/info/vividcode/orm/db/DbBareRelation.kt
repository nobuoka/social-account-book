package info.vividcode.orm.db

import info.vividcode.orm.BareRelation
import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.SimpleRestrictedRelation
import info.vividcode.orm.TupleClassRegistry
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

interface DbBareRelation<T : Any> : BareRelation<T> {

    val tupleType: KClass<T>
    val relationName: String
    val tupleClassRegistry: TupleClassRegistry

    fun selectAll(): OperatedRelation<T> =
        OperatedRelation(
            SqlCommand("SELECT FROM \"$relationName\"", emptyList()),
            SqlResultInfo(tupleType, tupleClassRegistry)
        )

    companion object {
        fun <R : BareRelation<*>> create(
            relationName: String, relationClass: KClass<R>, tupleType: KClass<*>, tupleClassRegistry: TupleClassRegistry
        ): DbBareRelation<*> =
            Proxy.newProxyInstance(
                this::class.java.classLoader,
                arrayOf(relationClass.java, DbBareRelation::class.java),
                DbBareRelation.DelegateInvocationHandler(
                    DbBareRelation.Delegate(tupleType, relationName, tupleClassRegistry)
                )
            ) as DbBareRelation<*>
    }

    private class Delegate<T : Any>(
        override val tupleType: KClass<T>,
        override val relationName: String,
        override val tupleClassRegistry: TupleClassRegistry
    ) : DbBareRelation<T> {
        override fun select(predicate: RelationPredicate<T>): SimpleRestrictedRelation<T> =
            OperatedRelation.createSimpleRestricted(
                predicate, "\"$relationName\"", emptyList(), tupleType, tupleClassRegistry
            )
    }

    private class DelegateInvocationHandler<T : Any>(
        private val delegateObject: DbBareRelation<T>
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
            method.declaringClass.isAssignableFrom(DbBareRelation::class.java) ->
                method.invoke(delegateObject, *(args ?: emptyArray()))
            else -> throw RuntimeException(
                "The method `$method` is unknown. " +
                        "Only methods declared in `Object` class or " +
                        "in `${DbBareRelation::class.simpleName}` class are supported."
            )
        }

        private fun checkDelegateObjectEquality(other: DelegateInvocationHandler<*>): Boolean =
            delegateObject == other.delegateObject
    }

}
