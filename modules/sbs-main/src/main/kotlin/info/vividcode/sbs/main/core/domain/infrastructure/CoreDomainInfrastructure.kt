package info.vividcode.sbs.main.core.domain.infrastructure

import info.vividcode.orm.*
import info.vividcode.sbs.main.core.domain.Account
import info.vividcode.sbs.main.core.domain.AccountBook
import info.vividcode.sbs.main.core.domain.User
import java.time.LocalDate

internal interface CoreOrmContext : OrmQueryContext {

    val users: UsersRelation
    val accountBooks: AccountBooksRelation
    val userAccountBooks: UserAccountBooksRelation
    val accounts: AccountsRelation
    val balances: BalancesRelation

    /**
     * Insert [UserTuple] into [UsersRelation].
     */
    @Insert(returnGeneratedKeys = true)
    fun UsersRelation.insert(content: UserTuple.Content): Long

    /**
     * Insert [AccountBookTuple] into [AccountBooksRelation].
     */
    @Insert(returnGeneratedKeys = true)
    fun AccountBooksRelation.insert(content: AccountBookTuple.Content): Long

    /**
     * Insert [UserAccountBookTuple] into [UserAccountBooksRelation].
     */
    @Insert
    fun UserAccountBooksRelation.insert(content: UserAccountBookTuple)

    /**
     * Insert [AccountTuple] into [AccountsRelation].
     */
    @Insert(returnGeneratedKeys = true)
    fun AccountsRelation.insert(content: AccountTuple.Content): Long

    /**
     * Insert [BalanceTuple] into [BalancesRelation].
     */
    @Insert
    fun BalancesRelation.insert(balance: BalanceTuple)

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

@RelationName("account_books")
internal interface AccountBooksRelation : BareRelation<AccountBookTuple>
internal data class AccountBookTuple(
        @AttributeName("id") val id: Long,
        val content: Content
) {
    internal data class Content(@AttributeName("label") val label: String)
}
internal fun AccountBook.Companion.from(tuple: AccountBookTuple) =
        AccountBook(tuple.id, tuple.content.label)

@RelationName("user_account_books")
internal interface UserAccountBooksRelation : BareRelation<UserAccountBookTuple>
internal data class UserAccountBookTuple(
        @AttributeName("user_id") val userId: Long,
        @AttributeName("account_book_id") val accountBookId: Long
)

@RelationName("accounts")
internal interface AccountsRelation : BareRelation<AccountTuple>

internal data class AccountTuple(
    @AttributeName("id") val id: Long,
    val content: Content
) {
    internal data class Content(
            @AttributeName("account_book_id") val accountBookId: Long,
            @AttributeName("label") val label: String
    )
}

internal fun Account.Companion.from(tuple: AccountTuple) =
    Account(tuple.id, tuple.content.label)

@RelationName("balances")
internal interface BalancesRelation : BareRelation<BalanceTuple>

internal data class BalanceTuple(
        @AttributeName("account_id") val accountId: Long,
        @AttributeName("date") val date: LocalDate,
        @AttributeName("value") val value: Int
)
