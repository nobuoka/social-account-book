package info.vividcode.orm

/**
 * Relation in relational model, which represents a set of tuples.
 *
 * This interface provides relational operations.
 */
interface Relation<T : Any> {

    /**
     * Apply selection operation, which retrieves tuples that meets specified criteria from this relation.
     *
     * @param predicate The criteria used for tuple selection.
     * @return Relation includes tuples that meets specified criteria.
     */
    fun select(predicate: RelationPredicate<T>): Relation<T>

}

interface SimpleRestrictedRelation<T : Any> : Relation<T>

interface BareRelation<T : Any> : Relation<T> {
    override fun select(predicate: RelationPredicate<T>): SimpleRestrictedRelation<T>
}
