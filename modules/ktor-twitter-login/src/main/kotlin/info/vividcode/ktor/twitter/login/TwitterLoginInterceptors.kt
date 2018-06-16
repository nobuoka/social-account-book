package info.vividcode.ktor.twitter.login

import info.vividcode.ktor.twitter.login.application.ObtainRedirectUrlService
import info.vividcode.ktor.twitter.login.application.ObtainTwitterTokenService
import info.vividcode.ktor.twitter.login.application.TemporaryCredentialNotFoundException
import info.vividcode.ktor.twitter.login.application.TwitterCallFailedException
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.pipeline.PipelineInterceptor
import io.ktor.response.respondRedirect

class TwitterLoginInterceptors(
    private val clientCredential: ClientCredential,
    private val twitterLoginCallbackAbsoluteUrl: String,
    env: Env,
    private val outputPort: OutputPort
) {

    private val redirectService: ObtainRedirectUrlService = ObtainRedirectUrlService(env)
    private val accessTokenService: ObtainTwitterTokenService = ObtainTwitterTokenService(env)

    val startTwitterLogin: PipelineInterceptor<Unit, ApplicationCall> = {
        try {
            val redirectUrl = redirectService.obtainRedirectUrl(clientCredential, twitterLoginCallbackAbsoluteUrl)
            call.respondRedirect(redirectUrl, false)
        } catch (e: TwitterCallFailedException) {
            outputPort.twitterCallFailed(this, e)
        }
    }

    val finishTwitterLogin: PipelineInterceptor<Unit, ApplicationCall> = {
        val oauthToken = call.request.queryParameters["oauth_token"] ?: ""
        val oauthVerifier = call.request.queryParameters["oauth_verifier"] ?: ""

        try {
            val twitterUserInfo = accessTokenService.obtainTwitterToken(clientCredential, oauthToken, oauthVerifier)
            outputPort.success(this, twitterUserInfo)
        } catch (e: TwitterCallFailedException) {
            outputPort.twitterCallFailed(this, e)
        } catch (e: TemporaryCredentialNotFoundException) {
            outputPort.temporaryCredentialNotFound(this, e)
        }
    }

}
