# Simter File Server

## Requirement

- Maven 3.5.2+
- Kotlin 1.2.31+
- Java 8+
- Spring Framework 5+
- Spring Boot 2+
- Reactor 3+

## Maven Profiles

Environment | Profile            | Persistence        | Remark
------------|--------------------|--------------------|--------
Development |dev-reactive-mongo  | [Embedded MongoDB] | Reactive
Development |dev-jpa-hsql        | [HyperSQL]         | JPA
Production  |prod-reactive-mongo | [MongoDB]          | Reactive
Production  |prod-jpa-postgres   | [PostgreSQL]       | JPA

The default profile is `dev-reactive-mongo`. Run bellow command to start:

```bash
mvn spring-boot:run -P {profile-name}
```

Default server-port is 9013, use `-D port=9013` to change to another port.

## Maven Properties

Property Name | Default Value | Remark
--------------|---------------|--------
port          | 9013          | Web server port
db.host       | localhost     | Database host
db.name       | file          | Database name
db.username   | file          | Database connect username
db.password   | password      | Database connect password
db.init-mode  | never         | Init database by `spring.datasource.schema/data` config. `never` or `always`
Use `-D {property-name}={property-value}` to override default value. Such as:

```bash
mvn spring-boot:run -D port=9091
```

## Build Production

```bash
mvn clean package -P prod-{xxx}
```

## Run Production

```bash
java -jar {package-name}.jar

# or
nohup java -jar {package-name}.jar > /dev/null &
```

## Run Integration Test

Run test in the real server.

1. Start server. Such as `mvn spring-boot:run`
2. Run [IntegrationTest.kt]


[Embedded MongoDB]: https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo#embedded-mongodb
[MongoDB]: https://www.mongodb.com
[HyperSQL]: http://hsqldb.org
[PostgreSQL]: https://www.postgresql.org
[IntegrationTest.kt]: https://github.com/simter/simter-file/blob/master/simter-file-starter/src/test/kotlin/tech/simter/file/starter/IntegrationTest.kt