package info.vividcode.ktor.twitter.login.test

import info.vividcode.ktor.twitter.login.TemporaryCredential
import info.vividcode.ktor.twitter.login.TemporaryCredentialStore
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TestTemporaryCredentialStore : TemporaryCredentialStore {

    private val map: MutableMap<String, TemporaryCredential> =
        ConcurrentHashMap()

    override suspend fun saveTemporaryCredential(temporaryCredential: TemporaryCredential) {
        delay(1, TimeUnit.NANOSECONDS)
        map[temporaryCredential.token] = temporaryCredential
    }

    override suspend fun findTemporaryCredential(token: String): TemporaryCredential? {
        delay(1, TimeUnit.NANOSECONDS)
        return map[token]
    }

}
