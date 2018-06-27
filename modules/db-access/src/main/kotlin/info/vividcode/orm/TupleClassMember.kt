package info.vividcode.orm

import kotlin.reflect.KProperty1

sealed class TupleClassMember<T> {

    abstract val memberName: String
    abstract val property: KProperty1<T, *>

    class CounterpartToSingleAttribute<T> internal constructor(
        override val memberName: String,
        override val property: KProperty1<T, *>,
        val attributeName: String
    ) : TupleClassMember<T>()

    class CounterpartToMultipleAttributes<T, R : Any> internal constructor(
        override val memberName: String,
        override val property: KProperty1<T, R>,
        val subAttributeValues: TupleClass<R>
    ) : TupleClassMember<T>()

}
