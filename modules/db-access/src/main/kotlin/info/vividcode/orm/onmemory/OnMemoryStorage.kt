package info.vividcode.orm.onmemory

import info.vividcode.orm.RelationPredicate
import info.vividcode.orm.TupleClassRegistry
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.sendBlocking
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

class OnMemoryStorage {

    private class LongIdGenerator {
        private var next: Long = 1L
        fun next() = this.next++
    }

    private class R<T : Any>(val tupleClass: KClass<T>, val value: Set<T>, val idGenerators: Map<String, LongIdGenerator>) {
        constructor(tupleClass: KClass<T>, value: Set<T>, idGenerationAttributeNames: Set<String>) :
                this(tupleClass, value, idGenerationAttributeNames.map { it to LongIdGenerator() }.toMap())

        fun <E : Any> getValueOrThrow(klass: KClass<E>): Set<E> =
                if (klass == this.tupleClass) value as Set<E> else throw RuntimeException()

        fun updateRelation(inserted: Set<T>): Pair<R<T>, Int> {
            val count = inserted.size
            return Pair(R(tupleClass, value + inserted, idGenerators), count)
        }

        fun updateRelation(predicate: RelationPredicate<T>, update: (Set<T>) -> Set<T>): Pair<R<T>, Int> {
            val fold = value.fold(Pair(mutableSetOf<T>(), mutableSetOf<T>())) { d, v ->
                (if (predicate.check(v)) d.first else d.second).add(v); d
            }
            val count = fold.first.size
            val updated = update(fold.first)
            //value.asSequence().filter { !predicate.check(it) }.toSet()
            return Pair(R(tupleClass, updated + fold.second, idGenerators), count)
        }
    }

    private val nameToRelationTypeValuePairMap: MutableMap<String, R<*>> = mutableMapOf()

    fun <T : Any> registerRelation(
            name: String, tupleType: KClass<T>, initialSet: Set<T>,
            idGenerationAttributeNames: Set<String> = emptySet()
    ) {
        nameToRelationTypeValuePairMap[name] = R(tupleType, initialSet, idGenerationAttributeNames)
    }

    private val connectionPool = Channel<AutoCloseableConnection>(1)
    init {
        connectionPool.sendBlocking(
                object : AutoCloseableConnection, Connection by ConnectionImpl(RelationAccessor()) {
                    override fun close() {
                        connectionPool.sendBlocking(this)
                    }
                }
        )
    }

    suspend fun getConnection(): AutoCloseableConnection = connectionPool.receive()

    /**
     * Returns current state of relations.
     */
    val currentState: Map<String, Set<Any>>
        get() = nameToRelationTypeValuePairMap.map { it.key to it.value.value }.toMap()

    private inner class RelationAccessor {
        private var transactional: MutableMap<String, R<*>>? = null

        internal fun get(relationName: String): R<*> = (transactional ?: nameToRelationTypeValuePairMap)[relationName]
                ?: throw RuntimeException("Unknown relation ($relationName)")

        internal fun put(relationName: String, r: R<*>) {
            (transactional ?: nameToRelationTypeValuePairMap)[relationName] = r
        }

        internal fun startTransaction() {
            transactional = nameToRelationTypeValuePairMap.toMutableMap()
        }

        internal fun finishTransaction(commit: Boolean) {
            if (commit) {
                nameToRelationTypeValuePairMap.clear()
                nameToRelationTypeValuePairMap.putAll(requireNotNull(transactional))
            }
            transactional = null
        }
    }

    interface AutoCloseableConnection : Connection, AutoCloseable

    interface Connection {
        fun <T : Any> getList(relationName: String, tupleType: KClass<T>): List<T>

        fun <T> forUpdate(operate: Connection.() -> T): T

        fun <T : Any> insert(relationName: String, insertedValue: T, returnGeneratedKeys: Boolean, tupleClassRegistry: TupleClassRegistry): Any

        fun <T : Any> update(
                relationName: String, updateValue: T, predicate: RelationPredicate<*>, tupleClassRegistry: TupleClassRegistry
        ): Int

        fun delete(relationName: String, predicate: RelationPredicate<*>): Int

        suspend fun <T> transact(execution: suspend () -> T): T
    }

    private class ConnectionImpl internal constructor(private val relationAccessor: RelationAccessor) : Connection {
        override fun <T : Any> getList(relationName: String, tupleType: KClass<T>): List<T> =
                relationAccessor.get(relationName).getValueOrThrow(tupleType).toList()

        // Currently, do nothing.
        override fun <T> forUpdate(operate: Connection.() -> T): T = operate()

        override fun <T : Any> insert(relationName: String, insertedValue: T, returnGeneratedKeys: Boolean, tupleClassRegistry: TupleClassRegistry): Any {
            val insertedValueAttributesMap = TupleClassRegistry.createAttributesMap(insertedValue)
            val r = relationAccessor.get(relationName)
            val tupleClass = tupleClassRegistry.getTupleClass(r.tupleClass)

            val generatedKeys = mutableListOf<Long>()
            val inserted = tupleClass.createTuple { attributeName, returnType ->
                if (!insertedValueAttributesMap.containsKey(attributeName) && r.idGenerators.containsKey(attributeName)) {
                    val id = requireNotNull(r.idGenerators[attributeName]).next()
                    generatedKeys.add(id)
                    id
                } else {
                    insertedValueAttributesMap[attributeName].also {
                        if (it == null) {
                            if (!returnType.isMarkedNullable) {
                                throw RuntimeException("Not nullable (attribute name : $attributeName)")
                            }
                        } else {
                            if (it::class != returnType.jvmErasure) {
                                throw RuntimeException(
                                        "Unexpected class (attribute name : $attributeName, " +
                                                "expected class : ${returnType.jvmErasure}, actual class : ${it::class})"
                                )
                            }
                        }
                    }
                }
            }
            val updateResult = (r as R<Any>).updateRelation(setOf(inserted))

            relationAccessor.put(relationName, updateResult.first)
            return if (returnGeneratedKeys) {
                generatedKeys
            } else {
                updateResult.second
            }
        }

        override fun <T : Any> update(
                relationName: String, updateValue: T, predicate: RelationPredicate<*>, tupleClassRegistry: TupleClassRegistry
        ): Int {
            val updateValueAttributesMap = TupleClassRegistry.createAttributesMap(updateValue)
            val r = relationAccessor.get(relationName)
            val tupleClass = tupleClassRegistry.getTupleClass(r.tupleClass)

            val updateResult = (r as R<Any>).updateRelation(predicate as RelationPredicate<Any>) { updateTargetTuples ->
                updateTargetTuples.asSequence().map {
                    val attributesBeforeUpdate = TupleClassRegistry.createAttributesMap(it)
                    tupleClass.createTuple { attributeName, returnType ->
                        (attributesBeforeUpdate + updateValueAttributesMap).get(attributeName) // TODO : Type checking
                    }
                }.toSet()
            }

            relationAccessor.put(relationName, updateResult.first)
            return updateResult.second
        }

        override fun delete(relationName: String, predicate: RelationPredicate<*>): Int {
            val r = relationAccessor.get(relationName)

            val updateResult = (r as R<Any>).updateRelation(predicate as RelationPredicate<Any>) { emptySet() }

            relationAccessor.put(relationName, updateResult.first)
            return updateResult.second
        }

        override suspend fun <T> transact(execution: suspend () -> T): T {
            relationAccessor.startTransaction()
            var exception: Throwable? = null
            try {
                return execution()
            } catch (e: Throwable) {
                exception = e
                throw exception
            } finally {
                relationAccessor.finishTransaction(exception == null)
            }
        }
    }

}
