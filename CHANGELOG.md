# simter-file changelog

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