package info.vividcode.sbs.main.application.test

import info.vividcode.orm.onmemory.OnMemoryStorage
import info.vividcode.sbs.main.core.domain.infrastructure.*

internal fun createTestAppStorage(): OnMemoryStorage = OnMemoryStorage().also { storage ->
    storage.registerRelation("users", UserTuple::class, emptySet(), setOf("id"))
    storage.registerRelation("account_books", AccountBookTuple::class, emptySet(), setOf("id"))
    storage.registerRelation("user_account_books", UserAccountBookTuple::class, emptySet())
    storage.registerRelation("accounts", AccountTuple::class, emptySet(), setOf("id"))
    storage.registerRelation("user_accounts", UserAccountTuple::class, emptySet())
}
