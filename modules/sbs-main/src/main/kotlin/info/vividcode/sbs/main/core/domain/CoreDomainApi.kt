package info.vividcode.sbs.main.core.domain

import info.vividcode.orm.where
import info.vividcode.sbs.main.core.domain.infrastructure.*
import java.time.LocalDate

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

internal data class AccountAmount(
    val date: LocalDate,
    val value: Int
) {
    companion object
}

internal fun CoreOrmContext.createUser(displayName: String): User {
    val content = UserTuple.Content(displayName)
    val id = users.insert(content)
    return User.from(UserTuple(id, content))
}

internal fun CoreOrmContext.createUserAccount(targetUser: User, accountLabel: String): Account {
    val accountId = accounts.insert(AccountTuple.Content(accountLabel))
    userAccounts.insert(UserAccountTuple(targetUser.id, accountId))
    return accounts.select(where { AccountTuple::id eq accountId }).toSet().first()
        .let(Account.Companion::from)
}
