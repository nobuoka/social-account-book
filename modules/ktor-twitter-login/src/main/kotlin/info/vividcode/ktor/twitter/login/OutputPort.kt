package info.vividcode.ktor.twitter.login

import info.vividcode.ktor.twitter.login.application.TemporaryCredentialNotFoundException
import info.vividcode.ktor.twitter.login.application.TwitterCallFailedException
import io.ktor.application.ApplicationCall
import io.ktor.pipeline.PipelineContext

typealias OutputInterceptor<T> = suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit

interface OutputPort {
    val success: OutputInterceptor<TwitterToken>
    val twitterCallFailed: OutputInterceptor<TwitterCallFailedException>
    val temporaryCredentialNotFound: OutputInterceptor<TemporaryCredentialNotFoundException>
}
