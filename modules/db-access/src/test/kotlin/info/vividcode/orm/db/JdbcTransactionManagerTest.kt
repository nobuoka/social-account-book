package info.vividcode.orm.db

import info.vividcode.orm.*
import info.vividcode.orm.common.TransactionManagerSpec
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension
import java.sql.Connection
import javax.sql.DataSource

internal class JdbcTransactionManagerTest : TransactionManagerSpec(::createTransactionManager) {

    companion object {
        @JvmField
        @RegisterExtension
        internal val dataSourceExtension = DataSourceExtension()

        private fun createTransactionManager(): TransactionManager<OrmContextProvider<AppOrmContext>> {
            val coroutineDispatcher = newFixedThreadPoolContext(4, "JdbcCoroutineContext")
            return JdbcTransactionManager(
                    JdbcOrmContexts.createProviderFactoryFor(TransactionManagerSpec.AppOrmContext::class, coroutineDispatcher),
                    dataSourceExtension.dataSource
            )
        }
    }

    internal class DataSourceExtension : BeforeEachCallback, AfterEachCallback {
        internal val dataSource: DataSource = JdbcDataSource().apply {
            setUrl("jdbc:h2:mem:test;TRACE_LEVEL_FILE=4")
        }

        private var connectionInternal: Connection? = null

        override fun beforeEach(context: ExtensionContext?) {
            connectionInternal = dataSource.connection.also { c ->
                c.prepareStatement("""CREATE TABLE "foo" ( "id" BIGINT, "value1" TEXT, "value2" INTEGER )""")
                        .executeUpdate()
            }
        }

        override fun afterEach(context: ExtensionContext?) {
            connectionInternal?.close()
            connectionInternal = null
        }
    }

}
