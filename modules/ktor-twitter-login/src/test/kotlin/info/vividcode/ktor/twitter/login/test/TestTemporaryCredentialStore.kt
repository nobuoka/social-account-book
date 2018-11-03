package info.vividcode.ktor.twitter.login.test

import info.vividcode.ktor.twitter.login.TemporaryCredential
import info.vividcode.ktor.twitter.login.TemporaryCredentialStore
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class TestTemporaryCredentialStore : TemporaryCredentialStore {

    private val map: MutableMap<String, TemporaryCredential> =
        ConcurrentHashMap()

    override suspend fun saveTemporaryCredential(temporaryCredential: TemporaryCredential) {
        delay(Duration.ofNanos(1))
        map[temporaryCredential.token] = temporaryCredential
    }

    override suspend fun findTemporaryCredential(token: String): TemporaryCredential? {
        delay(Duration.ofNanos(1))
        return map[token]
    }

}
