package info.vividcode.orm.onmemory

import info.vividcode.orm.RelationPredicate

private fun <T : Any, R : Any> RelationPredicate.Converter<T, R>.internalCheck(entity: T): Boolean = condition.check(converter(entity))

fun <T : Any> RelationPredicate<T>.check(entity: T): Boolean = when (this) {
    is RelationPredicate.Eq<T, *> -> this.property.get(entity) == this.value
    is RelationPredicate.In<T, *> -> this.value.contains(this.property.get(entity))
    is RelationPredicate.IsNull<T, *> -> this.property.get(entity) == null
    is RelationPredicate.Converter<T, *> -> this.internalCheck(entity)
}
