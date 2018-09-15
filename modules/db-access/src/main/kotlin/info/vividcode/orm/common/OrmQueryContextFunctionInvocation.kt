package info.vividcode.orm.common

import info.vividcode.orm.OrmQueryContext
import kotlin.reflect.KFunction

/**
 * Method invocation handler for methods declared on [OrmQueryContext].
 */
internal class OrmQueryContextFunctionInvocation<C>(private val connection: C) {

    operator fun invoke(function: KFunction<*>, args: Array<out Any?>?): Set<Any> {
        val operatedRelation = args?.get(0).let { relation ->
            when (relation) {
                is BareRelationImplementation<*, *> -> relation.selectAll()
                is OperatedRelationImplementation<*, *> -> relation
                else -> throw RuntimeException(createUnexpectedParametersMessage(function, args))
            } as OperatedRelationImplementation<C, *>
        }
        return when (function.name) {
            "toSet" -> operatedRelation
            "forUpdate" -> operatedRelation.forUpdate()
            else -> throw RuntimeException(createUnexpectedParametersMessage(function, args))
        }.toSet(connection)
    }

    private fun createUnexpectedParametersMessage(function: KFunction<*>, args: Array<out Any?>?) =
            "This function handles functions declared on `${OrmQueryContext::class.simpleName}`. " +
                    "Passed parameters (`$function` and `${argsToString(args)}`) are not expected."

    private fun argsToString(args: Array<out Any?>?) =
            if (args == null) {
                "null"
            } else {
                "[${args.joinToString(",") { "$it" }}]"
            }

}
