package info.vividcode.orm.common

import info.vividcode.orm.*
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.jvmErasure

internal class BareRelationExtensionMethodInvocation<R : BareRelationImplementation<*, *>>(private val relvarUpdater: RelvarUpdater) {

    operator fun invoke(
            function: KFunction<*>,
            receiver: R,
            args: Array<out Any>,
            tupleClassRegistry: TupleClassRegistry
    ): Any? {
        val relationalOperationAnnotation =
                function.annotations.firstOrNull { it is Insert || it is Update || it is Delete }
        return when (relationalOperationAnnotation) {
            is Insert -> {
                if (args.size != 1) {
                    throw RuntimeException("`${Insert::class.simpleName}` annotated method must receive single argument.")
                }
                val v = relvarUpdater.insert(
                        receiver.relationName, args[0], tupleClassRegistry,
                        relationalOperationAnnotation.returnGeneratedKeys
                )
                return if (relationalOperationAnnotation.returnGeneratedKeys) {
                    when (function.returnType.jvmErasure) {
                        Long::class -> (v as List<*>).get(0) as Long
                        else ->
                            throw RuntimeException(
                                    "`${Insert::returnGeneratedKeys.name}` method must return `${Long::class.simpleName}`."
                            )
                    }
                } else {
                    when (function.returnType.jvmErasure) {
                        Int::class -> v
                        Unit::class -> Unit
                        else ->
                            throw RuntimeException(
                                    "Non `${Insert::returnGeneratedKeys.name}` method must " +
                                            "return `${Int::class.simpleName}` or `${Unit::class.simpleName}`."
                            )
                    }
                }
            }
            is Update -> {
                if (args.size != 2) {
                    throw RuntimeException("`${Update::class.simpleName}` annotated method must receive two arguments.")
                }
                val updateCount = relvarUpdater.update(
                        receiver.relationName, args[0], args[1] as RelationPredicate<*>, tupleClassRegistry
                )
                when (function.returnType.jvmErasure) {
                    Int::class -> updateCount
                    Unit::class -> Unit
                    else ->
                        throw RuntimeException(
                                "`${Update::class.simpleName}` annotated method must " +
                                        "return `${Int::class.simpleName}` or `${Unit::class.simpleName}`."
                        )
                }
            }
            is Delete -> {
                if (args.size != 1) {
                    throw RuntimeException("`${Delete::class.simpleName}` annotated method must receive single argument.")
                }
                val updateCount = relvarUpdater.delete(
                        receiver.relationName, args[0] as RelationPredicate<*>, tupleClassRegistry
                )
                when (function.returnType.jvmErasure) {
                    Int::class -> updateCount
                    Unit::class -> Unit
                    else ->
                        throw RuntimeException(
                                "`${Delete::class.simpleName}` annotated method must " +
                                        "return `${Int::class.simpleName}` or `${Unit::class.simpleName}`."
                        )
                }
            }
            else -> throw RuntimeException(
                    "`${function.name}` is not annotated with " +
                            "`${Insert::class.simpleName}`, `${Update::class.simpleName}` or `${Delete::class.simpleName}`."
            )
        }
    }

}
