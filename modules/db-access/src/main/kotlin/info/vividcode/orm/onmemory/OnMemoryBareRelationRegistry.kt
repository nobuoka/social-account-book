package info.vividcode.orm.onmemory

import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.common.BareRelationRegistry

internal class OnMemoryBareRelationRegistry(tupleClassRegistry: TupleClassRegistry) :
        BareRelationRegistry<OnMemoryBareRelation<*>>(tupleClassRegistry, OnMemoryBareRelation)
