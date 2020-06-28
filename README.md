# Simter File Server

A file manage server.

## Data Structure

*Database table name is st_file**. A different database should have different column type, 
check database script from [here](./simter-file-core/src/main/resources/tech/simter/file/sql).

## Maven Modules

Sn | Name                     | Parent             | Remark
---|--------------------------|--------------------|--------
1  | [simter-file]            | [simter-build]     | Build these modules and define global properties and pluginManagement
2  | simter-file-bom          | simter-file        | BOM
3  | simter-file-parent       | simter-file        | Define global dependencies and plugins
4  | simter-file-core         | simter-file-parent | Core API: [FileStore], [FileDao] and [FileService]
5  | simter-file-dao-mongo    | simter-file-parent | [FileDao] Implementation By Reactive MongoDB
6  | simter-file-dao-r2dbc    | simter-file-parent | [FileDao] Implementation By R2DBC
7  | simter-file-dao-jpa      | simter-file-parent | [FileDao] Implementation By JPA
8  | simter-file-rest-webflux | simter-file-parent | [Rest API] Implementation By WebFlux
9  | simter-file-starter      | simter-file-parent | Microservice Starter

## Requirement

- Maven 3.6+
- Kotlin 1.3+
- Java 8+
- Spring Framework 5.2+
- Spring Boot 2.3+
- Reactor 3.3+


[simter-build]: https://github.com/simter/simter-build
[simter-file]: https://github.com/simter/simter-file

[FileStore]: https://github.com/simter/simter-file/blob/master/simter-file-core/src/main/kotlin/tech/simter/file/core/File.kt
[FileDao]: https://github.com/simter/simter-file/blob/master/simter-file-core/src/main/kotlin/tech/simter/file/core/FileStoreDao.kt
[FileService]: https://github.com/simter/simter-file/blob/master/simter-file-core/src/main/kotlin/tech/simter/file/core/FileStoreService.kt
[Rest API]: ./docs/rest-api.md