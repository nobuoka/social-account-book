package info.vividcode.db.coroutine

interface ConnectionProvider {

    suspend fun <T> withConnection(withConnectionHolder: suspend (Transactional) -> T): T

}
