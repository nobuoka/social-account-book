package info.vividcode.orm.onmemory

import info.vividcode.orm.*
import info.vividcode.orm.db.JdbcOrmContextProvider
import kotlinx.coroutines.experimental.CoroutineDispatcher
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.cast
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

object JdbcOrmContexts {

    class OnMemoryStorage {

    }

    fun <T : Any> create(ormContextInterface: KClass<T>, onMemoryStorage: OnMemoryStorage): T = run {
        val tupleClassRegistry = TupleClassRegistry.Default
        Proxy.newProxyInstance(
            ormContextInterface.java.classLoader,
            arrayOf(ormContextInterface.java),
            OrmContextInvocationHandler(
                tupleClassRegistry,
                OnMemoryBareRelationRegistry(tupleClassRegistry),
                onMemoryStorage
            )
        ).let(ormContextInterface::cast)
    }

    fun <T : Any> createProviderFactoryFor(
        ormContextInterface: KClass<T>, jdbcCoroutineContext: CoroutineDispatcher
    ): (OnMemoryStorage) -> OrmContextProvider<T> = {
        TODO() //JdbcOrmContextProvider(ormContextInterface, it, jdbcCoroutineContext)
    }

    private class OrmContextInvocationHandler(
        private val tupleClassRegistry: TupleClassRegistry,
        private val relationRegistry: OnMemoryBareRelationRegistry,
        private val onMemoryStorage: OnMemoryStorage
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
                    return invokeOrmQueryContextFunction(function, args, TODO())
                }

                function.extensionReceiverParameter?.type?.jvmErasure?.asBareRelationOrNull()?.let { receiverClass ->
                    val expectedReceiver = relationRegistry.getRelationAsBareRelationType(receiverClass)
                    if (args?.get(0) === expectedReceiver) {
                        return invokeBareRelationExtensionMethod(
                            function, expectedReceiver, args.copyOfRange(1, args.size),
                            TODO(), tupleClassRegistry
                        )
                    } else {
                        throw RuntimeException("Extension receiver of `${method.name}` is unexpected instance.")
                    }
                }

                throw RuntimeException("`${method.name}` cannot be handled as function in ORM Context.")
            }
        }

    }

    private fun KClass<*>.asBareRelationOrNull() =
        if (this.isSubclassOf(BareRelation::class)) {
            this.java.asSubclass(BareRelation::class.java).kotlin
        } else {
            null
        }

    private fun invokeOrmQueryContextFunction(
        function: KFunction<*>,
        args: Array<out Any>?,
        onMemoryStorage: OnMemoryStorage
    ): Set<Any> {
        val operatedRelation = args?.get(0).let { relation ->
            when (relation) {
                is OnMemoryBareRelation<*> -> relation.selectAll()
                // TODO
                //is OperatedRelation<*> -> relation
                null -> throw RuntimeException("Relation must not be null.")
                else -> throw RuntimeException("Unknown relation type : ${relation::class.simpleName}")
            }
        }
        return when (function.name) {
            "toSet" -> operatedRelation
            "forUpdate" -> operatedRelation.forUpdate()
            else -> throw RuntimeException("`${function.name}` method cannot be handled.")
        }.toSet(TODO())
    }

    private fun invokeBareRelationExtensionMethod(
        function: KFunction<*>,
        receiver: OnMemoryBareRelation<*>,
        args: Array<out Any>,
        connection: Connection,
        tupleClassRegistry: TupleClassRegistry
    ): Any? {
        val relationalOperationAnnotation =
            function.annotations.firstOrNull { it is Insert || it is Update || it is Delete }
        return when (relationalOperationAnnotation) {
            is Insert -> {
                if (args.size != 1) {
                    throw RuntimeException("`${Insert::class.simpleName}` annotated method must receive single argument.")
                }
                TODO() /*(
                    connection, receiver.relationName, args[0],
                    tupleClassRegistry, function.returnType, relationalOperationAnnotation.returnGeneratedKeys
                )*/
            }
            is Update -> {
                if (args.size != 2) {
                    throw RuntimeException("`${Update::class.simpleName}` annotated method must receive two arguments.")
                }
                TODO() /*update(
                    connection, receiver.relationName, args[0], args[1] as RelationPredicate<*>,
                    tupleClassRegistry, function.returnType
                )*/
            }
            is Delete -> {
                if (args.size != 1) {
                    throw RuntimeException("`${Delete::class.simpleName}` annotated method must receive single argument.")
                }
                TODO() /*delete(
                    connection, receiver.relationName, args[0] as RelationPredicate<*>,
                    tupleClassRegistry, function.returnType
                )*/
            }
            else -> throw RuntimeException(
                "`${function.name}` is not annotated with " +
                        "`${Insert::class.simpleName}`, `${Update::class.simpleName}` or `${Delete::class.simpleName}`."
            )
        } as? Any?
    }

}
