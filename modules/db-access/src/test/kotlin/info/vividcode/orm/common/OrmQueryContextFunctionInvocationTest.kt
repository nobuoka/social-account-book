package info.vividcode.orm.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.lang.RuntimeException
import kotlin.reflect.KFunction

internal class OrmQueryContextFunctionInvocationTest {

    internal interface TestInterface {
        fun testFoo(): String
    }

    internal abstract class UnexpectedUsageSpec(private val function: KFunction<*>, private val args: Array<out Any?>?) {
        @Test
        internal fun test() {
            val ormQueryContextFunctionInvocation = OrmQueryContextFunctionInvocation("")

            val exception = Assertions.assertThrows(RuntimeException::class.java) {
                ormQueryContextFunctionInvocation(function, args)
            }
            Assertions.assertTrue(exception.message?.startsWith("This function handles functions declared on `OrmQueryContext`.") == true) {
                "Unexpected message : ${exception.message}"
            }
        }
    }

    @Nested
    internal inner class UnexpectedUsageTest {
        @Nested
        internal inner class NonNullArgsTest : UnexpectedUsageSpec(TestInterface::testFoo, arrayOf(null, "test"))

        @Nested
        internal inner class NullArgsTest : UnexpectedUsageSpec(TestInterface::testFoo, null)

        @Nested
        internal inner class OperatedRelationImplementationArgTest :
                UnexpectedUsageSpec(TestInterface::testFoo, arrayOf(Mockito.mock(OperatedRelationImplementation::class.java)))
    }

}
