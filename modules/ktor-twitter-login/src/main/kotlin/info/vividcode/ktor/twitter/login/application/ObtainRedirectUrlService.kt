package info.vividcode.ktor.twitter.login.application

import info.vividcode.ktor.twitter.login.ClientCredential
import info.vividcode.ktor.twitter.login.TemporaryCredential
import info.vividcode.ktor.twitter.login.TemporaryCredentialStore
import info.vividcode.oauth.HttpRequest
import info.vividcode.oauth.OAuth
import info.vividcode.oauth.OAuthCredentials
import info.vividcode.oauth.ProtocolParameter
import info.vividcode.oauth.protocol.ParameterTransmission
import info.vividcode.whatwg.url.parseWwwFormUrlEncoded
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.experimental.async
import okhttp3.Call
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import kotlin.coroutines.experimental.CoroutineContext

class ObtainRedirectUrlService(private val env: Required) {
    interface Required {
        val oauth: OAuth
        val httpClient: Call.Factory
        val httpCallContext: CoroutineContext
        val temporaryCredentialStore: TemporaryCredentialStore
    }

    suspend fun obtainRedirectUrl(
        clientCredential: ClientCredential,
        twitterLoginCallbackAbsoluteUrl: String
    ): String {
        val unauthorizedRequest = Request.Builder()
            .method("POST", RequestBody.create(null, ByteArray(0)))
            .url("https://api.twitter.com/oauth/request_token")
            .build()
        val additionalProtocolParameters = listOf(ProtocolParameter.Callback(twitterLoginCallbackAbsoluteUrl))
        val authorizedRequest = authorize(unauthorizedRequest, clientCredential, additionalProtocolParameters)

        println("Async start")
        val temporaryCredential = async(env.httpCallContext) {
            println("Async start 2")
            try {
                env.httpClient.newCall(authorizedRequest).execute()
            } catch (exception: IOException) {
                throw TwitterCallFailedException("Request couldn't be executed", exception)
            }.use { response ->
                if (!response.isSuccessful) {
                    throw TwitterCallFailedException("Response not successful (status code : ${response.code()})")
                }
                val body = response.body()?.bytes()
                if (body != null) {
                    val pairs = parseWwwFormUrlEncoded(body).toMap()
                    val oauthToken = pairs["oauth_token"]
                    val oauthTokenSecret = pairs["oauth_token_secret"]
                    if (oauthToken == null || oauthTokenSecret == null) {
                        throw TwitterCallFailedException("Unexpected response content (response body : $body)")
                    } else {
                        TemporaryCredential(oauthToken, oauthTokenSecret)
                    }
                } else {
                    throw TwitterCallFailedException("Not expected response content (response body is null)")
                }
            }.also {
                println("Async finish 2")
            }
        }.await()
        println("Async finish")
        env.temporaryCredentialStore.saveTemporaryCredential(temporaryCredential)
        return "https://api.twitter.com/oauth/authenticate?oauth_token=${encodeURLQueryComponent(temporaryCredential.token)}"
    }

    private fun authorize(
        unauthorizedRequest: Request,
        clientCredential: ClientCredential,
        additionalProtocolParameters: List<ProtocolParameter<*>>
    ): Request {
        val httpRequest = HttpRequest(unauthorizedRequest.method(), unauthorizedRequest.url().url())
        val protocolParameters = env.oauth.generateProtocolParametersSigningWithHmacSha1(
            httpRequest,
            clientCredentials = OAuthCredentials(clientCredential.identifier, clientCredential.sharedSecret),
            temporaryOrTokenCredentials = null,
            additionalProtocolParameters = additionalProtocolParameters
        )
        val authorizationHeaderString = ParameterTransmission.getAuthorizationHeaderString(protocolParameters, "")
        return unauthorizedRequest.newBuilder().header("Authorization", authorizationHeaderString).build()
    }

}
