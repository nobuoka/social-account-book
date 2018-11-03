package info.vividcode.sbs.main.core.application

import info.vividcode.orm.whereOf
import info.vividcode.sbs.main.core.domain.*
import info.vividcode.sbs.main.core.domain.infrastructure.AccountTuple
import info.vividcode.sbs.main.core.domain.infrastructure.from

internal class CreateUserAccountService(private val txManager: CoreTxManager) {

    internal suspend fun createUserAccount(actor: User, accountBookId: Long, accountLabel: String): Pair<AccountBook, Account> =
        txManager.withOrmContext {
            val accountBook = findAccountBooksOfUser(actor, setOf(accountBookId)).firstOrNull()
                    ?: throw RuntimeException()
            Pair(accountBook, createAccount(accountBook, accountLabel))
        }

}

internal class FindAccountsForAccountBooksFunction(private val txManager: CoreTxManager) {

    internal suspend operator fun invoke(actor: User, accountBookIds: Set<Long>): Map<AccountBook, List<Account>> =
        txManager.withOrmContext {
            val accountBooks = findAccountBooksOfUser(actor, accountBookIds)
            val accountTuples = accounts.select(whereOf(AccountTuple::content) {
                p(AccountTuple.Content::accountBookId) `in` accountBooks.map { it.id }
            }).toSet()
            accountBooks.map { accountBook ->
                accountBook to accountTuples.asSequence().filter { it.content.accountBookId == accountBook.id }.map { Account.from(it) }.toList()
            }.toMap()
        }

}
