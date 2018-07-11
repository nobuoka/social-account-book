package info.vividcode.orm.db

import info.vividcode.orm.TransactionManager
import java.sql.Connection
import javax.sql.DataSource

class JdbcTransactionManager<T>(
    private val transactionHandlerFactory: (Connection) -> T,
    private val dataSource: DataSource
) : TransactionManager<T> {

    override suspend fun <R> withTransaction(execute: suspend (T) -> R): R =
        dataSource.connection.use {
            it.autoCommit = false
            val (exception, result) = try {
                Pair(null, execute(transactionHandlerFactory(it)))
            } catch (e: Throwable) {
                Pair(e, null)
            }
            if (exception != null) {
                try {
                    it.rollback()
                } catch (e: Throwable) {
                    exception.addSuppressed(e)
                }
                throw exception
            } else {
                it.commit()
                @Suppress("UNCHECKED_CAST")
                return result as R
            }
        }

}
