# Simter File Server

A file manage server.

## Data Structure

**Database table name is st_file**. A different database should have different column type, 
check database script from [here](./simter-file-core/src/main/resources/tech/simter/file/sql).

## Maven Modules

| Sn  | Name                     | Type | Parent                | Remark                                                                |
|-----|--------------------------|------|-----------------------|-----------------------------------------------------------------------|
| 1   | [simter-file]            | pom  | [simter-dependencies] | Build these modules and define global properties and pluginManagement |
| 2   | simter-file-bom          | pom  | simter-file           | BOM                                                                   |
| 3   | simter-file-parent       | pom  | simter-file           | Define global dependencies and plugins                                |
| 4   | simter-file-core         | jar  | simter-file-parent    | Core API: [FileStore], [FileDao] and [FileService]                    |
| 5   | simter-file-test         | jar  | simter-file-parent    | Common unit test helper method                                        |
| 6   | simter-file-dao-r2dbc    | jar  | simter-file-parent    | [FileDao] Implementation By R2DBC                                     |
| 7   | simter-file-dao-mongo    | jar  | simter-file-parent    | TODO: [FileDao] Implementation By Reactive MongoDB                    |
| 8   | simter-file-dao-jpa      | jar  | simter-file-parent    | [FileDao] Implementation By JPA                                       |
| 9   | simter-file-dao-web      | jar  | simter-file-parent    | TODO: [FileDao] Implementation By WebFlux                             |
| 10  | simter-file-service-impl | jar  | simter-file-parent    | Default [FileService] Implementation                                  |
| 11  | simter-file-rest-webflux | jar  | simter-file-parent    | [Rest API] Implementation By WebFlux                                  |
| 12  | simter-file-starter      | jar  | simter-file-parent    | Microservice Starter                                                  |

## Requirement

- Java 17+
- Maven 3.8+
- Spring Boot 2.7+
    - Spring Framework 5.3+
    - Kotlin 1.6+
    - Reactor 3.4+


[simter-dependencies]: https://github.com/simter/simter-dependencies
[simter-file]: https://github.com/simter/simter-file

[FileStore]: https://github.com/simter/simter-file/blob/master/simter-file-core/src/main/kotlin/tech/simter/file/core/File.kt
[FileDao]: https://github.com/simter/simter-file/blob/master/simter-file-core/src/main/kotlin/tech/simter/file/core/FileStoreDao.kt
[FileService]: https://github.com/simter/simter-file/blob/master/simter-file-core/src/main/kotlin/tech/simter/file/core/FileStoreService.kt
[Rest API]: ./docs/rest-api.md