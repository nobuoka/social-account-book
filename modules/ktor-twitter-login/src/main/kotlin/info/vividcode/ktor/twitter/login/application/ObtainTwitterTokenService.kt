package info.vividcode.ktor.twitter.login.application

import info.vividcode.ktor.twitter.login.*
import info.vividcode.oauth.HttpRequest
import info.vividcode.oauth.OAuth
import info.vividcode.oauth.OAuthCredentials
import info.vividcode.oauth.ProtocolParameter
import info.vividcode.oauth.protocol.ParameterTransmission
import info.vividcode.whatwg.url.parseWwwFormUrlEncoded
import kotlinx.coroutines.experimental.async
import okhttp3.Call
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import kotlin.coroutines.experimental.CoroutineContext

class ObtainTwitterTokenService(
    private val env: Required
) {
    interface Required {
        val oauth: OAuth
        val httpClient: Call.Factory
        val httpCallContext: CoroutineContext
        val temporaryCredentialStore: TemporaryCredentialStore
    }

    suspend fun obtainTwitterToken(
        clientCredential: ClientCredential,
        oauthToken: String,
        oauthVerifier: String
    ): TwitterToken {
        val temporaryCredential = env.temporaryCredentialStore.findTemporaryCredential(oauthToken)
                ?: throw TemporaryCredentialNotFoundException()

        val unauthorizedRequest = Request.Builder()
            .method("POST", RequestBody.create(null, ByteArray(0)))
            .url("https://api.twitter.com/oauth/access_token")
            .build()
        val authorizedRequest = authorize(
            unauthorizedRequest,
            clientCredential,
            temporaryCredential,
            listOf(ProtocolParameter.Verifier(oauthVerifier))
        )

        return async(env.httpCallContext) {
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
                    val token = pairs["oauth_token"]
                    val tokenSecret = pairs["oauth_token_secret"]
                    val userId = pairs["user_id"]
                    val screenName = pairs["screen_name"]
                    if (token == null || tokenSecret == null || userId == null || screenName == null) {
                        val percentEncoded = body.joinToString("") { String.format("%%%02X", it) }
                        throw TwitterCallFailedException(
                            "Unexpected response content " +
                                    "(percent-encoded response body : $percentEncoded)"
                        )
                    } else {
                        TwitterToken(token, tokenSecret, userId, screenName)
                    }
                } else {
                    throw TwitterCallFailedException("Not expected response content (response body is null)")
                }
            }
        }.await()
    }

    private fun authorize(
        unauthorizedRequest: Request,
        clientCredential: ClientCredential,
        temporaryCredential: TemporaryCredential,
        additionalProtocolParameters: List<ProtocolParameter<*>>
    ): Request {
        val httpRequest = HttpRequest(unauthorizedRequest.method(), unauthorizedRequest.url().url())
        val protocolParameters = env.oauth.generateProtocolParametersSigningWithHmacSha1(
            httpRequest,
            clientCredentials = OAuthCredentials(clientCredential.identifier, clientCredential.sharedSecret),
            temporaryOrTokenCredentials = OAuthCredentials(temporaryCredential.token, temporaryCredential.secret),
            additionalProtocolParameters = additionalProtocolParameters
        )
        val authorizationHeaderString = ParameterTransmission.getAuthorizationHeaderString(protocolParameters, "")
        return unauthorizedRequest.newBuilder().header("Authorization", authorizationHeaderString).build()
    }

}
