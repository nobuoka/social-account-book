package info.vividcode.orm.db

import info.vividcode.orm.BareRelation
import info.vividcode.orm.Relation
import info.vividcode.orm.RelationName
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.cast
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

class DbBareRelationRegistry {

    private val relationMap =
        ConcurrentHashMap<KClass<*>, DbBareRelation<*>>()

    fun <R : BareRelation<*>> getRelationAsRelationType(relationClass: KClass<R>): R =
        relationClass.cast(getRelationAsBareRelationType(relationClass))

    fun <R : BareRelation<*>> getRelationAsBareRelationType(relationClass: KClass<R>): DbBareRelation<*> =
        relationMap.getOrPut(relationClass) {
            val relationName = relationClass.findAnnotation<RelationName>()?.name ?: throw RuntimeException("Unknown")
            val relationSuperType = relationClass.allSupertypes.find { it.jvmErasure == Relation::class } ?: throw RuntimeException("Unknown : $relationClass")
            val tupleType = relationSuperType.arguments.first().type?.jvmErasure ?: throw RuntimeException("Unknown")
            DbBareRelation.create(relationName, relationClass, tupleType)
        }

}
