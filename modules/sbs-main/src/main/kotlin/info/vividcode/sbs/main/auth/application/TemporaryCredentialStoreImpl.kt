package info.vividcode.sbs.main.auth.application

import info.vividcode.ktor.twitter.login.TemporaryCredential
import info.vividcode.ktor.twitter.login.TemporaryCredentialStore
import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.TransactionManager
import info.vividcode.orm.where
import info.vividcode.sbs.main.auth.domain.infrastructure.AuthOrmContext
import info.vividcode.sbs.main.auth.domain.infrastructure.TwitterTemporaryCredentialTuple

internal class TemporaryCredentialStoreImpl(
    private val transactionManager: TransactionManager<OrmContextProvider<AuthOrmContext>>
) : TemporaryCredentialStore {

    override suspend fun saveTemporaryCredential(temporaryCredential: TemporaryCredential) {
        val value = TwitterTemporaryCredentialTuple(
            temporaryCredential.token,
            TwitterTemporaryCredentialTuple.Content(temporaryCredential.secret)
        )
        transactionManager.withTransaction { tx ->
            tx.withOrmContext {
                val selected = twitterTemporaryCredentials.select(where {
                    TwitterTemporaryCredentialTuple::identifier eq value.identifier
                }).forUpdate().firstOrNull()
                if (selected == null) {
                    twitterTemporaryCredentials.insert(value)
                } else {
                    twitterTemporaryCredentials.update(value.content, where {
                        TwitterTemporaryCredentialTuple::identifier eq value.identifier
                    })
                }
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
                            it.content.sharedSecret
                        )
                    }
            }
        }

}
