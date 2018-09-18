package info.vividcode.orm

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

class TupleClassRegistry {

    private val map = ConcurrentHashMap<KClass<*>, TupleClass<*>>()

    fun <T : Any> getTupleClass(target: KClass<T>): TupleClass<T> =
        map.getOrPut(target) {
            createTupleClass(target)
        } as TupleClass<T>

    private fun <T : Any> createTupleClass(target: KClass<T>): TupleClass<T> = run {
        val constructor = target.primaryConstructor
                ?: throw RuntimeException("The `${target.simpleName}` class not have primary constructor.")
        TupleClass(constructor, properties(constructor).map { (memberName, parameter, property) ->
            if (parameter.type.jvmErasure.isData) {
                createTupleClassForMultipleAttributes(memberName, property as KProperty1<T, Any>)
            } else {
                val columnName = parameter.findAnnotation<AttributeName>()?.name ?: memberName
                TupleClassMember.CounterpartToSingleAttribute(memberName, property, columnName)
            }
        })
    }

    private fun <T : Any, R : Any> createTupleClassForMultipleAttributes(
        memberName: String,
        property: KProperty1<T, R>
    ) = TupleClassMember.CounterpartToMultipleAttributes(
        memberName,
        property,
        getTupleClass(property.returnType.jvmErasure as KClass<R>)
    )

    internal fun <T, R : Any> withTupleClass(targetValue: R, task: TupleClass<*>.(R) -> T): T = run {
        task(getTupleClass(targetValue::class as KClass<*>), targetValue)
    }

    companion object {
        val Default = TupleClassRegistry()

        fun <T : Any> createAttributesMap(value: T): Map<String, Any?> {
            fun <T : Any> setAttributeValuePairsToMap(value: T, map: MutableMap<String, Any?>) {
                val targetClass = value::class
                val primaryConstructor = targetClass.primaryConstructor
                        ?: throw RuntimeException("The `${targetClass.simpleName}` class not have primary constructor.")

                properties(primaryConstructor).forEach { (memberName, parameter, property) ->
                    val attributeValue = (property as KProperty1<T, Any?>).get(value)
                    if (parameter.type.jvmErasure.isData) {
                        if (attributeValue != null) setAttributeValuePairsToMap(attributeValue, map)
                    } else {
                        val columnName = parameter.findAnnotation<AttributeName>()?.name ?: memberName
                        map.put(columnName, attributeValue)
                    }
                }
            }
            val map = mutableMapOf<String, Any?>()
            setAttributeValuePairsToMap(value, map)
            return map
        }

        private data class TupleClassProperty<T>(val name: String, val parameter: KParameter, val property: KProperty1<T, *>)

        private fun <T> properties(constructor: KFunction<T>): List<TupleClassProperty<T>> {
            val targetType = constructor.returnType.jvmErasure
            return constructor.valueParameters.map { parameter ->
                val memberName = parameter.name
                        ?: throw RuntimeException("Property `$parameter` has no name.")
                val property = (targetType.memberProperties.find { it.name == memberName } as? KProperty1<T, *>)
                        ?: throw RuntimeException("There is no member property which is named `$memberName`")

                TupleClassProperty(memberName, parameter, property)
            }
        }
    }

}
