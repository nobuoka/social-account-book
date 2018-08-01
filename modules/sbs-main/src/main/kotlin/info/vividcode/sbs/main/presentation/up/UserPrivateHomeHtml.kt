package info.vividcode.sbs.main.presentation.up

import info.vividcode.sbs.main.core.domain.Account
import info.vividcode.sbs.main.core.domain.User
import kotlinx.html.*

internal fun userPrivateHomeHtml(
    actor: User,
    userAccounts: List<Account>,
    logoutPath: String,
    userAccountsPath: String
): suspend TagConsumer<*>.() -> Unit = {
    html {
        head {
            meta(name = "viewport", content = "width=device-width,initial-scale=1")
            title("Social B/S")
            styleLink("/static/css/main.css")
        }
        body {
            h1 { +"Social B/S" }

            div {
                span { +"Hi, ${actor.displayName}!" }
            }
            form(method = FormMethod.post, action = logoutPath) {
                submitInput {
                    value = "Sign out"
                }
            }

            div {
                h2 { +"Your accounts"}
                h3 { +"Add account" }
                form(method = FormMethod.post, action = userAccountsPath) {
                    textInput(name = "label")
                    submitInput {
                        value = "Create account"
                    }
                }
                h3 { +"Current accounts" }
                if (userAccounts.isEmpty()) {
                    span { +"Not added yet." }
                } else {
                    ul {
                        userAccounts.forEach { userAccount ->
                            li { +userAccount.label }
                        }
                    }
                }
            }
        }
    }
}
