package info.vividcode.sbs.main.core.application

import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.TransactionManager
import info.vividcode.sbs.main.core.domain.infrastructure.CoreOrmContext

internal typealias CoreTxManager = TransactionManager<OrmContextProvider<CoreOrmContext>>

internal suspend fun <R> CoreTxManager.withOrmContext(execution: CoreOrmContext.() -> R) =
    withTransaction { it.withOrmContext(execution) }
