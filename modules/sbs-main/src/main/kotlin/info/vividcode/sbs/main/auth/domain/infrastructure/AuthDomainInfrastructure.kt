package info.vividcode.sbs.main.auth.domain.infrastructure

import info.vividcode.orm.*

internal interface AuthOrmContext : OrmQueryContext {

    val twitterTemporaryCredentials: TwitterTemporaryCredentialsRelation

    /**
     * Insert [TwitterTemporaryCredentialTuple] into [TwitterTemporaryCredentialsRelation].
     */
    @Insert
    fun TwitterTemporaryCredentialsRelation.insert(credential: TwitterTemporaryCredentialTuple)

    /**
     * Update [TwitterTemporaryCredentialsRelation].
     */
    @Update
    fun TwitterTemporaryCredentialsRelation.update(
        value: TwitterTemporaryCredentialTuple.Content, predicate: RelationPredicate<TwitterTemporaryCredentialTuple>
    )

}

@RelationName("twitter_temporary_credentials")
internal interface TwitterTemporaryCredentialsRelation : BareRelation<TwitterTemporaryCredentialTuple>

internal data class TwitterTemporaryCredentialTuple(
    @AttributeName("identifier") val identifier: String,
    val content: Content
) {
    internal data class Content(
        @AttributeName("shared_secret") val sharedSecret: String
    )
}
