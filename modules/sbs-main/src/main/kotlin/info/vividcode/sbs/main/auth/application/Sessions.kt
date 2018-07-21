package info.vividcode.sbs.main.auth.application

import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.TransactionManager
import info.vividcode.orm.where
import info.vividcode.sbs.main.auth.domain.*
import info.vividcode.sbs.main.auth.domain.infrastructure.AuthOrmContext
import info.vividcode.sbs.main.auth.domain.infrastructure.LoginSessionTuple
import info.vividcode.sbs.main.core.application.CreateUserService
import info.vividcode.sbs.main.core.application.FindUserService
import info.vividcode.sbs.main.core.domain.User

private typealias TxManager = TransactionManager<OrmContextProvider<AuthOrmContext>>

private suspend fun <R> TxManager.withOrmContext(execution: AuthOrmContext.() -> R) =
    withTransaction { it.withOrmContext(execution) }

class RetrieveActorUserService(
    private val transactionManager: TxManager,
    private val findUserService: FindUserService
) {
    suspend fun retrieveActorUserOrNull(sessionId: SessionId): User? = run {
        transactionManager.withOrmContext {
            loginSessions.select(where { LoginSessionTuple::id eq sessionId.value }).toSet().firstOrNull()
                ?.content?.userId
        }?.let {
            findUserService.findUser(it)
        }
    }
}

class CreateNewSessionService(
    private val transactionManager: TxManager,
    private val findUserService: FindUserService,
    private val createUserService: CreateUserService
) {
    suspend fun createNewSessionByTwitterLogin(twitterUserId: Long, twitterScreenName: String): SessionId = run {
        val userIdOrNull = transactionManager.withOrmContext {
            getUserIdConnectedToTwitterAccount(twitterUserId)
        }

        val user = if (userIdOrNull == null) {
            createUserService.createUser(twitterScreenName)
        } else {
            findUserService.findUser(userIdOrNull)
                    ?: throw RuntimeException("Data inconsistency (user[$userIdOrNull] not found)")
        }

        transactionManager.withOrmContext {
            createTwitterUserConnection(user, twitterUserId, twitterScreenName)
            SessionId(loginSessions.insert(LoginSessionTuple.Content(user.id)))
        }
    }
}

class DeleteSessionService(private val transactionManager: TxManager) {
    suspend fun deleteSession(sessionId: SessionId): Boolean =
        transactionManager.withOrmContext {
            loginSessions.delete(where { LoginSessionTuple::id eq sessionId.value }) > 0
        }
}
