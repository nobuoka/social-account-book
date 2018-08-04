package info.vividcode.sbs.main.core.application

import info.vividcode.orm.where
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.core.domain.createUser
import info.vividcode.sbs.main.core.domain.infrastructure.UserTuple
import info.vividcode.sbs.main.core.domain.infrastructure.from

internal interface CreateUserService {

    /**
     * Create new [User] entity.
     */
    suspend fun createUser(displayName: String): User

    companion object {
        internal fun create(txManager: CoreTxManager): CreateUserService = object : CreateUserService {
            override suspend fun createUser(displayName: String): User =
                txManager.withOrmContext { createUser(displayName) }
        }
    }

}

internal interface FindUserService {

    /**
     * Find [User] entity by its id.
     */
    suspend fun findUser(id: Long): User?

    companion object {
        internal fun create(txManager: CoreTxManager): FindUserService = object : FindUserService {
            override suspend fun findUser(id: Long): User? = run {
                txManager.withOrmContext {
                    users.select(where { UserTuple::id eq id }).toSet().firstOrNull()?.let(User.Companion::from)
                }
            }
        }
    }

}
