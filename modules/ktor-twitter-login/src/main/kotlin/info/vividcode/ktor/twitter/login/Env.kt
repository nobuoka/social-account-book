package info.vividcode.ktor.twitter.login

import info.vividcode.ktor.twitter.login.application.ObtainRedirectUrlService
import info.vividcode.ktor.twitter.login.application.ObtainTwitterTokenService
import info.vividcode.oauth.OAuth
import kotlinx.coroutines.experimental.newSingleThreadContext
import okhttp3.OkHttpClient
import kotlin.coroutines.experimental.CoroutineContext

interface Env : ObtainRedirectUrlService.Required, ObtainTwitterTokenService.Required {

    companion object Default : Env {
        override val temporaryCredentialStore: TemporaryCredentialStore =
            TemporaryCredentialStore.OnMemoryTemporaryCredentialStore
        override val oauth: OAuth = OAuth.DEFAULT
        override val httpClient = OkHttpClient()
        override val httpCallContext: CoroutineContext = newSingleThreadContext("HttpCall")
    }

}
