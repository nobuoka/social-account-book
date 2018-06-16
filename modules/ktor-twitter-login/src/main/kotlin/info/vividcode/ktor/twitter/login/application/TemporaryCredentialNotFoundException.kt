package info.vividcode.ktor.twitter.login.application

class TemporaryCredentialNotFoundException(val temporaryCredentialIdentifier: String) : Exception()
