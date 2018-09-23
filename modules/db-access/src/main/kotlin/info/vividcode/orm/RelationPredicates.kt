package info.vividcode.orm

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun <T : Any> where(builder: RelationPredicateBuilder.() -> RelationPredicate<T>): RelationPredicate<T> =
    builder(RelationPredicateBuilder)

fun <T : Any, R : Any> whereOf(
    c: (T) -> R,
    builder: RelationPredicateBuilder.() -> RelationPredicate<R>
): RelationPredicate<T> =
    where { of(c, builder) }

sealed class RelationPredicate<T : Any> {

    class Eq<T : Any, R : Any>(val type: KClass<T>, val property: KProperty1<T, R?>, val value: R) :
        RelationPredicate<T>()

    /**
     * This class represents SQL `in` condition.
     */
    class In<T : Any, R : Any>(val type: KClass<T>, val property: KProperty1<T, R?>, val value: Collection<R>) :
        RelationPredicate<T>()

    class IsNull<T : Any, R>(val type: KClass<T>, val property: KProperty1<T, R>) :
        RelationPredicate<T>()

    class Converter<T : Any, R : Any>(val converter: (T) -> R, val condition: RelationPredicate<R>) :
        RelationPredicate<T>()

}

object RelationPredicateBuilder {

    /**
     * Create [Prop] for [property]. Created value can be used for [in] method.
     */
    inline fun <reified T : Any, R : Any> p(property: KProperty1<T, R?>) = Prop(T::class, property)

    inline infix fun <reified T : Any> KProperty1<T, Long?>.eq(d: Long): RelationPredicate<T> {
        return RelationPredicate.Eq(T::class, this, d)
    }

    inline infix fun <reified T : Any> KProperty1<T, Int?>.eq(d: Int): RelationPredicate<T> {
        return RelationPredicate.Eq(T::class, this, d)
    }

    inline infix fun <reified T : Any> KProperty1<T, String?>.eq(d: String): RelationPredicate<T> {
        return RelationPredicate.Eq(T::class, this, d)
    }

    /**
     * Create a object which represents SQL `in` condition.
     *
     * @param v List of target values. If empty collection is passed, this condition will be always false.
     */
    infix fun <T : Any, E : Any> Prop<T, E?>.`in`(v: Collection<E>): RelationPredicate<T> =
        RelationPredicate.In(this.receiverClass, this.property, v)

    fun <T : Any, R : Any> of(
        c: (T) -> R,
        builder: RelationPredicateBuilder.() -> RelationPredicate<R>
    ): RelationPredicate<T> =
        RelationPredicate.Converter(c, builder(RelationPredicateBuilder))

    inline val <reified T : Any> KProperty1<T, Any?>.isNull: RelationPredicate<T>
        get() {
            return RelationPredicate.IsNull(T::class, this)
        }

    /**
     * This class represents a property.
     *
     * Type parameter of [KProperty1] which represents type of property is out variant.
     * So that type inference for [KProperty1] doesn't work well. Type inference for [Prop] works well.
     *
     * @param T The type of the receiver which should be used to obtain the value of the property.
     * @param R The type of the property. It's not out variant.
     */
    class Prop<T : Any, R>(val receiverClass: KClass<T>, val property: KProperty1<T, R>)

}
