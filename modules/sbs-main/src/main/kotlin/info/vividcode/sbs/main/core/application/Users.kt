package info.vividcode.sbs.main.core.application

import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.TransactionManager
import info.vividcode.orm.where
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.core.domain.infrastructure.CoreOrmContext
import info.vividcode.sbs.main.core.domain.infrastructure.UserTuple
import info.vividcode.sbs.main.core.domain.infrastructure.from

private typealias TxManager = TransactionManager<OrmContextProvider<CoreOrmContext>>

private suspend fun <R> TxManager.withOrmContext(execution: CoreOrmContext.() -> R) =
    withTransaction { it.withOrmContext(execution) }

interface CreateUserService {
    suspend fun createUser(displayName: String): User

    companion object {
        fun create(txManager: TxManager): CreateUserService = object : CreateUserService {
            override suspend fun createUser(displayName: String): User = run {
                txManager.withOrmContext {
                    val content = UserTuple.Content(displayName)
                    val id = users.insert(content)
                    User.from(UserTuple(id, content))
                }
            }
        }
    }
}

interface FindUserService {
    suspend fun findUser(id: Long): User?

    companion object {
        fun create(txManager: TxManager): FindUserService = object : FindUserService {
            override suspend fun findUser(id: Long): User? = run {
                txManager.withOrmContext {
                    users.select(where { UserTuple::id eq id }).toSet().firstOrNull()?.let(User.Companion::from)
                }
            }
        }
    }
}
