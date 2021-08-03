# simter-file changelog

## 2.0.0-M4 - 2021-08-03

- Upgrade to simter-3.0.0-M3 (spring-boot-2.5.3)
- Simplify unit test class name
  > Such as rename `Delete...Test` to `DeleteTest`
- simter-file-test:
    - Rename property `server-url` to `server.url`
    - Rename property `base-data-url` to `data.url`
- dao-jpa - Upgrade to new database init config
- dao-r2dbc - Upgrade to new database init config and delete embedded database profile
- starter - Upgrade to new database init config and delete embedded database profile

## 2.0.0-M3 - 2021-05-26

- Support custom upload file size by param 'size'
- Not need to decode pathVariable value
- Fixed module value mapped
- Implement delete file interface by [PR#79]

[PR#79]: https://github.com/simter/simter-file/pull/79

## 2.0.0-M2 - 2021-04-27

- Upgrade to simter-3.0.0-M2 (spring-boot-2.4.5)
- Remove jackson dependency
- Change parameter offset to Long type

## 2.0.0-M1 - 2020-12-28

- Upgrade to simter-3.0.0-M1 (spring-boot-2.4.1)
- Upgrade to kotlinx-serialization-1.0.1

## 1.0.0 - 2020-11-20

- Upgrade to simter-2.0.0 (spring-boot-2.3.6)
- First stable version
- Simplify property simter-file.server-url to server-url (on simter-file-test)
- Simplify property simter-file.base-data-dir to base-data-dir (on simter-file-test)

## 0.10.0 2020-06-28

- Add a module simter-file-service-impl
- Upgrade to simter-2.0.0-M1

## 0.9.0 2020-04-17

- Polishing - root page info
- Polishing - code format
- Polishing - remove unused import
- Use simter-kotlin module's share kotlin json instance
- Move 'tech.simter.file.core.Page' to simter-kotlin module
- Upgrade to simter-1.3.0-M14

## 0.8.2 2020-04-09

- Rename authorization key
- Log main config property on starter with info level
- Package starter finalName with platform type as suffix
- Set starter default profile to r2dbc-embedded-h2
- Add db.options property for embedded-h2, see [r2dbc-h2/issues#19](https://github.com/r2dbc/r2dbc-h2/issues/19)
- Rename default prod database name st_file to file

## 0.8.1 2020-03-15

- Implement new dao api by jpa
- Create SQL for hsql
- Create SQL for derby
- Create SQL for mysql
- Create SQL for mssql

## 0.8.0 2020-03-13

**This version redesign all api include rest, service, dao, domain interface.**

Target to make the api clear and simple. 

- Redesign all api interface
- Implement new dao api by r2dbc
- Implement new rest-api by WebFlux
- Add integration test on real server
- Temporary remove dao-mongo and dao-jpa submodule compilation until new implementation

## 0.7.0 2020-03-04

**Breaking api changes:**

- Upload file by stream should return 201 with location header
- Upload file by form submit should return 201 with location header
- Rename property 'simter.file.root' to 'simter-file.root-dir'

**Bug fixed and improved:**

- Add integration test:
    - find module attachments
    - download file by inline mode
    - upload file by form submit
    - upload file by stream
    - Initial integration test config
- Add new module simter-file-test for common unit test
- Make simter-file root dir can be config by maven property 'simter-file.root-dir'
- Should not config default.operations by default
- Use 'System' as creator if without authenticated user info when upload file
- Remove mockito dependency
- Minimize hikari config
- Remove javax.persistence-api dependency on core module
- Make server port configurable by maven property `server.port`
- Add maven property `module.rest-context-path.simter-file` in starter
- Add module authorizer config sample on starter
- Rename SUB_MODULES_AUTHORIZER_KEY to MODULES_AUTHORIZER_KEY

## 0.6.0 2020-02-21

- Refactor groupId to `tech.simter.file` [#67]
- Refactor module structure to make core api simplify and clear [#70]
- Add r2dbc dao implementation [#66]
- Support static file on starter [#68]
- Add rest-api.md from [github wiki](https://github.com/simter/simter-file.wiki.git) and replenish missing rest-api items，see [docs/rest-api.md](./docs/rest-api.md)

[#66]: https://github.com/simter/simter-file/issues/66
[#67]: https://github.com/simter/simter-file/issues/67
[#68]: https://github.com/simter/simter-file/issues/68
[#70]: https://github.com/simter/simter-file/issues/70

## 0.5.0 2020-02-14

For older merge release.

- [PR#64] Add access control (there are too much commits)
- [PR#63]
    - Design and implement `AttachmentService.uploadFile(attachment: Attachment, writer: (File) -> Mono<Void>) : Mono<Void>`
    - Design and implement `AttachmentService.reuploadFile(dto: AttachmentDto, fileData: ByteArray): Mono<Void>`
    - Remove `AttachmentService.save(vararg attachments: Attachment): Mono<Void>`
    - Refactor `UploadFileByFormHandler`
    - Refactor `ReuploadFileByStreamHandler`
- [PR#62] Add reactive mongo dao implementation

[PR#64]: https://github.com/simter/simter-file/pull/64
[PR#63]: https://github.com/simter/simter-file/pull/63
[PR#62]: https://github.com/simter/simter-file/pull/62

## 0.4.0 2019-01-14

- Upgrade to simter-build-1.1.0 and simter-dependencies-1.1.0

## 0.3.0 2019-01-10

- Upgrade to simter platform 1.0.0

## 0.2.0 2018-12-04

- Fixed jpa find-descendents unit test failed
- Upgrade to simter platform 0.7.0

## 0.1.3 2018-12-03

- Change the default file store directory to `/data/file`
- Implement `Service|Dao.delete(vararg ids: String)` interface method
- Refactoring po field
    - `ext` rename to `type` and use specific value ":d" to express directory
    - `uploadOn` rename to `createOn`
    - `uploader` rename to `creator`
    - `subgroup` rename to `upperId` for support tree structure, default value is "EMPTY"
    - Add `modifyOn`, `modifier`
- Add upload file by stream rest url
- Add `Service.getFullPath(id: String)`
- Add `Service.update(id: String, dto: AttachmentDto4Update)`
- Add `Service.findDescendents(id: String)`
- Add `Service.create(vararg attachments: Attachment)`
- Add `ReuploadFileByStreamHandler`
- Add `CreateAttachmentsHandler`
- Add `FindAttechmentDescendentsHandler`
- Add `UpdateAttachmentHandler`
- Add `DeleteNumerousFilesHandler`
- Add `PackageFilesHandler`

## 0.1.2 2018-08-29

- Fix find by module and subgroup sort in mongodb dao

## 0.1.1 2018-08-27

- Add inline file handler and handler router config
- Rename 'simter.version.file' to 'module.version.simter-file'
- Rename 'simter.rest.context-path.file:/' to 'module.rest-context-path.simter-file:/file'

## 0.1.0 2018-08-27

- Initial base on simter platform 0.6.0