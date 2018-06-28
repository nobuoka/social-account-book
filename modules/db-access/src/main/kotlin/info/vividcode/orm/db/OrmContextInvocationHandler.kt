package info.vividcode.orm.db

import info.vividcode.orm.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

class OrmContextInvocationHandler(
    private val tupleClassRegistry: TupleClassRegistry,
    private val relationRegistry: DbBareRelationRegistry,
    private val connection: Connection
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val isProperty = method.returnType != null && args == null && method.name.startsWith("get")
        if (isProperty) {
            method.returnType.kotlin.asBareRelationOrNull()?.let {
                return relationRegistry.getRelationAsRelationType(it)
            }
            throw RuntimeException("????")
        } else {
            val function = method.kotlinFunction ?: throw RuntimeException("???")

            if (method.declaringClass == OrmQueryContext::class.java) {
                val operatedRelation = args?.get(0).let { relation ->
                    when (relation) {
                        is DbBareRelation<*> -> relation.selectAll()
                        is OperatedRelation<*> -> relation
                        else -> throw RuntimeException("Unknown")
                    }
                }
                return when (function.name) {
                    "toSet" -> operatedRelation
                    "forUpdate" -> operatedRelation.forUpdate()
                    else -> throw RuntimeException("Unknown")
                }.toSet(connection)
            }

            function.extensionReceiverParameter?.type?.jvmErasure?.asBareRelationOrNull()?.let { bareRelationClass ->
                val expectedReceiver = relationRegistry.getRelationAsBareRelationType(bareRelationClass)
                if (args?.get(0) == expectedReceiver) {
                    val relationalOperationAnnotation =
                        function.annotations.firstOrNull { it is Insert || it is Update || it is Delete }
                    return when (relationalOperationAnnotation) {
                        is Insert -> {
                            insert(
                                connection, expectedReceiver.relationName, args[1],
                                tupleClassRegistry, function.returnType, false
                            )
                        }
                        is Update -> TODO()
                        is Delete -> TODO()
                        else -> TODO()
                    } as? Any?
                }
            }

            throw RuntimeException("Unknown")
        }
    }

    private fun KClass<*>.asBareRelationOrNull() =
        if (this.isSubclassOf(BareRelation::class)) {
            this.java.asSubclass(BareRelation::class.java).kotlin
        } else {
            null
        }

}
