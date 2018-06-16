社会的貸借対照表 (Social B/S)
==========

[![CircleCI](https://circleci.com/gh/nobuoka/social-balance-sheet.svg?style=svg)](https://circleci.com/gh/nobuoka/social-balance-sheet)
[![codecov](https://codecov.io/gh/nobuoka/social-balance-sheet/branch/master/graph/badge.svg)](https://codecov.io/gh/nobuoka/social-balance-sheet)

## Heroku

This application runs on Heroku.

* https://vcsbs.herokuapp.com/

[Procfile](./Procfile) file and Gradle `:app:stage` task are defined for Heroku.

* See : [Deploying Gradle Apps on Heroku | Heroku Dev Center](https://devcenter.heroku.com/articles/deploying-gradle-apps-on-heroku)

## Application configuration

### Environment variables

| Name | Example |
|---|---|
| SBS_CONTEXT_URL | https://vcsbs.herokuapp.com/ |
| SBS_TWITTER_CLIENT_IDENTIFIER | YOUR_TWITTER_APP_IDENTIFIER |
| SBS_TWITTER_CLIENT_SECRET | YOUR_TWITTER_APP_SHARED_SECRET |
