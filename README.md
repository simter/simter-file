# Simter File Server

## Requirement

- Java 8+
- Maven 3.5.2+
- Spring Framework 5+
- Spring Boot 2+
- Reactor 3+

## Maven Modules

Name                                   | Parent                   | Remark
----------------------------------|----------------------|-------
[simter-file-build]                 | [simter-build:0.4.0]  |
[simter-file-dependencies]         | simter-file-build        |
[simter-file-parent]                | simter-file-dependencies |
[simter-file-data]                  | simter-file-parent       |
[simter-file-data-reactive-mongo] | simter-file-parent       |
[simter-file-data-jpa]              | simter-file-parent       |
[simter-file-rest-webflux]         | simter-file-parent       |
[simter-file-starter]               | simter-file-parent       |


[simter-build:0.4.0]: https://github.com/simter/simter-build/tree/0.4.0
[simter-file-build]: https://github.com/simter/simter-file
[simter-file-dependencies]: https://github.com/simter/simter-file/tree/master/simter-file-dependencies
[simter-file-parent]: https://github.com/simter/simter-file/tree/master/simter-file-parent
[simter-file-data]: https://github.com/simter/simter-file/tree/master/simter-file-data
[simter-file-data-reactive-mongo]: https://github.com/simter/simter-file/tree/master/simter-file-data-reactive-mongo
[simter-file-data-jpa]: https://github.com/simter/simter-file/tree/master/simter-file-data-jpa
[simter-file-rest-webflux]: https://github.com/simter/simter-file/tree/master/simter-file-rest-webflux
[simter-file-starter]: https://github.com/simter/simter-file/tree/master/simter-file-starter