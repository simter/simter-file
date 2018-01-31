# Simter File Server Starter

## Requirement

- Java 8+
- Maven 3.5.2+
- Spring Framework 5+
- Spring Boot 2+
- Reactor 3+

## Maven Profiles

Environment | Profile      | Persistence           | Remark
----------|------------|--------------------|-------
Development |dev           | [Embedded MongoDB] | Reactive mongo
Development |dev-hsql      | [HyperSQL]          | JPA
Development |dev-postgres  | [PostgreSQL]        | JPA
Production  |prod          | [MongoDB]           | Reactive mongo
Production  |prod-postgres | [PostgreSQL]        | JPA

The default profile is `dev`. Run bellow command to start:

```bash
mvn spring-boot:run -P {profile-name}
```

Default server-port is 9013, use `-D port=9013` to change to another port.

## Maven Properties

Property Name | Default Value | Remark
------------|-------------|-------
port          | 9013          | Web server port
db.host       | localhost     | Database host
db.name       | file          | Database name
db.username   | file          | Database connect username
db.password   | password      | Database connect password

Use `-D {property-name}={property-value}` to override default value. Such as:

```bash
mvn spring-boot:run -D port=9999
```

## Build Production Package

```bash
mvn clean package -P prod
```

## Run Production Package

```bash
java -jar {package-name}.jar

# or
nohup java -jar {package-name}.jar > /dev/null &
```


[Embedded MongoDB]: https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo#embedded-mongodb
[MongoDB]: https://www.mongodb.com
[HyperSQL]: http://hsqldb.org
[PostgreSQL]: https://www.postgresql.org