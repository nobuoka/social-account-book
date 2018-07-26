package info.vividcode.sbs.main.core.domain.infrastructure

import info.vividcode.orm.*
import info.vividcode.sbs.main.core.domain.User

internal interface CoreOrmContext : OrmQueryContext {

    val users: UsersRelation

    /**
     * Insert [UserTuple] into [UsersRelation].
     */
    @Insert(returnGeneratedKeys = true)
    fun UsersRelation.insert(content: UserTuple.Content): Long

}

@RelationName("users")
internal interface UsersRelation : BareRelation<UserTuple>

internal data class UserTuple(
    @AttributeName("id") val id: Long,
    val content: Content
) {
    internal data class Content(@AttributeName("display_name") val displayName: String)
}

internal fun User.Companion.from(tuple: UserTuple) =
    User(tuple.id, tuple.content.displayName)
