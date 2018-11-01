package info.vividcode.sbs.main.core.domain

import info.vividcode.orm.where
import info.vividcode.sbs.main.core.domain.infrastructure.*

internal data class User(
    val id: Long,
    val displayName: String
) {
    companion object
}

internal data class AccountBook(
        val id: Long,
        val label: String
) {
    companion object
}

internal data class Account(
    val id: Long,
    val label: String
) {
    companion object
}

internal fun CoreOrmContext.createUser(displayName: String): User {
    val content = UserTuple.Content(displayName)
    val id = users.insert(content)
    return User.from(UserTuple(id, content))
}

internal fun CoreOrmContext.findOrCreateDefaultUserAccountBook(targetUser: User): AccountBook =
        findAccountBooksOfUser(targetUser).find { it.label == "default" }
                ?: createUserAccountBook(targetUser, "default")

internal fun CoreOrmContext.createUserAccountBook(targetUser: User, accountBookLabel: String): AccountBook {
    val accountBookId = accountBooks.insert(AccountBookTuple.Content(accountBookLabel))
    userAccountBooks.insert(UserAccountBookTuple(targetUser.id, accountBookId))
    return accountBooks.select(where { AccountBookTuple::id eq accountBookId }).toSet().first()
            .let(AccountBook.Companion::from)
}

internal fun CoreOrmContext.findAccountBooksOfUser(targetUser: User, accountBookIdsOrNull: Collection<Long>? = null): Set<AccountBook> {
    val userAccountBookTuples = userAccountBooks.select(
            if (accountBookIdsOrNull != null) {
                where {
                    (UserAccountBookTuple::userId eq targetUser.id) and
                            (p(UserAccountBookTuple::accountBookId) `in` accountBookIdsOrNull)
                }
            } else {
                where { (UserAccountBookTuple::userId eq targetUser.id) }
            }
    ).toSet()
    return accountBooks.select(where { p(AccountBookTuple::id) `in` userAccountBookTuples.map { it.accountBookId } })
            .toSet()
            .asSequence()
            .map(AccountBook.Companion::from)
            .toSet()
}

internal fun CoreOrmContext.createAccount(
        targetAccountBook: AccountBook, accountLabel: String
): Account {
    val accountId = accounts.insert(AccountTuple.Content(targetAccountBook.id, accountLabel))
    return accounts.select(where { AccountTuple::id eq accountId }).toSet().first()
        .let(Account.Companion::from)
}
