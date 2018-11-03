package info.vividcode.sbs.main.application.test

import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.TransactionManager
import info.vividcode.orm.onmemory.OnMemoryOrmContexts
import info.vividcode.orm.onmemory.OnMemoryStorage
import info.vividcode.orm.onmemory.OnMemoryTransactionManager
import info.vividcode.sbs.main.infrastructure.database.AppOrmContext
import kotlinx.coroutines.newFixedThreadPoolContext
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.concurrent.atomic.AtomicReference

internal class TestTransactionManagerExtension(
        private val createStorage: () -> OnMemoryStorage
) : BeforeEachCallback, AfterEachCallback {

    internal val txManager by lazy { createTestTransactionManager() }

    internal val storage: OnMemoryStorage get() = requireNotNull(internalStorageRef.get())
    private var internalStorageRef: AtomicReference<OnMemoryStorage?> = AtomicReference(null)

    override fun beforeEach(context: ExtensionContext?) {
        internalStorageRef.set(createStorage())
    }

    override fun afterEach(context: ExtensionContext?) {
        internalStorageRef.set(null)
    }

    private fun createTestTransactionManager(): TransactionManager<OrmContextProvider<AppOrmContext>> =
            run {
                val dbAccessContexts = newFixedThreadPoolContext(1, "TestDbAccess")
                OnMemoryTransactionManager(
                        OnMemoryOrmContexts.createProviderFactoryFor(AppOrmContext::class, dbAccessContexts),
                        storage
                )
            }

}
