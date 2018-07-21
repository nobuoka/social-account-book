package info.vividcode.sbs.main

import info.vividcode.ktor.twitter.login.TemporaryCredential
import info.vividcode.ktor.twitter.login.TemporaryCredentialStore
import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.TransactionManager
import info.vividcode.orm.where
import info.vividcode.sbs.main.database.TwitterTemporaryCredentialTuple
import info.vividcode.sbs.main.database.TwitterTemporaryCredentialsContext

internal class TemporaryCredentialStoreImpl(
    private val transactionManager: TransactionManager<OrmContextProvider<TwitterTemporaryCredentialsContext>>
) : TemporaryCredentialStore {

    override suspend fun saveTemporaryCredential(temporaryCredential: TemporaryCredential) {
        val value = TwitterTemporaryCredentialTuple(
            temporaryCredential.token,
            temporaryCredential.secret
        )
        transactionManager.withTransaction { tx ->
            tx.withOrmContext {
                twitterTemporaryCredentials.insert(value)
            }
        }
    }

    override suspend fun findTemporaryCredential(token: String): TemporaryCredential? =
        transactionManager.withTransaction { tx ->
            tx.withOrmContext {
                twitterTemporaryCredentials.select(where { TwitterTemporaryCredentialTuple::identifier eq token })
                    .toSet().firstOrNull()?.let {
                        TemporaryCredential(
                            it.identifier,
                            it.sharedSecret
                        )
                    }
            }
        }

}
