package info.vividcode.orm

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

class TupleClassRegistry(private val map: MutableMap<KClass<*>, TupleClass<*>> = mutableMapOf()) {

    fun <T : Any> getTupleClass(target: KClass<T>): TupleClass<T> = run {
        target.primaryConstructor?.let {
            it.valueParameters.map { p ->
                val memberName = p.name!!
                val property = target.memberProperties.find { it.name == memberName }!!
                if (p.type.jvmErasure.isData) {
                    createTupleClassForMultipleAttributes(memberName, property as KProperty1<T, Any>)
                } else {
                    val columnName = p.findAnnotation<AttributeName>()?.name ?: memberName
                    TupleClassMember.CounterpartToSingleAttribute(memberName, property, columnName)
                }
            }.let {
                TupleClass(target, it)
            }
        } ?: throw RuntimeException("$target not have primary constructor")
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
    }

}
