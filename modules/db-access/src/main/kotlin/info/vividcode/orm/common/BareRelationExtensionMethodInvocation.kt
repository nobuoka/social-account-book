package info.vividcode.orm.common

import info.vividcode.orm.*
import kotlin.reflect.KFunction

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
                relvarUpdater.insert(
                        receiver.relationName, args[0], tupleClassRegistry, function.returnType,
                        relationalOperationAnnotation.returnGeneratedKeys
                )
            }
            is Update -> {
                if (args.size != 2) {
                    throw RuntimeException("`${Update::class.simpleName}` annotated method must receive two arguments.")
                }
                relvarUpdater.update(
                        receiver.relationName, args[0], args[1] as RelationPredicate<*>,
                        tupleClassRegistry, function.returnType
                )
            }
            is Delete -> {
                if (args.size != 1) {
                    throw RuntimeException("`${Delete::class.simpleName}` annotated method must receive single argument.")
                }
                relvarUpdater.delete(
                        receiver.relationName, args[0] as RelationPredicate<*>,
                        tupleClassRegistry, function.returnType
                )
            }
            else -> throw RuntimeException(
                    "`${function.name}` is not annotated with " +
                            "`${Insert::class.simpleName}`, `${Update::class.simpleName}` or `${Delete::class.simpleName}`."
            )
        }
    }

}
