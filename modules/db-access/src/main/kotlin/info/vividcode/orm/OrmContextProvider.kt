package info.vividcode.orm

interface OrmContextProvider<T : Any> {

    suspend fun <R> withOrmContext(execute: T.() -> R): R

}
