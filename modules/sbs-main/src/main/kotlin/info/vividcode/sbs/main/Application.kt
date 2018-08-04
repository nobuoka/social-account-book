@file:JvmName("Application")

package info.vividcode.sbs.main

import com.zaxxer.hikari.HikariDataSource
import info.vividcode.ktor.twitter.login.*
import info.vividcode.ktor.twitter.login.application.TemporaryCredentialNotFoundException
import info.vividcode.ktor.twitter.login.application.TwitterCallFailedException
import info.vividcode.oauth.OAuth
import info.vividcode.sbs.main.auth.application.CreateNewSessionService
import info.vividcode.sbs.main.auth.application.DeleteSessionService
import info.vividcode.sbs.main.auth.application.RetrieveActorUserService
import info.vividcode.sbs.main.auth.application.TemporaryCredentialStoreImpl
import info.vividcode.sbs.main.core.application.CreateUserAccountService
import info.vividcode.sbs.main.core.application.CreateUserService
import info.vividcode.sbs.main.core.application.FindUserAccountsService
import info.vividcode.sbs.main.core.application.FindUserService
import info.vividcode.sbs.main.core.domain.User
import info.vividcode.sbs.main.infrastructure.database.createTransactionManager
import info.vividcode.sbs.main.infrastructure.web.SessionCookieEncrypt
import info.vividcode.sbs.main.infrastructure.web.SessionCookieHandler
import info.vividcode.sbs.main.presentation.TopHtmlPresentationModel
import info.vividcode.sbs.main.presentation.topHtml
import info.vividcode.sbs.main.presentation.up.userPrivateHomeHtml
import info.vividcode.sbs.main.presentation.withHtmlDoctype
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.content.resources
import io.ktor.content.static
import io.ktor.content.staticBasePackage
import io.ktor.http.ContentType.Text.Html
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.Parameters
import io.ktor.request.ApplicationRequest
import io.ktor.request.ContentTransformationException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondWrite
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.flywaydb.core.Flyway
import java.time.Clock
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext

fun Application.setup(env: Env? = null) {
    val appContextUrl = environment.config.property("sbs.contextUrl").getString()
    val appDatabaseJdbcUrl = "jdbc:h2:mem:sbs_dev;TRACE_LEVEL_FILE=4"
    val appSessionEncryptionKey = environment.config.property("sbs.session.encryptionKey").getString().toByteArray()
    val appSessionSignKey = environment.config.property("sbs.session.signKey").getString().toByteArray()
    val twitterClientCredentials = environment.config.config("sbs.twitter.clientCredential").let {
        ClientCredential(it.property("identifier").getString(), it.property("sharedSecret").getString())
    }

    val appDataSource = HikariDataSource().apply {
        jdbcUrl = appDatabaseJdbcUrl
        isAutoCommit = false
    }

    val flyway = Flyway()
    flyway.dataSource = appDataSource
    flyway.migrate()

    val transactionManager = createTransactionManager(appDataSource)

    val envNotNull = env
            ?: object : Env {
                override val oauth: info.vividcode.oauth.OAuth = OAuth(object : OAuth.Env {
                    override val clock: Clock = Clock.systemDefaultZone()
                    override val nextInt: (Int) -> Int = Random()::nextInt
                })
                override val httpClient: okhttp3.Call.Factory = OkHttpClient.Builder()
                    .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                    .build()
                override val httpCallContext: CoroutineContext = newFixedThreadPoolContext(4, "HttpCall")
                override val temporaryCredentialStore: TemporaryCredentialStore =
                    TemporaryCredentialStoreImpl(transactionManager)
            }

    val findUerService = FindUserService.create(transactionManager)
    val createUserService = CreateUserService.create(transactionManager)
    val retrieveActorUserService = RetrieveActorUserService(transactionManager, findUerService)
    val createNewSessionService = CreateNewSessionService(transactionManager, findUerService, createUserService)
    val deleteSessionService = DeleteSessionService(transactionManager)

    val findUserAccountsService = FindUserAccountsService(transactionManager)
    val createUserAccountService = CreateUserAccountService(transactionManager)

    intercept(ApplicationCallPipeline.Call) {
        try {
            proceed()
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    val sessionCookieHandler =
        SessionCookieHandler(SessionCookieEncrypt(appSessionEncryptionKey, appSessionSignKey).createCodec())

    suspend fun ApplicationRequest.getActorUserOrNull(): User? =
        sessionCookieHandler.getCookieSessionIdOrNull(call.request)?.let { sessionId ->
            retrieveActorUserService.retrieveActorUserOrNull(sessionId)
        }

    routing {
        staticBasePackage = "sbs.main"
        static("static") {
            resources("static")
        }

        get(UrlPaths.top) {
            val actorUserOrNull = call.request.getActorUserOrNull()
            val m = if (actorUserOrNull != null) {
                val userPrivateHomePath = actorUserOrNull.let { UrlPaths.UserPrivate.Home.concrete(it) }
                TopHtmlPresentationModel.LoginUser(actorUserOrNull, UrlPaths.logout, userPrivateHomePath)
            } else {
                TopHtmlPresentationModel.AnonymousUser(UrlPaths.TwitterLogin.start)
            }
            val htmlOutput = withHtmlDoctype(topHtml(m))
            call.respondWrite(Html, OK, htmlOutput)
        }

        post(UrlPaths.logout) {
            sessionCookieHandler.getCookieSessionIdOrNull(call.request)?.let {
                deleteSessionService.deleteSession(it)
            }
            sessionCookieHandler.clearCookieSessionId(call.response)
            call.respondRedirect(UrlPaths.top, false)
        }

        get(UrlPaths.UserPrivate.Home.parameterized) {
            call.request.getActorUserOrNull()?.let { actorUser ->
                val targetUserId = UrlPaths.UserPrivate.Home.getUserId(call)?.toLongOrNull()

                if (actorUser.id != targetUserId) {
                    // Not found.
                } else {
                    val userAccounts = findUserAccountsService.findUserAccounts(actorUser)
                    val userAccountsPath = UrlPaths.UserPrivate.Accounts.concrete(actorUser)
                    val htmlOutput =
                        withHtmlDoctype(userPrivateHomeHtml(actorUser, userAccounts, UrlPaths.logout, userAccountsPath))
                    call.respondWrite(Html, OK, htmlOutput)
                }
            }
        }

        post(UrlPaths.UserPrivate.Accounts.parameterized) {
            call.request.getActorUserOrNull()?.let { actorUser ->
                val targetUserId = UrlPaths.UserPrivate.Accounts.getUserId(call)?.toLongOrNull()

                val requestBody = try {
                    call.receive<Parameters>()
                } catch (e: ContentTransformationException) {
                    log.warn("Request body cannot be received.", e)
                    Parameters.Empty
                }
                val label = requestBody["label"] ?: "(Label not specified)"

                if (actorUser.id != targetUserId) {
                    // Not found.
                } else {
                    createUserAccountService.createUserAccount(actorUser, label)
                    val userPrivateHomePath = UrlPaths.UserPrivate.Home.concrete(actorUser)
                    call.respondRedirect(userPrivateHomePath, false)
                }
            }
        }

        setupTwitterLogin(
            UrlPaths.TwitterLogin.start, UrlPaths.TwitterLogin.callback,
            appContextUrl, twitterClientCredentials, envNotNull,
            object : OutputPort {
                override val success: OutputInterceptor<TwitterToken> = { token ->
                    val sessionId =
                        createNewSessionService.createNewSessionByTwitterLogin(token.userId.toLong(), token.screenName)
                    sessionCookieHandler.appendCookieSessionId(call.response, sessionId)
                    call.respondRedirect(UrlPaths.top, false)
                }
                override val twitterCallFailed: OutputInterceptor<TwitterCallFailedException> = {
                    it.printStackTrace()
                    call.respond(HttpStatusCode.BadGateway, "Failed to request for Twitter")
                }
                override val temporaryCredentialNotFound: OutputInterceptor<TemporaryCredentialNotFoundException> = {
                    call.respond(HttpStatusCode.BadRequest, "Temporary credential not found")
                }
            })
    }
}
