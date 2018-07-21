package info.vividcode.sbs.main.application

import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.TransactionManager
import info.vividcode.orm.where
import info.vividcode.sbs.main.database.*

typealias AppOrmContextTransactionManager = TransactionManager<OrmContextProvider<AppOrmContext>>
suspend fun <R> AppOrmContextTransactionManager.withOrmContext(execution: AppOrmContext.() -> R) =
    withTransaction { it.withOrmContext(execution) }

class RetrieveActorUserService(private val transactionManager: AppOrmContextTransactionManager) {
    suspend fun retrieveActorUserOrNull(sessionId: Long): UserTuple? =
        transactionManager.withOrmContext {
            loginSessions.select(where { LoginSessionTuple::id eq sessionId }).toSet().firstOrNull()?.let {
                users.select(where { UserTuple::id eq it.content.userId }).toSet().firstOrNull()
            }
        }
}

class CreateNewSessionService(private val transactionManager: AppOrmContextTransactionManager) {
    suspend fun createNewSessionByTwitterLogin(twitterUserId: Long, twitterScreenName: String): Long =
        transactionManager.withOrmContext {
            val twitterUserConnection = twitterUserConnectionsRelation.select(where {
                TwitterUserConnectionTuple::twitterUserId eq twitterUserId
            }).forUpdate().firstOrNull()

            val userId: Long
            if (twitterUserConnection == null) {
                userId = users.insert(UserTuple.Content(twitterScreenName))

                val twitterUser = twitterUsers.select(where {
                    TwitterUserTuple::twitterUserId eq twitterUserId
                }).forUpdate().firstOrNull()
                if (twitterUser != null) {
                    twitterUsers.insert(TwitterUserTuple(twitterUserId, twitterScreenName))
                }
                twitterUserConnectionsRelation.insert(TwitterUserConnectionTuple(userId, twitterUserId))
            } else {
                userId = twitterUserConnection.userId
            }

            loginSessions.insert(LoginSessionTuple.Content(userId))
        }
}

class DeleteSessionService(private val transactionManager: AppOrmContextTransactionManager) {
    suspend fun deleteSession(sessionId: Long): Boolean =
        transactionManager.withOrmContext {
            loginSessions.delete(where { LoginSessionTuple::id eq sessionId }) > 0
        }
}
