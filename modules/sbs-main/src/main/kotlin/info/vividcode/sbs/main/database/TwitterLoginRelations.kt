package info.vividcode.sbs.main.database

import info.vividcode.orm.*

data class TwitterTemporaryCredentialTuple(
    @AttributeName("identifier") val identifier: String,
    @AttributeName("shared_secret") val sharedSecret: String
)

@RelationName("twitter_temporary_credentials")
interface TwitterTemporaryCredentialsRelation : BareRelation<TwitterTemporaryCredentialTuple>

interface TwitterTemporaryCredentialsContext : OrmQueryContext {

    val twitterTemporaryCredentials: TwitterTemporaryCredentialsRelation

    @Insert
    fun TwitterTemporaryCredentialsRelation.insert(credential: TwitterTemporaryCredentialTuple)

}
