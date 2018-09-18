package info.vividcode.orm

import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

/**
 * This represents a Kotlin type information of a class that represents a tuple of relational model.
 *
 * To retrieve an instance of this class, use the [TupleClassRegistry.getTupleClass] method.
 */
class TupleClass<T : Any> internal constructor(
        val constructor: KFunction<T>,
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

    fun createTuple(columnValues: (attributeName: String, returnType: KType) -> Any?): T {
        val args = this.members.map {
            when (it) {
                is TupleClassMember.CounterpartToSingleAttribute ->
                    columnValues(it.attributeName, it.property.returnType)
                is TupleClassMember.CounterpartToMultipleAttributes<T, *> ->
                    it.subAttributeValues.createTuple(columnValues)
            }
        }
        try {
            return this.constructor.call(*args.toTypedArray())
        } catch (e: Exception) {
            throw RuntimeException("Tuple creation failed (constructor : `${this.constructor}`, args : `$args`)", e)
        }
    }

}
