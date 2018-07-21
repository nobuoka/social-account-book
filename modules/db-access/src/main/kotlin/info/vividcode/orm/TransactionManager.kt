package info.vividcode.orm

interface TransactionManager<out T> {

    suspend fun <R> withTransaction(execute: suspend (T) -> R): R

}
