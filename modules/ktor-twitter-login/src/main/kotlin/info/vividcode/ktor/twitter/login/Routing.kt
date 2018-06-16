package info.vividcode.ktor.twitter.login

import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post

/**
 * @param currentContextUrl http://localhost:8080 みたいな値。
 */
fun Routing.setupTwitterLogin(
    twitterLoginStartPath: String,
    twitterLoginCallbackPath: String,
    currentContextUrl: String,
    clientCredential: ClientCredential,
    env: Env,
    outputPort: OutputPort
) {
    val interceptors =
        TwitterLoginInterceptors(clientCredential, "$currentContextUrl$twitterLoginCallbackPath", env, outputPort)
    post(twitterLoginStartPath, interceptors.startTwitterLogin)
    get(twitterLoginCallbackPath, interceptors.finishTwitterLogin)
}
