package info.vividcode.sbs.main.core.application

import info.vividcode.orm.whereOf
import info.vividcode.sbs.main.core.domain.Account
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.core.domain.createAccount
import info.vividcode.sbs.main.core.domain.findOrCreateDefaultUserAccountBook
import info.vividcode.sbs.main.core.domain.infrastructure.AccountTuple
import info.vividcode.sbs.main.core.domain.infrastructure.from

internal class CreateUserAccountService(private val txManager: CoreTxManager) {

    internal suspend fun createUserAccount(actor: User, accountLabel: String): Account =
        txManager.withOrmContext {
            val defaultAccountBook = findOrCreateDefaultUserAccountBook(actor)
            createAccount(defaultAccountBook, accountLabel)
        }

}

internal class FindUserAccountsService(private val txManager: CoreTxManager) {

    internal suspend fun findUserAccounts(actor: User): List<Account> =
        txManager.withOrmContext {
            val defaultAccountBook = findOrCreateDefaultUserAccountBook(actor)
            accounts.select(whereOf(AccountTuple::content) { AccountTuple.Content::accountBookId eq defaultAccountBook.id }).toSet()
        }.map(Account.Companion::from).sortedBy(Account::id)

}
