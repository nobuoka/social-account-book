package info.vividcode.sbs.main

import io.ktor.config.MapApplicationConfig
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test

internal class ApplicationTest {

    @Test
    internal fun setup(): Unit = withTestApplication({
        (environment.config as MapApplicationConfig).apply {
            put("sbs.contextUrl", "http://test.example.com")
            put("sbs.databaseJdbcUrl", "jdbc:h2:mem:test;TRACE_LEVEL_FILE=4")
            put("sbs.session.encryptionKey", "0123456789ABCDEF")
            put("sbs.session.signKey", "01234567")
            put("sbs.twitter.clientCredential.identifier", "test-twitter-client-identifier")
            put("sbs.twitter.clientCredential.sharedSecret", "test-twitter-client-shared-secret")
        }
        setup()
    }) {
        // Do nothing.
    }

}
