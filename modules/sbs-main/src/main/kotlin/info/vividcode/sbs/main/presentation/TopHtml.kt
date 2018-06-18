package info.vividcode.sbs.main.presentation

import kotlinx.html.*

fun topHtml(
    twitterLoginPath: String
): suspend TagConsumer<*>.() -> Unit = {
    html {
        head {
            title("Social B/S")
            styleLink("/static/css/main.css")
        }
        body {
            h1 { +"Social B/S" }
            form(method = FormMethod.post, action = twitterLoginPath) {
                submitInput {
                    value = "Sign in with Twitter"
                }
            }
        }
    }
}
