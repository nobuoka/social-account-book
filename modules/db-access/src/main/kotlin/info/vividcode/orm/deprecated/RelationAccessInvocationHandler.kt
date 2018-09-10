package info.vividcode.orm.deprecated

import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.TupleClassRegistry
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

class RelationAccessInvocationHandler<T : Any>(
    private val targetClass: KClass<T>,
    private val relationName: String,
    private val connection: Connection
) : InvocationHandler {

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
        if (!targetClass.isInstance(proxy)) {
            throw RuntimeException("$proxy is not instance of $targetClass")
        }

        val sameNameMembers = targetClass.members
            .filter { it.name == method.name }
        val targetMember = sameNameMembers
            .firstOrNull { it.valueParameters.map { it.type.jvmErasure.java } == method.parameters.map { it.type } }

        return when (targetMember?.name) {
            "insert" -> insert(relationName, connection, targetMember, args)
            "select" -> select(
                targetMember.returnType.arguments.first().type!!.jvmErasure as KClass<T>,
                args?.find(RelationPredicate::class::isInstance) as? RelationPredicate<T>?,
                "\"$relationName\"",
                emptyList(),
                TupleClassRegistry.Default
            ).toSet(connection)
            else -> throw RuntimeException()
        }
    }

}
