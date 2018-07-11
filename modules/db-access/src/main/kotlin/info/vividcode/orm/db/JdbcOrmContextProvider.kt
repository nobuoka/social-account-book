package info.vividcode.orm.db

import info.vividcode.orm.OrmContextProvider
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.withContext
import java.sql.Connection
import kotlin.reflect.KClass

internal class JdbcOrmContextProvider<T : Any>(
    ormContextInterface: KClass<T>,
    connection: Connection,
    private val jdbcCoroutineContext: CoroutineDispatcher
) : OrmContextProvider<T> {

    private val ormContext = JdbcOrmContexts.create(ormContextInterface, connection)

    override suspend fun <R> withOrmContext(execute: T.() -> R): R =
        withContext(jdbcCoroutineContext) { ormContext.execute() }

}
