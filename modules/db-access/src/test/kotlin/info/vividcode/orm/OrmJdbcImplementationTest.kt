package info.vividcode.orm

import info.vividcode.orm.db.createJdbcRelation
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.sql.DriverManager

internal class OrmJdbcImplementationTest {

    data class FooTuple(
        @AttributeName("id") val testId: Long,
        val content: Content
    ) {

        data class Content(
            @AttributeName("value") val testValue: String,
            @AttributeName("v2") val testValue2: Int
        )

    }

    interface FooRelation : Relation<FooTuple> {

        fun insert(tuple: FooTuple.Content): Long

        fun update(tuple: FooTuple.Content, condition: RelationPredicate<FooTuple>): Int

        fun delete(condition: RelationPredicate<FooTuple>): Int

    }

    @Test
    fun test() {
        DriverManager.getConnection("jdbc:h2:mem:test;TRACE_LEVEL_FILE=4").use { connection ->
            connection.prepareStatement("""CREATE TABLE "foo" ("id" BIGINT NOT NULL AUTO_INCREMENT, "value" TEXT NOT NULL, "v2" INTEGER)""")
                .execute()
            val fooRelation: FooRelation = createJdbcRelation(FooRelation::class, "foo", connection)

            val id = fooRelation.insert(FooTuple.Content("Test value", 100))
            val expectedTuple = FooTuple(id, FooTuple.Content("Test value", 100))

            run {
                val selected =
                    fooRelation
                        .select(where { FooTuple::testId eq id })
                        .select(whereOf(FooTuple::content) { FooTuple.Content::testValue2 eq 100 })
                        .toSet()
                Assertions.assertEquals(setOf(expectedTuple), selected)
            }

            run {
                val selected =
                    fooRelation
                        .select(whereOf(FooTuple::content) { FooTuple.Content::testValue eq "Test value" })
                        .toSet()
                Assertions.assertEquals(setOf(expectedTuple), selected)
            }
        }
    }

}
