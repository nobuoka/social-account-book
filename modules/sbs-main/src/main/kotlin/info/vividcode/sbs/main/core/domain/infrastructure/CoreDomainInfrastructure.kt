package info.vividcode.sbs.main.core.domain.infrastructure

import info.vividcode.orm.*
import info.vividcode.sbs.main.core.domain.Account
import info.vividcode.sbs.main.core.domain.User

internal interface CoreOrmContext : OrmQueryContext {

    val users: UsersRelation
    val accounts: AccountsRelation
    val userAccounts: UserAccountsRelation

    /**
     * Insert [UserTuple] into [UsersRelation].
     */
    @Insert(returnGeneratedKeys = true)
    fun UsersRelation.insert(content: UserTuple.Content): Long

    /**
     * Insert [AccountTuple] into [AccountsRelation].
     */
    @Insert(returnGeneratedKeys = true)
    fun AccountsRelation.insert(content: AccountTuple.Content): Long

    /**
     * Insert [UserAccountTuple] into [UserAccountsRelation].
     */
    @Insert
    fun UserAccountsRelation.insert(content: UserAccountTuple)

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

@RelationName("accounts")
internal interface AccountsRelation : BareRelation<AccountTuple>

internal data class AccountTuple(
    @AttributeName("id") val id: Long,
    val content: Content
) {
    internal data class Content(@AttributeName("label") val label: String)
}

internal fun Account.Companion.from(tuple: AccountTuple) =
    Account(tuple.id, tuple.content.label)

@RelationName("user_accounts")
internal interface UserAccountsRelation : BareRelation<UserAccountTuple>

internal data class UserAccountTuple(
    @AttributeName("user_id") val userId: Long,
    @AttributeName("account_id") val accountId: Long
)

