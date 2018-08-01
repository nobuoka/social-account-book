package info.vividcode.sbs.main.presentation

import info.vividcode.sbs.main.core.domain.User
import kotlinx.html.*

internal fun topHtml(
    m: TopHtmlPresentationModel
): suspend TagConsumer<*>.() -> Unit = {
    html {
        head {
            meta(name = "viewport", content = "width=device-width,initial-scale=1")
            title("Social B/S")
            styleLink("/static/css/main.css")
        }
        body {
            h1 { +"Social B/S" }

            when (m) {
                is TopHtmlPresentationModel.LoginUser -> {
                    div {
                        a(href = m.userPrivateHomePath) { +"User : ${m.actor.displayName}" }
                    }
                    form(method = FormMethod.post, action = m.logoutPath) {
                        submitInput {
                            value = "Sign out"
                        }
                    }
                }
                is TopHtmlPresentationModel.AnonymousUser -> {
                    form(method = FormMethod.post, action = m.twitterLoginPath) {
                        submitInput {
                            value = "Sign in with Twitter"
                        }
                    }
                }
            } as? Any?
        }
    }
}

internal sealed class TopHtmlPresentationModel {
    class LoginUser(val actor: User, val logoutPath: String, val userPrivateHomePath: String) :
        TopHtmlPresentationModel()

    class AnonymousUser(val twitterLoginPath: String) :
        TopHtmlPresentationModel()
}
