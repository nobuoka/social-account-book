package info.vividcode.orm.onmemory

import info.vividcode.orm.Relation
import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.SimpleRestrictedRelation
import info.vividcode.orm.common.OperatedRelationImplementation

open class OnMemoryOperatedRelation<T : Any>(
        private val operate: OnMemoryStorage.Connection.() -> List<T>
) : OperatedRelationImplementation<OnMemoryStorage.Connection, T> {

    class SimpleRestricted<T : Any>(operation: OnMemoryStorage.Connection.() -> List<T>) :
            OnMemoryOperatedRelation<T>(operation), SimpleRestrictedRelation<T>

    override fun select(predicate: RelationPredicate<T>): Relation<T> =
            OnMemoryOperatedRelation { operate().filter(predicate::check) }

    override fun forUpdate(): OnMemoryOperatedRelation<T> =
            OnMemoryOperatedRelation { forUpdate(operate) }

    override fun toSet(connection: OnMemoryStorage.Connection): Set<T> = connection.operate().toSet()

    companion object {
        fun <T : Any> createSimpleRestricted(
                source: OnMemoryStorage.Connection.() -> List<T>,
                predicate: RelationPredicate<T>
        ): SimpleRestrictedRelation<T> = SimpleRestricted { source().filter(predicate::check) }
    }

}
