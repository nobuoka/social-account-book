CREATE TABLE "users" (
  "id" BIGSERIAL PRIMARY KEY,
  "display_name" VARCHAR(128) NOT NULL
);

CREATE TABLE "twitter_temporary_credentials" (
  "identifier" VARCHAR(512) NOT NULL PRIMARY KEY,
  "shared_secret" VARCHAR(512) NOT NULL
);
