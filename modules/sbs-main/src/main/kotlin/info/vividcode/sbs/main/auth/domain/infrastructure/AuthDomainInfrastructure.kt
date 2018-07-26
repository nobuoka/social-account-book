package info.vividcode.sbs.main.auth.domain.infrastructure

import info.vividcode.orm.*

internal interface AuthOrmContext : OrmQueryContext {

    val loginSessions: LoginSessionsRelation
    val twitterUsers: TwitterUsersRelation
    val twitterUserConnectionsRelation: TwitterUserConnectionsRelation
    val twitterTemporaryCredentials: TwitterTemporaryCredentialsRelation

    /**
     * Insert [LoginSessionTuple] into [LoginSessionsRelation].
     */
    @Insert(returnGeneratedKeys = true)
    fun LoginSessionsRelation.insert(content: LoginSessionTuple.Content): Long

    /**
     * Delete [LoginSessionTuple] from [LoginSessionsRelation].
     */
    @Delete
    fun LoginSessionsRelation.delete(predicate: RelationPredicate<LoginSessionTuple>): Int

    /**
     * Insert [TwitterUserTuple] into [TwitterUsersRelation].
     */
    @Insert
    fun TwitterUsersRelation.insert(content: TwitterUserTuple)

    /**
     * Insert [TwitterUserConnectionTuple] into [TwitterUserConnectionsRelation].
     */
    @Insert
    fun TwitterUserConnectionsRelation.insert(content: TwitterUserConnectionTuple)

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

@RelationName("login_sessions")
internal interface LoginSessionsRelation : BareRelation<LoginSessionTuple>

internal data class LoginSessionTuple(
    @AttributeName("id") val id: Long,
    val content: Content
) {
    internal data class Content(@AttributeName("user_id") val userId: Long)
}

@RelationName("twitter_user_connections")
internal interface TwitterUserConnectionsRelation : BareRelation<TwitterUserConnectionTuple>

internal data class TwitterUserConnectionTuple(
    @AttributeName("user_id") val userId: Long,
    @AttributeName("twitter_user_id") val twitterUserId: Long
)

@RelationName("twitter_users")
internal interface TwitterUsersRelation : BareRelation<TwitterUserTuple>

internal data class TwitterUserTuple(
    @AttributeName("twitter_user_id") val twitterUserId: Long,
    @AttributeName("twitter_user_name") val twitterUserName: String
)

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
