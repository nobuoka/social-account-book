package info.vividcode.sbs.main.database

import info.vividcode.orm.*

interface AppOrmContext : OrmQueryContext, MyOrmContext, TwitterTemporaryCredentialsContext

interface MyOrmContext {

    val users: UsersRelation
    val loginSessions: LoginSessionsRelation
    val twitterUsers: TwitterUsersRelation
    val twitterUserConnectionsRelation: TwitterUserConnectionsRelation

    @Insert(returnGeneratedKeys = true)
    fun UsersRelation.insert(content: UserTuple.Content): Long

    @Insert(returnGeneratedKeys = true)
    fun LoginSessionsRelation.insert(content: LoginSessionTuple.Content): Long

    @Delete
    fun LoginSessionsRelation.delete(predicate: RelationPredicate<LoginSessionTuple>): Int

    @Insert
    fun TwitterUsersRelation.insert(content: TwitterUserTuple)

    @Insert
    fun TwitterUserConnectionsRelation.insert(content: TwitterUserConnectionTuple)

}

@RelationName("users")
interface UsersRelation : BareRelation<UserTuple>

data class UserTuple(
    @AttributeName("id") val id: Long,
    val content: Content
) {
    data class Content(@AttributeName("display_name") val displayName: String)
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
