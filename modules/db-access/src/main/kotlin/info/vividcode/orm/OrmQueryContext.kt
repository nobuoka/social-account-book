package info.vividcode.orm

interface OrmQueryContext {

    fun <T : Any> Relation<T>.toSet(): Set<T>

    fun <T : Any> SimpleRestrictedRelation<T>.forUpdate(): Set<T>

}
