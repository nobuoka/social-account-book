package info.vividcode.orm

interface OrmContextProvider<out T : Any> {

    suspend fun <R> withOrmContext(execute: T.() -> R): R

}
