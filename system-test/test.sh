#!/bin/sh
set -xeu

base_url=${TEST_BASE_URL:-"http://localhost:8080"}

curl $base_url/ > result
test "$(cat result)" = "Hello world!"
