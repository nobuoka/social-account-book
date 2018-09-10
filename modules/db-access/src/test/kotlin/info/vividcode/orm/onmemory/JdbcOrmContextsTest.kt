package info.vividcode.orm.onmemory

import info.vividcode.orm.db.JdbcOrmContextsTest
import org.junit.jupiter.api.Nested

internal class JdbcOrmContextsTest {

    @Nested
    inner class PropertyTest : JdbcOrmContextsTest.PropertySpec<OnMemoryBareRelation<*>>(withOrmContextFunction, OnMemoryBareRelation::class)

    private val withOrmContextFunction = object : JdbcOrmContextsTest.WithOrmContextFunction {
        override operator fun <T> invoke(runnable: JdbcOrmContextsTest.OrmContext.() -> T): T {
            val ormContext = JdbcOrmContexts.create(JdbcOrmContextsTest.OrmContext::class, JdbcOrmContexts.OnMemoryStorage())
            return with(ormContext, runnable)
        }
    }

}
