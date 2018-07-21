package info.vividcode.sbs.main.auth.domain

import info.vividcode.orm.where
import info.vividcode.sbs.main.auth.domain.infrastructure.AuthOrmContext
import info.vividcode.sbs.main.auth.domain.infrastructure.TwitterUserConnectionTuple
import info.vividcode.sbs.main.auth.domain.infrastructure.TwitterUserTuple
import info.vividcode.sbs.main.core.domain.User

data class SessionId(val value: Long)

fun AuthOrmContext.getUserIdConnectedToTwitterAccount(twitterUserId: Long): Long? =
    run {
        twitterUserConnectionsRelation.select(where {
            TwitterUserConnectionTuple::twitterUserId eq twitterUserId
        }).forUpdate().firstOrNull()?.userId
    }

fun AuthOrmContext.createTwitterUserConnection(user: User, twitterUserId: Long, twitterScreenName: String) {
    val twitterUser = twitterUsers.select(where {
        TwitterUserTuple::twitterUserId eq twitterUserId
    }).forUpdate().firstOrNull()
    if (twitterUser == null) {
        twitterUsers.insert(
            TwitterUserTuple(
                twitterUserId,
                twitterScreenName
            )
        )
    }
    twitterUserConnectionsRelation.insert(
        TwitterUserConnectionTuple(
            user.id,
            twitterUserId
        )
    )
}
