package info.vividcode.orm.onmemory

import info.vividcode.orm.common.OrmContextsSpec
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

internal class OnMemoryOrmContextsTest : OrmContextsSpec<OnMemoryBareRelation<*>>(withOrmContextFunction, OnMemoryBareRelation::class) {

    companion object {
        @JvmField
        @RegisterExtension
        val testOnMemoryStorageExtension = TestOnMemoryStorageExtension()

        private val withOrmContextFunction = object : OrmContextsSpec.WithOrmContextFunction {
            override operator fun <T> invoke(runnable: OrmContextsSpec.OrmContext.() -> T): T {
                val ormContext = OnMemoryOrmContexts.create(
                        OrmContextsSpec.OrmContext::class,
                        OnMemoryOrmContextsTest.testOnMemoryStorageExtension.onMemoryStorage.createConnection()
                )
                return with(ormContext, runnable)
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

}
