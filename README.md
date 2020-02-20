# Simter File Server

A file manage web server.

## Data Structure

*Database table name is st_attachment**. Different database should have different column type, 
check database script from [here](./simter-file-core/src/main/resources/tech/simter/file/sql).

## Maven Modules

Sn | Name                       | Parent             | Remark
---|----------------------------|--------------------|--------
1  | [simter-file]              | [simter-build]     | Build these modules and define global properties and pluginManagement
2  | [simter-file-bom]          | simter-file        | BOM
3  | [simter-file-parent]       | simter-file        | Define global dependencies and plugins
4  | [simter-file-core]         | simter-file-parent | Core API: [Attachment], [AttachmentDao] and [AttachmentService]
5  | [simter-file-dao-mongo]    | simter-file-parent | [AttachmentDao] Implementation By Reactive MongoDB
6  | [simter-file-dao-r2dbc]    | simter-file-parent | [AttachmentDao] Implementation By R2DBC
7  | [simter-file-dao-jpa]      | simter-file-parent | [AttachmentDao] Implementation By JPA
8  | [simter-file-rest-webflux] | simter-file-parent | [Rest API] Implementation By WebFlux
9  | [simter-file-starter]      | simter-file-parent | Microservice Starter

## Requirement

- Maven 3.6+
- Kotlin 1.3+
- Java 8+
- Spring Framework 5.2+
- Spring Boot 2.2+
- Reactor 3.3+


[simter-build]: https://github.com/simter/simter-build/tree/master
[simter-file]: https://github.com/simter/simter-file
[simter-file-bom]: https://github.com/simter/simter-file/tree/master/simter-file-bom
[simter-file-parent]: https://github.com/simter/simter-file/tree/master/simter-file-parent
[simter-file-core]: https://github.com/simter/simter-file/tree/master/simter-file-core
[simter-file-dao-mongo]: https://github.com/simter/simter-file/tree/master/simter-file-dao-mongo
[simter-file-dao-r2dbc]: https://github.com/simter/simter-file/tree/master/simter-file-dao-r2dbc
[simter-file-dao-jpa]: https://github.com/simter/simter-file/tree/master/simter-file-dao-jpa
[simter-file-rest-webflux]: https://github.com/simter/simter-file/tree/master/simter-file-rest-webflux
[simter-file-starter]: https://github.com/simter/simter-file/tree/master/simter-file-starter

[Attachment]: https://github.com/simter/simter-file/blob/master/simter-file-core/src/main/kotlin/tech/simter/file/core/Attachment.kt
[AttachmentDao]: https://github.com/simter/simter-file/blob/master/simter-file-core/src/main/kotlin/tech/simter/file/core/AttachmentDao.kt
[AttachmentService]: https://github.com/simter/simter-file/blob/master/simter-file-core/src/main/kotlin/tech/simter/file/core/AttachmentService.kt
[Rest API]: ./docs/rest-api.md