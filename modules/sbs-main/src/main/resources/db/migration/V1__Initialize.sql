CREATE TABLE "users" (
  "id" BIGSERIAL PRIMARY KEY,
  "display_name" VARCHAR(128) NOT NULL
);

CREATE TABLE "login_sessions" (
  "id" BIGSERIAL PRIMARY KEY,
  "user_id" BIGINT NOT NULL
);

CREATE TABLE "twitter_users" (
  "twitter_user_id" BIGINT NOT NULL PRIMARY KEY,
  "twitter_user_name" VARCHAR(128) NOT NULL
);

CREATE TABLE "twitter_user_connections" (
  "user_id" BIGINT NOT NULL,
  "twitter_user_id" BIGINT NOT NULL
);

CREATE TABLE "twitter_temporary_credentials" (
  "identifier" VARCHAR(512) NOT NULL PRIMARY KEY,
  "shared_secret" VARCHAR(512) NOT NULL
);
