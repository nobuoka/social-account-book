import * as wd from "webdriverio";

(async () => {
    await main();
})().catch(e => {
    console.error(e);
    process.exit(1);
});

async function main() {
    let wdClient = wd.remote({
        port: 9516,
        path: "/",
        desiredCapabilities: <any> {
            "moz:firefoxOptions": {
                "args": ["-headless"]
            }
        }
    });
    let session = wdClient.init();
    try {
        await session.url('http://localhost:8080/');
        let title = await session.getTitle();
        assertEqual(title, "Social B/S")
    } finally {
        await session.end();
    }
}

function assertEqual(actual: string, expected: string) {
    if (actual !== expected) {
        throw Error("Assertion failure (actual: " + actual + ", expected: " + expected + ")");
    }
}
