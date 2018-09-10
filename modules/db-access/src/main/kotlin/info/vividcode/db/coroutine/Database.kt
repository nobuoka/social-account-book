package info.vividcode.db.coroutine

interface Transactional {
    suspend fun <T> inReadonlyTransaction(withDatabase: ReadonlyTransaction.() -> T): T
    suspend fun <T> inWritableTransaction(withDatabase: WritableTransaction.() -> T): T
}

interface ReadonlyTransaction {
    fun <T> execute(query: DatabaseQuery<T>): T
}

interface WritableTransaction {
    fun <T> execute(query: DatabaseQuery<T>): T
    fun execute(command: DatabaseCommand): Unit
}

interface DatabaseQuery<T>
interface DatabaseCommand

sealed class CommonTableOperation : DatabaseCommand
data class DropTable(val tableName: String, val ifExist: Boolean) : CommonTableOperation()
data class CreateTable(
        val tableName: String, val columnDefinitions: List<String>, val ifNotExist: Boolean
) : CommonTableOperation()
