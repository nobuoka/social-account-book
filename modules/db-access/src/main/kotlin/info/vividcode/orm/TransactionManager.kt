package info.vividcode.orm

interface TransactionManager<T> {

    suspend fun <R> withTransaction(execute: suspend (T) -> R): R

}
