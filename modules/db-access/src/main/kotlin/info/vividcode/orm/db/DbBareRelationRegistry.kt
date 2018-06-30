package info.vividcode.orm.db

import info.vividcode.orm.BareRelation
import info.vividcode.orm.Relation
import info.vividcode.orm.RelationName
import info.vividcode.orm.TupleClassRegistry
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.cast
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

class DbBareRelationRegistry(private val tupleClassRegistry: TupleClassRegistry) {

    private val relationMap =
        ConcurrentHashMap<KClass<*>, DbBareRelation<*>>()

    fun <R : BareRelation<*>> getRelationAsRelationType(relationClass: KClass<R>): R =
        relationClass.cast(getRelationAsBareRelationType(relationClass))

    fun <R : BareRelation<*>> getRelationAsBareRelationType(relationClass: KClass<R>): DbBareRelation<*> =
        relationMap.getOrPut(relationClass) {
            val relationName = relationClass.findAnnotation<RelationName>()?.name
                    ?: throw RuntimeException(
                        "`${relationClass.simpleName}` must be annotated " +
                                "with `@${RelationName::class.simpleName}` annotation"
                    )
            val relationSuperType = relationClass.allSupertypes.find { it.jvmErasure == Relation::class }
                    ?: throw RuntimeException(
                        "`${relationClass.simpleName}` class must be " +
                                "subclass of `${Relation::class.simpleName}`"
                    )
            val tupleType = relationSuperType.arguments.first().type?.jvmErasure
                    ?: throw RuntimeException("Tuple type cannot be specified by `$relationSuperType`")
            DbBareRelation.create(relationName, relationClass, tupleType, tupleClassRegistry)
        }

}
