Kdbi (DB Interface for Kotlin)
==========

This library is inspired by [Jdbi](http://jdbi.org/) and
[DBIx::MoCo](https://metacpan.org/pod/release/JKONDO/DBIx-MoCo-0.06/lib/DBIx/MoCo.pm).

## Usage

In case that you want to handle a following relation (table).

```sql
CREATE TABLE "foo" (
    "id" BIGINT,
    "value" TEXT
);
```

First, a tuple (row) class and a relation (table) interface are defined.
The `BareRelation` interface provides `select` operation.

```kotlin
data class FooTuple(
    @AttributeName("id") val id: Long,
    @AttributeName("value") val value: String
)

@RelationName("foo")
interface FooRelation : BareRelation<FooTuple>
```

Next, a relation context interface is defined.
It will provide a relation instance and update operations as extension functions,
such as `@Insert`-annotated method.

```kotlin
interface FooRelationContext {
    val fooRelation: FooRelation

    @Insert
    fun FooRelation.insert(value: FooTuple)
}
```

Now you can use the defined relation context as following.

```kotlin
interface AppOrmContext : OrmQueryContext, FooRelationContext

val connection: Connection
val ormContext = JdbcOrmContexts.create(AppOrmContext::class, connection)

with (ormContext) {
    fooRelation.insert(FooTuple(1, "Hello"))
    val selected = fooRelation.select(where { FooTuple::id eq 1 }).toSet()
}
```
