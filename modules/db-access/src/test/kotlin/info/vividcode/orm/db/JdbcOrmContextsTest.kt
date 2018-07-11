package info.vividcode.orm.db

import info.vividcode.orm.*
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension
import java.sql.Connection
import java.sql.DriverManager

internal class JdbcOrmContextsTest {

    data class MyTuple(val id: Id, val content: Content) {
        data class Id(@AttributeName("id") val value: Long)
        data class Content(@AttributeName("value") val value: String)
    }

    interface MyRelationContext {
        @RelationName("test")
        interface MyRelation : BareRelation<MyTuple>

        val myRelation: MyRelation

        val unknownType: String

        @Insert
        fun MyRelation.insertReturningInt(value: MyTuple): Int

        @Insert
        fun MyRelation.insertReturningUnit(value: MyTuple)

        @Insert
        fun MyRelation.insertReturningString(value: MyTuple): String

        @Insert(returnGeneratedKeys = true)
        fun MyRelation.insertReturningGeneratedKeys(value: MyTuple.Content): Long

        @Insert(returnGeneratedKeys = true)
        fun MyRelation.insertReturningGeneratedKeysInt(value: MyTuple.Content): Int

        @Insert
        fun MyRelation.insertTwoParameters(value: MyTuple.Content, value2: String)

        @Update
        fun MyRelation.updateReturningInt(value: MyTuple.Content, predicate: RelationPredicate<MyTuple>): Int

        @Update
        fun MyRelation.updateReturningUnit(value: MyTuple.Content, predicate: RelationPredicate<MyTuple>)

        @Update
        fun MyRelation.updateReturningString(value: MyTuple.Content, predicate: RelationPredicate<MyTuple>): String

        @Update
        fun MyRelation.updateThreeParameters(value: MyTuple.Content, predicate: RelationPredicate<MyTuple>, value3: String)

        @Delete
        fun MyRelation.deleteReturningInt(predicate: RelationPredicate<MyTuple>): Int

        @Delete
        fun MyRelation.deleteReturningUnit(predicate: RelationPredicate<MyTuple>)

        @Delete
        fun MyRelation.deleteReturningString(predicate: RelationPredicate<MyTuple>): String

        @Delete
        fun MyRelation.deleteTwoParameters(predicate: RelationPredicate<MyTuple>, value2: String)

        fun MyRelation.notAnnotated()

        fun unknownMethod()
    }

    interface OrmContext : OrmQueryContext, MyRelationContext

    companion object {
        @JvmField
        @RegisterExtension
        val connectionTestExtension = TestDbConnectionExtension()
    }

    @Nested
    inner class PropertyTest {
        @Test
        fun property() {
            withOrmContext {
                Assertions.assertTrue(myRelation is DbBareRelation<*>)
            }
        }

        @Test
        fun property_unknown() {
            withOrmContext {
                val exception = Assertions.assertThrows(RuntimeException::class.java) {
                    unknownType
                }

                Assertions.assertEquals(
                    "The return type of method `getUnknownType` cannot be able to handle (type : String).",
                    exception.message
                )
            }
        }
    }

    @Nested
    inner class OrmQueryContextMethodsTest {
        @Test
        fun ormQueryContext_toSet_withoutPredicate() {
            val relations = withOrmContext {
                myRelation.toSet()
            }

            Assertions.assertEquals(
                setOf(
                    MyTuple(MyTuple.Id(10), MyTuple.Content("Hello, world!")),
                    MyTuple(MyTuple.Id(20), MyTuple.Content("Good bye!"))
                ),
                relations
            )
        }

        @Test
        fun ormQueryContext_toSet_withPredicate() {
            val relations = withOrmContext {
                myRelation.select(whereOf(MyTuple::id) { MyTuple.Id::value eq 20 }).toSet()
            }

            Assertions.assertEquals(
                setOf(
                    MyTuple(MyTuple.Id(20), MyTuple.Content("Good bye!"))
                ),
                relations
            )
        }

        @Test
        fun ormQueryContext_forUpdate_withPredicate() {
            val relations = withOrmContext {
                myRelation.select(whereOf(MyTuple::id) { MyTuple.Id::value eq 20 }).forUpdate()
            }

            Assertions.assertEquals(setOf(MyTuple(MyTuple.Id(20), MyTuple.Content("Good bye!"))), relations)
        }
    }

    @Test
    fun unknownBareRelationInstance() {
        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            withOrmContext {
                (object : MyRelationContext.MyRelation {
                    override fun select(predicate: RelationPredicate<MyTuple>): SimpleRestrictedRelation<MyTuple> {
                        throw RuntimeException("Exception")
                    }
                }).insertReturningInt(MyTuple(MyTuple.Id(1), MyTuple.Content("Test value")))
            }
        }

        Assertions.assertEquals(
            "Extension receiver of `insertReturningInt` is unexpected instance.",
            exception.message
        )
    }

    @Test
    fun callUnknownMethod() {
        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            withOrmContext {
                unknownMethod()
            }
        }

        Assertions.assertEquals(
            "`unknownMethod` cannot be handled as function in ORM Context.",
            exception.message
        )
    }

    @Test
    fun callNotAnnotatedMethod() {
        val exception = Assertions.assertThrows(RuntimeException::class.java) {
            withOrmContext {
                myRelation.notAnnotated()
            }
        }

        Assertions.assertEquals(
            "`notAnnotated` is not annotated with `Insert`, `Update` or `Delete`.",
            exception.message
        )
    }

    @Nested
    inner class InsertTest {
        @Test
        fun returningInt() {
            val insertedCount = withOrmContext {
                myRelation.insertReturningInt(MyTuple(MyTuple.Id(1), MyTuple.Content("Test value")))
            }

            Assertions.assertEquals(1, insertedCount)
        }

        @Test
        fun returningUnit() {
            withOrmContext {
                myRelation.insertReturningUnit(MyTuple(MyTuple.Id(1), MyTuple.Content("Test value")))
            }
        }

        @Test
        fun returningString() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.insertReturningString(MyTuple(MyTuple.Id(1), MyTuple.Content("Test value")))
                }
            }

            Assertions.assertEquals(
                "Non `returnGeneratedKeys` method must return `Int` or `Unit`.",
                exception.message
            )
        }

        @Test
        fun returnGeneratedKeys() {
            val inserted = withOrmContext {
                val id = myRelation.insertReturningGeneratedKeys(MyTuple.Content("Test value"))
                myRelation.select(whereOf(MyTuple::id) { MyTuple.Id::value eq id }).forUpdate()
            }

            Assertions.assertEquals(1, inserted.size)
            Assertions.assertEquals(MyTuple.Content("Test value"), inserted.first().content)
        }

        @Test
        fun returnGeneratedKeys_int() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.insertReturningGeneratedKeysInt(MyTuple.Content("Test value"))
                }
            }

            Assertions.assertEquals("`returnGeneratedKeys` method must return `Long`.", exception.message)
        }

        @Test
        fun twoParameters() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.insertTwoParameters(MyTuple.Content("Test value"), "")
                }
            }

            Assertions.assertEquals("`Insert` annotated method must receive single argument.", exception.message)
        }
    }

    @Nested
    inner class UpdateTest {
        @Test
        fun returningInt() {
            val updatedCount = withOrmContext {
                myRelation.updateReturningInt(
                    MyTuple.Content("Test value update"),
                    whereOf(MyTuple::id) { MyTuple.Id::value eq 10L }
                )
            }

            Assertions.assertEquals(1, updatedCount)

            val selected = withOrmContext { myRelation.toSet() }
            Assertions.assertEquals(
                setOf(
                    MyTuple(MyTuple.Id(10), MyTuple.Content("Test value update")),
                    MyTuple(MyTuple.Id(20), MyTuple.Content("Good bye!"))
                ),
                selected
            )
        }

        @Test
        fun returningUnit() {
            withOrmContext {
                myRelation.updateReturningUnit(
                    MyTuple.Content("Test value update"),
                    whereOf(MyTuple::id) { MyTuple.Id::value eq 10L }
                )
            }

            val selected = withOrmContext { myRelation.toSet() }
            Assertions.assertEquals(
                setOf(
                    MyTuple(MyTuple.Id(10), MyTuple.Content("Test value update")),
                    MyTuple(MyTuple.Id(20), MyTuple.Content("Good bye!"))
                ),
                selected
            )
        }

        @Test
        fun returningString() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.updateReturningString(
                        MyTuple.Content("Test value update"),
                        whereOf(MyTuple::id) { MyTuple.Id::value eq 10 }
                    )
                }
            }

            Assertions.assertEquals(
                "`Update` annotated method must return `Int` or `Unit`.",
                exception.message
            )
        }

        @Test
        fun threeParameters() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.updateThreeParameters(
                        MyTuple.Content("Test value update"),
                        whereOf(MyTuple::id) { MyTuple.Id::value eq 10 },
                        ""
                    )
                }
            }

            Assertions.assertEquals(
                "`Update` annotated method must receive two arguments.",
                exception.message
            )
        }
    }

    @Nested
    inner class DeleteTest {
        @Test
        fun returningInt() {
            val deletedCount = withOrmContext {
                myRelation.deleteReturningInt(whereOf(MyTuple::id) { MyTuple.Id::value eq 10 })
            }

            Assertions.assertEquals(1, deletedCount)

            val selected = withOrmContext { myRelation.toSet() }
            Assertions.assertEquals(
                setOf(MyTuple(MyTuple.Id(20), MyTuple.Content("Good bye!"))),
                selected
            )
        }

        @Test
        fun returningUnit() {
            withOrmContext {
                myRelation.deleteReturningUnit(whereOf(MyTuple::id) { MyTuple.Id::value eq 10 })
            }

            val selected = withOrmContext { myRelation.toSet() }
            Assertions.assertEquals(
                setOf(MyTuple(MyTuple.Id(20), MyTuple.Content("Good bye!"))),
                selected
            )
        }

        @Test
        fun returningString() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.deleteReturningString(whereOf(MyTuple::id) { MyTuple.Id::value eq 10 })
                }
            }

            Assertions.assertEquals(
                "`Delete` annotated method must return `Int` or `Unit`.",
                exception.message
            )
        }

        @Test
        fun twoParameters() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.deleteTwoParameters(whereOf(MyTuple::id) { MyTuple.Id::value eq 10 }, "")
                }
            }

            Assertions.assertEquals(
                "`Delete` annotated method must receive single argument.",
                exception.message
            )
        }
    }

    private fun <T> withOrmContext(runnable: OrmContext.() -> T): T {
        val ormContext = JdbcOrmContexts.create(OrmContext::class, connectionTestExtension.connection)
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
