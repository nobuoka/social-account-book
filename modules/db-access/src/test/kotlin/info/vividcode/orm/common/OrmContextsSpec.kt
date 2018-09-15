package info.vividcode.orm.common

import info.vividcode.orm.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal abstract class OrmContextsSpec<T : BareRelation<*>>(
        private val withOrmContext: OrmContextsSpec.WithOrmContextFunction,
        private val expectedBareRelationClass: KClass<T>
) {

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

    interface WithOrmContextFunction {
        operator fun <T> invoke(runnable: OrmContext.() -> T): T
    }

    @Nested
    internal inner class PropertySpec {
        @Test
        fun property() {
            withOrmContext {
                Assertions.assertTrue(expectedBareRelationClass.isInstance(myRelation))
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
    internal inner class OrmQueryContextMethodsSpec {
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
                (object : OrmContextsSpec.MyRelationContext.MyRelation {
                    override fun select(predicate: RelationPredicate<OrmContextsSpec.MyTuple>): SimpleRestrictedRelation<OrmContextsSpec.MyTuple> {
                        throw RuntimeException("Exception")
                    }
                }).insertReturningInt(OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(1), OrmContextsSpec.MyTuple.Content("Test value")))
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
                myRelation.insertReturningInt(OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(1), OrmContextsSpec.MyTuple.Content("Test value")))
            }

            Assertions.assertEquals(1, insertedCount)
        }

        @Test
        fun returningUnit() {
            withOrmContext {
                myRelation.insertReturningUnit(OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(1), OrmContextsSpec.MyTuple.Content("Test value")))
            }
        }

        @Test
        fun returningString() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.insertReturningString(OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(1), OrmContextsSpec.MyTuple.Content("Test value")))
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
                val id = myRelation.insertReturningGeneratedKeys(OrmContextsSpec.MyTuple.Content("Test value"))
                myRelation.select(whereOf(OrmContextsSpec.MyTuple::id) { OrmContextsSpec.MyTuple.Id::value eq id }).forUpdate()
            }

            Assertions.assertEquals(1, inserted.size)
            Assertions.assertEquals(OrmContextsSpec.MyTuple.Content("Test value"), inserted.first().content)
        }

        @Test
        fun returnGeneratedKeys_int() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.insertReturningGeneratedKeysInt(OrmContextsSpec.MyTuple.Content("Test value"))
                }
            }

            Assertions.assertEquals("`returnGeneratedKeys` method must return `Long`.", exception.message)
        }

        @Test
        fun twoParameters() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.insertTwoParameters(OrmContextsSpec.MyTuple.Content("Test value"), "")
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
                        OrmContextsSpec.MyTuple.Content("Test value update"),
                        whereOf(OrmContextsSpec.MyTuple::id) { OrmContextsSpec.MyTuple.Id::value eq 10L }
                )
            }

            Assertions.assertEquals(1, updatedCount)

            val selected = withOrmContext { myRelation.toSet() }
            Assertions.assertEquals(
                    setOf(
                            OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(10), OrmContextsSpec.MyTuple.Content("Test value update")),
                            OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(20), OrmContextsSpec.MyTuple.Content("Good bye!"))
                    ),
                    selected
            )
        }

        @Test
        fun returningUnit() {
            withOrmContext {
                myRelation.updateReturningUnit(
                        OrmContextsSpec.MyTuple.Content("Test value update"),
                        whereOf(OrmContextsSpec.MyTuple::id) { OrmContextsSpec.MyTuple.Id::value eq 10L }
                )
            }

            val selected = withOrmContext { myRelation.toSet() }
            Assertions.assertEquals(
                    setOf(
                            OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(10), OrmContextsSpec.MyTuple.Content("Test value update")),
                            OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(20), OrmContextsSpec.MyTuple.Content("Good bye!"))
                    ),
                    selected
            )
        }

        @Test
        fun returningString() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.updateReturningString(
                            OrmContextsSpec.MyTuple.Content("Test value update"),
                            whereOf(OrmContextsSpec.MyTuple::id) { OrmContextsSpec.MyTuple.Id::value eq 10 }
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
                            OrmContextsSpec.MyTuple.Content("Test value update"),
                            whereOf(OrmContextsSpec.MyTuple::id) { OrmContextsSpec.MyTuple.Id::value eq 10 },
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
                myRelation.deleteReturningInt(whereOf(OrmContextsSpec.MyTuple::id) { OrmContextsSpec.MyTuple.Id::value eq 10 })
            }

            Assertions.assertEquals(1, deletedCount)

            val selected = withOrmContext { myRelation.toSet() }
            Assertions.assertEquals(
                    setOf(OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(20), OrmContextsSpec.MyTuple.Content("Good bye!"))),
                    selected
            )
        }

        @Test
        fun returningUnit() {
            withOrmContext {
                myRelation.deleteReturningUnit(whereOf(OrmContextsSpec.MyTuple::id) { OrmContextsSpec.MyTuple.Id::value eq 10 })
            }

            val selected = withOrmContext { myRelation.toSet() }
            Assertions.assertEquals(
                    setOf(OrmContextsSpec.MyTuple(OrmContextsSpec.MyTuple.Id(20), OrmContextsSpec.MyTuple.Content("Good bye!"))),
                    selected
            )
        }

        @Test
        fun returningString() {
            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                withOrmContext {
                    myRelation.deleteReturningString(whereOf(OrmContextsSpec.MyTuple::id) { OrmContextsSpec.MyTuple.Id::value eq 10 })
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
                    myRelation.deleteTwoParameters(whereOf(OrmContextsSpec.MyTuple::id) { OrmContextsSpec.MyTuple.Id::value eq 10 }, "")
                }
            }

            Assertions.assertEquals(
                    "`Delete` annotated method must receive single argument.",
                    exception.message
            )
        }
    }

}