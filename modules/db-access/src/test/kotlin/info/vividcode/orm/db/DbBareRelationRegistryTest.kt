package info.vividcode.orm.db

import info.vividcode.orm.common.BareRelationRegistrySpec

internal class DbBareRelationRegistryTest : BareRelationRegistrySpec<DbBareRelation<*>>(::DbBareRelationRegistry)
