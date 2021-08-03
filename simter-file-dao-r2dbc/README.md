# simter-file-dao-r2dbc

The [FileDao] implementation by [R2DBC].

## Unit test

Run below command to test the compatibility with different database:

```
mvn test -P h2 \
&& mvn test -P postgres \
&& mvn test -P mysql
```

> Could change the host database connection params through below `Maven Properties`.

## Maven Properties

| Property Name | Default Value | Remark                    |
|---------------|---------------|---------------------------|
| db.host       | localhost     | Database connect host     |
| db.port       |               | Database connect port     |
| db.name       | testdb        | Database name             |
| db.url        |               | Database connect url      |
| db.username   | tester        | Database connect username |
| db.password   | password      | Database connect password |

Use `-D {property-name}={property-value}` to override default value. Such as:

```bash
mvn test -D db.name=testdb
```

## Maven Profiles:

| Name     | Default | Supported |
|----------|:-------:|:---------:|
| h2       |    √    |     √     |
| postgres |         |     √     |
| mysql    |         |           |
| mssql    |         |           |

The default profile is `h2`.
Use `-P {profile-name}` to override default. Such as:

```bash
mvn test -P {profile-name}
```

[R2DBC]: https://r2dbc.io
[FileDao]: https://github.com/simter/simter-file/blob/master/simter-file-core/src/main/kotlin/tech/simter/file/core/FileDao.kt
