package info.vividcode.orm.onmemory

import info.vividcode.orm.common.OrmContextsSpec
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

internal class OnMemoryOrmContextsTest {

    companion object {
        @JvmField
        @RegisterExtension
        val testOnMemoryStorageExtension = TestOnMemoryStorageExtension()

        /**
         * This function is used [OnMemoryOrmContexts.create] method.
         */
        private val withOrmContextSimplyCreated = object : OrmContextsSpec.WithOrmContextFunction {
            override operator fun <T> invoke(runnable: OrmContextsSpec.OrmContext.() -> T): T = runBlocking {
                OnMemoryOrmContextsTest.testOnMemoryStorageExtension.onMemoryStorage.getConnection().use { connection ->
                    val ormContext = OnMemoryOrmContexts.create(OrmContextsSpec.OrmContext::class, connection)
                    with(ormContext, runnable)
                }
            }
        }

        /**
         * This function is used [OnMemoryOrmContexts.createProviderFactoryFor] method.
         */
        private val withOrmContextFromProvider = object : OrmContextsSpec.WithOrmContextFunction {
            private val provider = OnMemoryOrmContexts.createProviderFactoryFor(
                    OrmContextsSpec.OrmContext::class, newFixedThreadPoolContext(1, "TestContext")
            )

            override operator fun <T> invoke(runnable: OrmContextsSpec.OrmContext.() -> T): T = runBlocking {
                OnMemoryOrmContextsTest.testOnMemoryStorageExtension.onMemoryStorage.getConnection().use { connection ->
                    provider(connection).withOrmContext(runnable)
                }
            }
        }
    }

    class TestOnMemoryStorageExtension : BeforeEachCallback {
        val onMemoryStorage: OnMemoryStorage = OnMemoryStorage()

        override fun beforeEach(context: ExtensionContext?) {
            onMemoryStorage.registerRelation("test", OrmContextsSpec.MyTuple::class, setOf(
                    OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(10), OrmContextsSpec.MyTuple.Content("Hello, world!")),
                    OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(20), OrmContextsSpec.MyTuple.Content("Good bye!"))
            ), idGenerationAttributeNames = setOf("id"))
        }
    }

    @Nested
    internal inner class SimplyCreatedOrmContextTest :
            OrmContextsSpec<OnMemoryBareRelation<*>>(withOrmContextSimplyCreated, OnMemoryBareRelation::class)

    @Nested
    internal inner class OrmContextFromProviderTest :
            OrmContextsSpec<OnMemoryBareRelation<*>>(withOrmContextFromProvider, OnMemoryBareRelation::class)

}
