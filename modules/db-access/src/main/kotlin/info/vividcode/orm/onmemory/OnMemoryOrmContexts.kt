package info.vividcode.orm.onmemory

import info.vividcode.orm.OrmContextProvider
import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.common.BareRelationExtensionMethodInvocation
import info.vividcode.orm.common.OrmContextInvocationHandler
import info.vividcode.orm.common.OrmQueryContextFunctionInvocation
import info.vividcode.orm.common.RelvarUpdater
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

object OnMemoryOrmContexts {

    fun <T : Any> create(ormContextInterface: KClass<T>, connection: OnMemoryStorage.Connection): T = run {
        val tupleClassRegistry = TupleClassRegistry.Default
        Proxy.newProxyInstance(
            ormContextInterface.java.classLoader,
            arrayOf(ormContextInterface.java),
                OrmContextInvocationHandler(
                        tupleClassRegistry,
                        OnMemoryBareRelationRegistry(tupleClassRegistry),
                        OrmQueryContextFunctionInvocation(connection),
                        BareRelationExtensionMethodInvocation(OnMemoryRelvarUpdater(connection))
                )
        ).let(ormContextInterface::cast)
    }

    fun <T : Any> createProviderFactoryFor(
            ormContextInterface: KClass<T>, coroutineContext: CoroutineDispatcher
    ): (OnMemoryStorage.Connection) -> OrmContextProvider<T> = {
        OnMemoryOrmContextProvider(ormContextInterface, it, coroutineContext)
    }

    private class OnMemoryOrmContextProvider<T : Any>(
            ormContextInterface: KClass<T>,
            connection: OnMemoryStorage.Connection,
            private val coroutineContext: CoroutineDispatcher
    ) : OrmContextProvider<T> {
        private val ormContext = create(ormContextInterface, connection)

        override suspend fun <R> withOrmContext(execute: T.() -> R): R =
                withContext(coroutineContext) { ormContext.execute() }
    }

    internal class OnMemoryRelvarUpdater(private val connection: OnMemoryStorage.Connection) : RelvarUpdater {
        override fun insert(
                relationName: String,
                insertedValue: Any,
                tupleClassRegistry: TupleClassRegistry,
                returnGeneratedKeys: Boolean
        ) = connection.insert(relationName, insertedValue, returnGeneratedKeys, tupleClassRegistry)

        override fun update(
                relationName: String, updateValue: Any, predicate: RelationPredicate<*>, tupleClassRegistry: TupleClassRegistry
        ) = connection.update(relationName, updateValue, predicate, tupleClassRegistry)

        override fun delete(
                relationName: String, predicate: RelationPredicate<*>, tupleClassRegistry: TupleClassRegistry
        ) = connection.delete(relationName, predicate)
    }

}
