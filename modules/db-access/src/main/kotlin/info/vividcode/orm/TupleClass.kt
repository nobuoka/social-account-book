package info.vividcode.orm

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * This represents a Kotlin type information of a class that represents a tuple of relational model.
 *
 * To retrieve an instance of this class, use the [TupleClassRegistry.getTupleClass] method.
 */
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
            .firstOrNull()
                ?: throw RuntimeException("Attribute name corresponding to `${property.name}` property not found")

}
