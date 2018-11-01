package info.vividcode.sbs.main.core.application

import info.vividcode.sbs.main.core.domain.*

internal class CreateUserAccountBookFunction(private val txManager: CoreTxManager) {
    internal suspend operator fun invoke(actor: User, accountLabel: String): AccountBook =
        txManager.withOrmContext { createUserAccountBook(actor, accountLabel) }
}

internal class FindUserAccountBooksFunction(private val txManager: CoreTxManager) {
    internal suspend operator fun invoke(actor: User): List<AccountBook> =
        txManager.withOrmContext { findAccountBooksOfUser(actor) }.sortedBy(AccountBook::id)
}
