package info.vividcode.sbs.main.presentation.up

import info.vividcode.sbs.main.core.domain.Account
import info.vividcode.sbs.main.core.domain.AccountBook
import info.vividcode.sbs.main.core.domain.User
import kotlinx.html.*

internal fun userPrivateAccountBookHtml(
        actor: User,
        accountBook: AccountBook,
        accounts: List<Account>,
        logoutPath: String,
        userAccountsPath: String
): TagConsumer<*>.() -> Unit = {
    html {
        head {
            meta(name = "viewport", content = "width=device-width,initial-scale=1")
            title("Account book : ${accountBook.label} - Social B/S")
            styleLink("/static/css/main.css")
        }
        body {
            userPrivateHeaderFragment(actor, logoutPath)

            div {
                h1 { +"Account book : ${accountBook.label}" }

                h2 { +"Accounts"}
                h3 { +"Add account" }
                form(method = FormMethod.post, action = userAccountsPath) {
                    textInput(name = "label")
                    hiddenInput(name = "account-book-id") {
                        value = "${accountBook.id}"
                    }
                    submitInput {
                        value = "Create account"
                    }
                }
                h3 { +"Current accounts" }
                if (accounts.isEmpty()) {
                    span { +"Not added yet." }
                } else {
                    ul {
                        accounts.forEach { account ->
                            li { +account.label }
                        }
                    }
                }
            }
        }
    }
}
