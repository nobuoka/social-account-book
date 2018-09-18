package info.vividcode.orm.onmemory

import info.vividcode.orm.common.BareRelationRegistrySpec

internal class OnMemoryBareRelationRegistryTest :
        BareRelationRegistrySpec<OnMemoryBareRelation<*>>(::OnMemoryBareRelationRegistry)
