package info.vividcode.orm.onmemory

import info.vividcode.orm.RelationPredicate

private fun <T : Any, R : Any> RelationPredicate.Converter<T, R>.internalCheck(entity: T): Boolean = condition.check(converter(entity))

private fun <T : Any, R : Comparable<R>> internalCheck(predicate: RelationPredicate.Between<T, R>, entity: T): Boolean = run {
    val value = predicate.property.get(entity)!!
    predicate.start <= value && value <= predicate.end
}

fun <T : Any> RelationPredicate<T>.check(entity: T): Boolean = when (this) {
    is RelationPredicate.Eq<T, *> -> this.property.get(entity) == this.value
    is RelationPredicate.In<T, *> -> this.value.contains(this.property.get(entity))
    is RelationPredicate.Between<T, *> -> internalCheck(this as RelationPredicate.Between<T, Comparable<Any>>, entity)
    is RelationPredicate.IsNull<T, *> -> this.property.get(entity) == null
    is RelationPredicate.Converter<T, *> -> this.internalCheck(entity)
    is RelationPredicate.And -> this.expressions.all { it.check(entity) }
}
