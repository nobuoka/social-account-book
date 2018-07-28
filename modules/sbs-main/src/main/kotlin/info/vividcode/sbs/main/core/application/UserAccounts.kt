package info.vividcode.sbs.main.core.application

import info.vividcode.orm.where
import info.vividcode.sbs.main.core.domain.Account
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.core.domain.createUserAccount
import info.vividcode.sbs.main.core.domain.infrastructure.AccountTuple
import info.vividcode.sbs.main.core.domain.infrastructure.UserAccountTuple
import info.vividcode.sbs.main.core.domain.infrastructure.from

internal class CreateUserAccountService(private val txManager: CoreTxManager) {

    internal suspend fun createUserAccount(actor: User, accountLabel: String): Account =
        txManager.withOrmContext { createUserAccount(actor, accountLabel) }

}

internal class FindUserAccountsService(private val txManager: CoreTxManager) {

    internal suspend fun findUserAccounts(actor: User): List<Account> =
        txManager.withOrmContext {
            val selected = userAccounts.select(where { UserAccountTuple::userId eq actor.id }).toSet()
            accounts.select(where { p(AccountTuple::id) `in` selected.map { it.accountId } }).toSet()
        }.map(Account.Companion::from).sortedBy(Account::id)

}
