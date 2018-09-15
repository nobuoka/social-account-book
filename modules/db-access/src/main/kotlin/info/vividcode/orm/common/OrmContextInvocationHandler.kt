package info.vividcode.orm.common

import info.vividcode.orm.BareRelation
import info.vividcode.orm.OrmQueryContext
import info.vividcode.orm.TupleClassRegistry
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

internal class OrmContextInvocationHandler<T : BareRelationImplementation<C, *>, C>(
        private val tupleClassRegistry: TupleClassRegistry,
        private val relationRegistry: BareRelationRegistry<T>,
        private val ormQueryContextFunctionInvocation: OrmQueryContextFunctionInvocation<C>,
        private val bareRelationExtensionMethodInvocation: BareRelationExtensionMethodInvocation<T>
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val isProperty =
            method.kotlinFunction == null && method.returnType != null && args == null && method.name.startsWith("get")
        if (isProperty) {
            method.returnType.kotlin.asBareRelationOrNull()?.let {
                return relationRegistry.getRelationAsRelationType(it)
            }
            throw RuntimeException(
                "The return type of method `${method.name}` cannot be able to handle (type : ${method.returnType.simpleName})."
            )
        } else {
            val function = method.kotlinFunction
                    ?: throw RuntimeException("`${method.name}` is not Kotlin function.")

            if (method.declaringClass == OrmQueryContext::class.java) {
                return ormQueryContextFunctionInvocation(function, args)
            }

            function.extensionReceiverParameter?.type?.jvmErasure?.asBareRelationOrNull()?.let { receiverClass ->
                val expectedReceiver = relationRegistry.getRelationAsBareRelationType(receiverClass)
                if (args?.get(0) === expectedReceiver) {
                    return bareRelationExtensionMethodInvocation(
                            function, expectedReceiver, args.copyOfRange(1, args.size), tupleClassRegistry
                    )
                } else {
                    throw RuntimeException("Extension receiver of `${method.name}` is unexpected instance.")
                }
            }

            throw RuntimeException("`${method.name}` cannot be handled as function in ORM Context.")
        }
    }

    private fun KClass<*>.asBareRelationOrNull() =
            if (this.isSubclassOf(BareRelation::class)) {
                this.java.asSubclass(BareRelation::class.java).kotlin
            } else {
                null
            }

}
