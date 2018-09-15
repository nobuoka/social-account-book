package info.vividcode.orm.common

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

internal class DelegateInvocationHandler<T : Any>(
        private val delegateObject: T,
        private val targetType: KClass<T>
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
        method.declaringClass.isAssignableFrom(targetType.java) ->
            method.invoke(delegateObject, *(args ?: emptyArray()))
        else -> throw RuntimeException(
                "The method `$method` is unknown. " +
                        "Only methods declared in `Object` class or " +
                        "in `${targetType.simpleName}` class are supported."
        )
    }

    private fun checkDelegateObjectEquality(other: DelegateInvocationHandler<*>): Boolean =
            delegateObject == other.delegateObject

}
