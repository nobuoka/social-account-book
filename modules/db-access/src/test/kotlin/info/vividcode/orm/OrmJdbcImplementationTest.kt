package info.vividcode.orm

import info.vividcode.orm.db.JdbcOrmContexts
import org.junit.jupiter.api.Test
import java.sql.DriverManager

internal class OrmJdbcImplementationTest {

    data class MyTuple(@AttributeName("id") val id: Long)

    interface MyRelationContext {

        @RelationName("test")
        interface MyRelation : BareRelation<MyTuple>

        val myRelation: MyRelation

        @Insert
        fun MyRelation.insert(value: MyTuple): Long

        @Update
        fun MyRelation.update(value: MyTuple, predicate: RelationPredicate<MyTuple>): Int

        @Delete
        fun MyRelation.delete(predicate: RelationPredicate<MyTuple>): Int

    }

    interface OrmContext : OrmQueryContext, MyRelationContext

    @Test
    fun ddd() {
        DriverManager.getConnection("jdbc:h2:mem:test;TRACE_LEVEL_FILE=4").use { connection ->
            connection.prepareStatement("""CREATE TABLE "test" ("id" BIGINT NOT NULL AUTO_INCREMENT)""").execute()
            connection.prepareStatement("""INSERT INTO "test" ("id") VALUES (20), (30)""").execute()

            val ormContext = JdbcOrmContexts.create(OrmContext::class, connection)
            with(ormContext) {
                println(myRelation.insert(MyTuple(10)))
                println(myRelation.select(where { MyTuple::id eq 10 }).select(where { MyTuple::id eq 10 }).toSet())
                println(myRelation.select(where { MyTuple::id eq 10 }).forUpdate())
            }
        }
    }

}
