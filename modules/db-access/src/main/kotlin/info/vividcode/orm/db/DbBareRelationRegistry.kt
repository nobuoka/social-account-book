package info.vividcode.orm.db

import info.vividcode.orm.TupleClassRegistry
import info.vividcode.orm.common.BareRelationRegistry

internal class DbBareRelationRegistry(tupleClassRegistry: TupleClassRegistry) :
        BareRelationRegistry<DbBareRelation<*>>(tupleClassRegistry, DbBareRelation)
