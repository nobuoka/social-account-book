package info.vividcode.sbs.main.application.test

import info.vividcode.orm.onmemory.OnMemoryStorage
import info.vividcode.sbs.main.core.domain.infrastructure.AccountBookTuple
import info.vividcode.sbs.main.core.domain.infrastructure.AccountTuple
import info.vividcode.sbs.main.core.domain.infrastructure.UserAccountBookTuple
import info.vividcode.sbs.main.core.domain.infrastructure.UserTuple

internal fun createTestAppStorage(): OnMemoryStorage = OnMemoryStorage().also { storage ->
    storage.registerRelation("users", UserTuple::class, emptySet(), setOf("id"))
    storage.registerRelation("account_books", AccountBookTuple::class, emptySet(), setOf("id"))
    storage.registerRelation("user_account_books", UserAccountBookTuple::class, emptySet())
    storage.registerRelation("accounts", AccountTuple::class, emptySet(), setOf("id"))
}
