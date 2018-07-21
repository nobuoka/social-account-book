package info.vividcode.sbs.main.core.domain.infrastructure

import info.vividcode.orm.*
import info.vividcode.sbs.main.core.domain.User

interface CoreOrmContext : OrmQueryContext {

    val users: UsersRelation

    @Insert(returnGeneratedKeys = true)
    fun UsersRelation.insert(content: UserTuple.Content): Long

}

@RelationName("users")
interface UsersRelation : BareRelation<UserTuple>

data class UserTuple(
    @AttributeName("id") val id: Long,
    val content: Content
) {
    data class Content(@AttributeName("display_name") val displayName: String)
}

fun User.Companion.from(tuple: UserTuple) =
    User(tuple.id, tuple.content.displayName)
