package info.vividcode.orm

interface Relation<T : Any> {

    fun select(predicate: RelationPredicate<T>): Relation<T>

    fun toSet(): Set<T>

}
