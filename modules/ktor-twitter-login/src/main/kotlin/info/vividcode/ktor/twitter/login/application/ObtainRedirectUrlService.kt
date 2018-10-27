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
import kotlinx.coroutines.experimental.withContext
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

        val temporaryCredential = withContext(env.httpCallContext) {
            try {
                env.httpClient.newCall(authorizedRequest).execute()
            } catch (exception: IOException) {
                throw TwitterCallFailedException("Request couldn't be executed", exception)
            }.use { response ->
                val body = response.body()?.bytes()

                if (!response.isSuccessful) {
                    val percentEncoded = body?.joinToString("") { String.format("%%%02X", it) }
                    throw TwitterCallFailedException(
                        "Response not successful (status code : ${response.code()}, " +
                                "percent-encoded response body : $percentEncoded))"
                    )
                }

                if (body != null) {
                    val pairs = parseWwwFormUrlEncoded(body).toMap()
                    val oauthToken = pairs["oauth_token"]
                    val oauthTokenSecret = pairs["oauth_token_secret"]
                    if (oauthToken == null || oauthTokenSecret == null) {
                        val percentEncoded = body.joinToString("") { String.format("%%%02X", it) }
                        throw TwitterCallFailedException(
                            "Unexpected response content " +
                                    "(percent-encoded response body : $percentEncoded)"
                        )
                    } else {
                        TemporaryCredential(oauthToken, oauthTokenSecret)
                    }
                } else {
                    throw TwitterCallFailedException("Not expected response content (response body is null)")
                }
            }
        }
        env.temporaryCredentialStore.saveTemporaryCredential(temporaryCredential)
        return "https://api.twitter.com/oauth/authenticate?oauth_token=${temporaryCredential.token.encodeURLQueryComponent()}"
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
