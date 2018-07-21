package info.vividcode.sbs.main

import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.sql.Connection
import javax.sql.DataSource

class H2DatabaseTestExtension : BeforeEachCallback, AfterEachCallback {

    lateinit var dataSource: DataSource
    private lateinit var connection: Connection

    override fun beforeEach(context: ExtensionContext) {
        dataSource = JdbcDataSource().apply {
            setUrl("jdbc:h2:mem:test;TRACE_LEVEL_FILE=4")
        }
        connection = dataSource.connection
    }

    override fun afterEach(context: ExtensionContext) {
        if (this::connection.isInitialized) {
            connection.close()
        }
    }

}
