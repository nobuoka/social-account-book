package info.vividcode.orm.onmemory

import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.TransactionManager
import info.vividcode.orm.common.TransactionManagerSpec
import kotlinx.coroutines.newFixedThreadPoolContext
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.atomic.AtomicReference

internal class OnMemoryTransactionManagerTest : TransactionManagerSpec(::createTransactionManager) {

    companion object {
        @JvmField
        @RegisterExtension
        internal val dataSourceExtension = OnMemoryStorageExtension()

        private fun createTransactionManager(): TransactionManager<OrmContextProvider<AppOrmContext>> {
            val coroutineDispatcher = newFixedThreadPoolContext(4, "JdbcCoroutineContext")
            return OnMemoryTransactionManager(
                    OnMemoryOrmContexts.createProviderFactoryFor(AppOrmContext::class, coroutineDispatcher),
                    dataSourceExtension.storage
            )
        }
    }

    internal class OnMemoryStorageExtension : BeforeEachCallback, AfterEachCallback {
        internal val storage: OnMemoryStorage get() = requireNotNull(internalStorageRef.get())
        private var internalStorageRef: AtomicReference<OnMemoryStorage?> = AtomicReference(null)

        override fun beforeEach(context: ExtensionContext?) {
            internalStorageRef.set(
                    OnMemoryStorage().also { s ->
                        s.registerRelation("foo", FooTuple::class, emptySet())
                    }
            )
        }

        override fun afterEach(context: ExtensionContext?) {
            internalStorageRef.set(null)
        }
    }

}
