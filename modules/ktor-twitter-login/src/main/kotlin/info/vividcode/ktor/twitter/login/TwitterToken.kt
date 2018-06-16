package info.vividcode.ktor.twitter.login

data class TwitterToken(
    val token: String,
    val sharedSecret: String,
    val userId: String,
    val screenName: String
)
