package info.vividcode.orm.db

import info.vividcode.orm.common.OrmContextsSpec
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension
import java.sql.Connection
import java.sql.DriverManager

internal class JdbcOrmContextsTest : OrmContextsSpec<DbBareRelation<*>>(withOrmContextFunction, DbBareRelation::class) {

    companion object {
        @JvmField
        @RegisterExtension
        val connectionTestExtension = TestDbConnectionExtension()

        private val withOrmContextFunction = object : OrmContextsSpec.WithOrmContextFunction {
            override operator fun <T> invoke(runnable: OrmContextsSpec.OrmContext.() -> T): T {
                val ormContext = JdbcOrmContexts.create(OrmContextsSpec.OrmContext::class, connectionTestExtension.connection)
                return with(ormContext, runnable)
            }
        }
    }

    private fun <T> withOrmContext(runnable: OrmContextsSpec.OrmContext.() -> T): T {
        val ormContext = JdbcOrmContexts.create(OrmContextsSpec.OrmContext::class, connectionTestExtension.connection)
        return with(ormContext, runnable)
    }

    class TestDbConnectionExtension : BeforeEachCallback, AfterEachCallback {
        val connection: Connection get() = requireNotNull(connectionInternal)
        private var connectionInternal: Connection? = null

        override fun beforeEach(context: ExtensionContext?) {
            connectionInternal = DriverManager.getConnection("jdbc:h2:mem:test;TRACE_LEVEL_FILE=4")
                .apply {
                    prepareStatement("""CREATE TABLE "test" ("id" BIGINT NOT NULL AUTO_INCREMENT, "value" TEXT)""")
                        .execute()
                    prepareStatement("""INSERT INTO "test" ("id", "value") VALUES (10, 'Hello, world!'), (20, 'Good bye!')""")
                        .execute()
                }
        }

        override fun afterEach(context: ExtensionContext?) {
            connectionInternal?.close()
            connectionInternal = null
        }
    }

}
