package info.vividcode.sbs.main.auth.domain.infrastructure

import info.vividcode.orm.*

interface AuthOrmContext : OrmQueryContext {

    val loginSessions: LoginSessionsRelation
    val twitterUsers: TwitterUsersRelation
    val twitterUserConnectionsRelation: TwitterUserConnectionsRelation
    val twitterTemporaryCredentials: TwitterTemporaryCredentialsRelation

    @Insert(returnGeneratedKeys = true)
    fun LoginSessionsRelation.insert(content: LoginSessionTuple.Content): Long

    @Delete
    fun LoginSessionsRelation.delete(predicate: RelationPredicate<LoginSessionTuple>): Int

    @Insert
    fun TwitterUsersRelation.insert(content: TwitterUserTuple)

    @Insert
    fun TwitterUserConnectionsRelation.insert(content: TwitterUserConnectionTuple)

    @Insert
    fun TwitterTemporaryCredentialsRelation.insert(credential: TwitterTemporaryCredentialTuple)

    @Update
    fun TwitterTemporaryCredentialsRelation.update(
        value: TwitterTemporaryCredentialTuple.Content, predicate: RelationPredicate<TwitterTemporaryCredentialTuple>
    )

}

@RelationName("login_sessions")
interface LoginSessionsRelation : BareRelation<LoginSessionTuple>

data class LoginSessionTuple(
    @AttributeName("id") val id: Long,
    val content: Content
) {
    data class Content(@AttributeName("user_id") val userId: Long)
}

@RelationName("twitter_user_connections")
interface TwitterUserConnectionsRelation : BareRelation<TwitterUserConnectionTuple>

data class TwitterUserConnectionTuple(
    @AttributeName("user_id") val userId: Long,
    @AttributeName("twitter_user_id") val twitterUserId: Long
)

@RelationName("twitter_users")
interface TwitterUsersRelation : BareRelation<TwitterUserTuple>

data class TwitterUserTuple(
    @AttributeName("twitter_user_id") val twitterUserId: Long,
    @AttributeName("twitter_user_name") val twitterUserName: String
)

@RelationName("twitter_temporary_credentials")
interface TwitterTemporaryCredentialsRelation : BareRelation<TwitterTemporaryCredentialTuple>

data class TwitterTemporaryCredentialTuple(
    @AttributeName("identifier") val identifier: String,
    val content: Content
) {
    data class Content(
        @AttributeName("shared_secret") val sharedSecret: String
    )
}
