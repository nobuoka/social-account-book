package info.vividcode.sbs.main

internal class ApplicationTest {
//
//    private val testServer = TestServerTwitter()
//    private val testTemporaryCredentialStore = TestTemporaryCredentialStore()
//
//    private val testEnv = object : Env {
//        override val oauth: OAuth = OAuth(object : OAuth.Env {
//            override val clock: Clock = Clock.fixed(Instant.parse("2015-01-01T00:00:00Z"), ZoneId.systemDefault())
//            override val nextInt: (Int) -> Int = { (it - 1) / 2 }
//        })
//        override val httpClient: Call.Factory = TestCallFactory(testServer)
//        override val httpCallContext: CoroutineContext = newSingleThreadContext("TestHttpCall")
//        override val temporaryCredentialStore: TemporaryCredentialStore = testTemporaryCredentialStore
//    }
//
//    @Test
//    fun testRequest() = withTestApplication({ setup(testEnv) }) {
//        testServer.responseBuilders.add(responseBuilder {
//            code(200).message("OK")
//            body(
//                ResponseBody.create(
//                    MediaType.parse("application/x-www-form-urlencoded"),
//                    "oauth_token=test-temporary-token&oauth_token_secret=test-token-secret"
//                )
//            )
//        })
//    }

}
