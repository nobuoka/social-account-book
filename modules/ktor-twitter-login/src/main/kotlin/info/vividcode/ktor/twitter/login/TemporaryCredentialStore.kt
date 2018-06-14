package info.vividcode.ktor.twitter.login

import java.util.concurrent.ConcurrentHashMap

interface TemporaryCredentialStore {

    suspend fun saveTemporaryCredential(temporaryCredential: TemporaryCredential)
    suspend fun findTemporaryCredential(token: String): TemporaryCredential?

    object OnMemoryTemporaryCredentialStore : TemporaryCredentialStore {
        private val map: MutableMap<String, TemporaryCredential> = ConcurrentHashMap()

        override suspend fun saveTemporaryCredential(temporaryCredential: TemporaryCredential) {
            map[temporaryCredential.token] = temporaryCredential
        }

        override suspend fun findTemporaryCredential(token: String) = map[token]
    }

}
