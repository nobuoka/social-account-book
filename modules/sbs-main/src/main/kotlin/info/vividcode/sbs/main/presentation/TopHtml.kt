package info.vividcode.sbs.main.presentation

import info.vividcode.sbs.main.core.domain.User
import kotlinx.html.*

fun topHtml(
    actor: User?,
    twitterLoginPath: String,
    logoutPath: String
): suspend TagConsumer<*>.() -> Unit = {
    html {
        head {
            meta(name = "viewport", content = "width=device-width,initial-scale=1")
            title("Social B/S")
            styleLink("/static/css/main.css")
        }
        body {
            h1 { +"Social B/S" }

            if (actor != null) {
                div {
                    span { +"User : ${actor.displayName}" }
                }
                form(method = FormMethod.post, action = logoutPath) {
                    submitInput {
                        value = "Sign out"
                    }
                }
            } else {
                form(method = FormMethod.post, action = twitterLoginPath) {
                    submitInput {
                        value = "Sign in with Twitter"
                    }
                }
            }
        }
    }
}
