package info.vividcode.sbs.main.infrastructure.database

import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.TransactionManager
import info.vividcode.orm.db.JdbcOrmContexts
import info.vividcode.orm.db.JdbcTransactionManager
import info.vividcode.sbs.main.auth.domain.infrastructure.AuthOrmContext
import info.vividcode.sbs.main.core.domain.infrastructure.CoreOrmContext
import kotlinx.coroutines.newFixedThreadPoolContext
import javax.sql.DataSource

internal interface AppOrmContext : CoreOrmContext, AuthOrmContext

internal fun createTransactionManager(appDataSource: DataSource): TransactionManager<OrmContextProvider<AppOrmContext>> =
    run {
        val dbAccessContexts = newFixedThreadPoolContext(4, "DbAccess")
        JdbcTransactionManager(
            JdbcOrmContexts.createProviderFactoryFor(AppOrmContext::class, dbAccessContexts),
            appDataSource
        )
    }
