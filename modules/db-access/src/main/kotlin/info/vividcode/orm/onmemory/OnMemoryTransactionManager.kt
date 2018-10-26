package info.vividcode.orm.onmemory

import info.vividcode.orm.TransactionManager

class OnMemoryTransactionManager<T>(
    private val transactionHandlerFactory: (OnMemoryStorage.Connection) -> T,
    private val storage: OnMemoryStorage
) : TransactionManager<T> {

    override suspend fun <R> withTransaction(execute: suspend (T) -> R): R =
            storage.getConnection().use { connection ->
                connection.transact {
                    execute(transactionHandlerFactory(connection))
                }
            }

}
