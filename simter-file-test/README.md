# simter-file-test

## 1. Unit test tools [TestHelper.kt]

A unit test tools for generate random value:

- `fun randomString(len: Int = 36): String`
- `fun randomModuleValue(): String`
- `fun randomFileId(): String`
- `fun randomFileStore(id, module, name, type, size, ts): FileStore`
- `fun randomAuthenticatedUser(id, account, name): SystemContext.User`

## 2. Integration test

### 2.1. Start server

For test purpose, start the test server:

```shell
$ cd ../simter-file-starter
$ mvn clean spring-boot:run
```

> Ignore this if test on another server.

### 2.2. Run integration test on server

```shell
$ cd ../simter-file-test
$ mvn clean test -P integration-test
```

This will run all the integration test on each rest-api define in <[rest-api.md]>.

Want to run the integration test on the real server, just add two specific params:

| ParamName  | Remark               | Default value
|------------|----------------------|---------------
| server.url | server address       | http://127.0.0.1:9013/file
| data.dir   | server base data dir | ../simter-file-starter/target/data

Such as:

```shell
$ mvn clean test -P integration-test -D server.url=http://127.0.0.1:9013/file
```


[TestHelper.kt]: https://github.com/simter/simter-file/blob/master/simter-file-test/src/main/kotlin/tech/simter/file/test/TestHelper.kt
[rest-api.md]: https://github.com/simter/simter-file/blob/master/docs/rest-api.md
