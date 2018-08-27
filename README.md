# Simter File Server

## Requirement

- Java 8+
- Kotlin 1.2.31+
- Maven 3.5.2+
- Spring Framework 5+
- Spring Boot 2+
- Reactor 3+

## Maven Modules

Sn | Name                              | Parent                        | Remark
---|-----------------------------------|-------------------------------|--------
1  | [simter-file-build]               | [simter-build:0.6-SNAPSHOT]   | Build modules and define global properties and pluginManagement
2  | [simter-file-dependencies]        | simter-file-build             | Define global dependencyManagement
3  | [simter-file-parent]              | simter-file-dependencies      | All sub modules parent module, Define global dependencies and plugins
4  | [simter-file-data]                | simter-file-parent            | Define Service and Dao Interfaces
5  | [simter-file-data-reactive-mongo] | simter-file-parent            | Dao Implementation By Reactive MongoDB
6  | [simter-file-data-jpa]            | simter-file-parent            | Dao Implementation By JPA
7  | [simter-file-rest-webflux]        | simter-file-parent            | Rest API By WebFlux
8  | [simter-file-starter]             | simter-file-parent            | Microservice Starter
     
Remark : Module 1, 2, 3 all has maven-enforcer-plugin and flatten-maven-plugin config. Other modules must not configure them.

[simter-build:0.6-SNAPSHOT]: https://github.com/simter/simter-build/tree/master
[simter-file-build]: https://github.com/simter/simter-file
[simter-file-dependencies]: https://github.com/simter/simter-file/tree/master/simter-file-dependencies
[simter-file-parent]: https://github.com/simter/simter-file/tree/master/simter-file-parent
[simter-file-data]: https://github.com/simter/simter-file/tree/master/simter-file-data
[simter-file-data-reactive-mongo]: https://github.com/simter/simter-file/tree/master/simter-file-data-reactive-mongo
[simter-file-data-jpa]: https://github.com/simter/simter-file/tree/master/simter-file-data-jpa
[simter-file-rest-webflux]: https://github.com/simter/simter-file/tree/master/simter-file-rest-webflux
[simter-file-starter]: https://github.com/simter/simter-file/tree/master/simter-file-starter