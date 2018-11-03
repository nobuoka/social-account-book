package info.vividcode.sbs.main.presentation.up

import info.vividcode.sbs.main.core.domain.User
import kotlinx.html.*

internal fun BODY.userPrivateHeaderFragment(
        actor: User,
        logoutPath: String
) {
    header {
        div(classes = "service-name") {
            span { +"Social B/S" }
        }

        div(classes = "actor-info") {
            div {
                span { +"Hi, ${actor.displayName}!" }
            }
            form(method = FormMethod.post, action = logoutPath) {
                submitInput {
                    value = "Sign out"
                }
            }
        }
    }
}
