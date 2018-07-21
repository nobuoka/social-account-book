@file:JvmName("Application")

package info.vividcode.sbs.main

import info.vividcode.ktor.twitter.login.*
import info.vividcode.ktor.twitter.login.application.TemporaryCredentialNotFoundException
import info.vividcode.ktor.twitter.login.application.TwitterCallFailedException
import info.vividcode.oauth.OAuth
import info.vividcode.sbs.main.presentation.topHtml
import info.vividcode.sbs.main.presentation.withHtmlDoctype
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.content.resources
import io.ktor.content.static
import io.ktor.content.staticBasePackage
import io.ktor.http.ContentType.Text.Html
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.response.respondWrite
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.time.Clock
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext
import com.zaxxer.hikari.HikariDataSource
import info.vividcode.orm.db.JdbcOrmContexts
import info.vividcode.orm.db.JdbcTransactionManager
import info.vividcode.sbs.main.application.CreateNewSessionService
import info.vividcode.sbs.main.application.DeleteSessionService
import info.vividcode.sbs.main.application.RetrieveActorUserService
import info.vividcode.sbs.main.database.*
import info.vividcode.sbs.main.infrastructure.web.appendCookieSessionId
import info.vividcode.sbs.main.infrastructure.web.clearCookieSessionId
import info.vividcode.sbs.main.infrastructure.web.getCookieSessionIdOrNull
import io.ktor.request.ApplicationRequest
import io.ktor.routing.post
import org.flywaydb.core.Flyway

fun Application.setup(env: Env? = null) {
    val appContextUrl = environment.config.property("sbs.contextUrl").getString()
    val appDatabaseJdbcUrl = environment.config.property("sbs.databaseJdbcUrl").getString()
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

    val dbAccessContexts = newFixedThreadPoolContext(4, "DbAccess")
    val transactionManager = JdbcTransactionManager(
        JdbcOrmContexts.createProviderFactoryFor(AppOrmContext::class, dbAccessContexts),
        appDataSource
    )

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

    val retrieveActorUserService = RetrieveActorUserService(transactionManager)
    val createNewSessionService = CreateNewSessionService(transactionManager)
    val deleteSessionService = DeleteSessionService(transactionManager)

    intercept(ApplicationCallPipeline.Call) {
        try {
            proceed()
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun ApplicationRequest.getActorUserOrNull(): UserTuple? =
        getCookieSessionIdOrNull()?.let { sessionId ->
            retrieveActorUserService.retrieveActorUserOrNull(sessionId)
        }

    routing {
        staticBasePackage = "sbs.main"
        static("static") {
            resources("static")
        }

        get(UrlPaths.top) {
            val actorUserOrNull = call.request.getActorUserOrNull()
            val htmlOutput = withHtmlDoctype(topHtml(actorUserOrNull, UrlPaths.TwitterLogin.start, UrlPaths.logout))
            call.respondWrite(Html, OK, htmlOutput)
        }

        post(UrlPaths.logout) {
            call.request.getCookieSessionIdOrNull()?.let { deleteSessionService.deleteSession(it) }
            call.response.clearCookieSessionId()
            call.respondRedirect(UrlPaths.top, false)
        }

        setupTwitterLogin(
            UrlPaths.TwitterLogin.start, UrlPaths.TwitterLogin.callback,
            appContextUrl, twitterClientCredentials, envNotNull,
            object : OutputPort {
                override val success: OutputInterceptor<TwitterToken> = { token ->
                    val sessionId =
                        createNewSessionService.createNewSessionByTwitterLogin(token.userId.toLong(), token.screenName)
                    call.response.appendCookieSessionId(sessionId)
                    call.respondRedirect(UrlPaths.top, false)
                }
                override val twitterCallFailed: OutputInterceptor<TwitterCallFailedException> = {
                    call.respond(HttpStatusCode.BadGateway, "Failed to request for Twitter")
                }
                override val temporaryCredentialNotFound: OutputInterceptor<TemporaryCredentialNotFoundException> = {
                    call.respond(HttpStatusCode.BadRequest, "Temporary credential not found")
                }
            })
    }
}
