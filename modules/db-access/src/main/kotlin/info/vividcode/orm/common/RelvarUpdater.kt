package info.vividcode.orm.common

import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.TupleClassRegistry

internal interface RelvarUpdater {

    /**
     * @return Generated keys if [returnGeneratedKeys] is true. Number of inserted tuples, otherwise.
     */
    fun insert(
            relationName: String,
            insertedValue: Any,
            tupleClassRegistry: TupleClassRegistry,
            returnGeneratedKeys: Boolean
    ): Any

    /**
     * @return Number of updated tuples.
     */
    fun update(
            relationName: String,
            updateValue: Any,
            predicate: RelationPredicate<*>,
            tupleClassRegistry: TupleClassRegistry
    ): Int

    /**
     * @return Number of deleted tuples.
     */
    fun delete(
            relationName: String,
            predicate: RelationPredicate<*>,
            tupleClassRegistry: TupleClassRegistry
    ): Int

}
