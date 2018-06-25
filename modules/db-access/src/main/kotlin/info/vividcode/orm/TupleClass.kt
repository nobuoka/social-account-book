package info.vividcode.orm

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class TupleClass<T : Any> internal constructor(
    val type: KClass<T>,
    val members: List<TupleClassMember<T>>
) {

    fun findAttributeNameFromProperty(property: KProperty1<T, *>): String =
        members
            .filter { it.memberName == property.name }
            .mapNotNull {
                when (it) {
                    is TupleClassMember.CounterpartToSingleAttribute -> it.attributeName
                    is TupleClassMember.CounterpartToMultipleAttributes<T, *> -> null
                }
            }
            .first()

}
