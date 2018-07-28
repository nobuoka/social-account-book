package info.vividcode.sbs.main.core.domain

import info.vividcode.orm.where
import info.vividcode.sbs.main.core.domain.infrastructure.AccountTuple
import info.vividcode.sbs.main.core.domain.infrastructure.CoreOrmContext
import info.vividcode.sbs.main.core.domain.infrastructure.UserAccountTuple
import info.vividcode.sbs.main.core.domain.infrastructure.from

internal data class User(
    val id: Long,
    val displayName: String
) {
    companion object
}

internal data class Account(
    val id: Long,
    val label: String
) {
    companion object
}

internal fun CoreOrmContext.createUserAccount(targetUser: User, accountLabel: String): Account {
    val accountId = accounts.insert(AccountTuple.Content(accountLabel))
    userAccounts.insert(UserAccountTuple(targetUser.id, accountId))
    return accounts.select(where { AccountTuple::id eq accountId }).toSet().first()
        .let(Account.Companion::from)
}
