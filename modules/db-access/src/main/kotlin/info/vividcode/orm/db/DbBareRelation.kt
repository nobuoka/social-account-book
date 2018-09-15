package info.vividcode.orm.db

import info.vividcode.orm.BareRelation
import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.SimpleRestrictedRelation
import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.common.BareRelationFactory
import info.vividcode.orm.common.BareRelationImplementation
import info.vividcode.orm.common.DelegateInvocationHandler
import info.vividcode.orm.common.OperatedRelationImplementation
import java.lang.reflect.Proxy
import java.sql.Connection
import kotlin.reflect.KClass

internal interface DbBareRelation<T : Any> : BareRelationImplementation<Connection, T> {

    override fun selectAll(): OperatedRelationImplementation<Connection, T> =
        OperatedRelation(
            SqlCommand("SELECT * FROM \"$relationName\"", emptyList()),
            SqlResultInfo(tupleType, tupleClassRegistry)
        )

    companion object : BareRelationFactory<DbBareRelation<*>> {
        override fun <R : BareRelation<*>> create(
            relationName: String, relationClass: KClass<R>, tupleType: KClass<*>, tupleClassRegistry: TupleClassRegistry
        ): DbBareRelation<*> =
            Proxy.newProxyInstance(
                this::class.java.classLoader,
                arrayOf(relationClass.java, DbBareRelation::class.java),
                    DelegateInvocationHandler(Delegate(tupleType, relationName, tupleClassRegistry), DbBareRelation::class)
            ) as DbBareRelation<*>
    }

    private class Delegate<T : Any>(
        override val tupleType: KClass<T>,
        override val relationName: String,
        override val tupleClassRegistry: TupleClassRegistry
    ) : DbBareRelation<T> {
        override fun select(predicate: RelationPredicate<T>): SimpleRestrictedRelation<T> =
            OperatedRelation.createSimpleRestricted(
                predicate, "\"$relationName\"", emptyList(), tupleType, tupleClassRegistry
            )
    }

}
