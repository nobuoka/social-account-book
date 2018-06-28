package info.vividcode.orm

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

fun <T : Any> where(builder: RelationPredicateBuilder.() -> RelationPredicate<T>): RelationPredicate<T> =
    builder(RelationPredicateBuilder)

fun <T : Any, R : Any> whereOf(
    c: (T) -> R,
    builder: RelationPredicateBuilder.() -> RelationPredicate<R>
): RelationPredicate<T> =
    where { of(c, builder) }

sealed class RelationPredicate<T : Any> {

    class Eq<T : Any, R : Any>(val type: KClass<T>, val property: KProperty1<T, R>, val value: R) :
        RelationPredicate<T>()

    class Converter<T : Any, R : Any>(val converter: (T) -> R, val condition: RelationPredicate<R>) :
        RelationPredicate<T>()

}

object RelationPredicateBuilder {

    inline infix fun <reified T : Any> KProperty1<T, Long>.eq(d: Long): RelationPredicate<T> {
        return RelationPredicate.Eq(T::class, this, d)
    }

    inline infix fun <reified T : Any> KProperty1<T, Int>.eq(d: Int): RelationPredicate<T> {
        return RelationPredicate.Eq(T::class, this, d)
    }

    inline infix fun <reified T : Any> KProperty1<T, String>.eq(d: String): RelationPredicate<T> {
        return RelationPredicate.Eq(T::class, this, d)
    }

    fun <T : Any, R : Any> of(
        c: (T) -> R,
        builder: RelationPredicateBuilder.() -> RelationPredicate<R>
    ): RelationPredicate<T> =
        RelationPredicate.Converter(c, builder(RelationPredicateBuilder))

    val <T> KProperty1<T, Any>.isNull: String
        get() {
            val parameterName = this.findAnnotation<ParameterName>()?.name ?: this.name
            return "$parameterName IS NULL"
        }

}
