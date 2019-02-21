package tech.simter.file.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.dto.AttachmentDto
import tech.simter.file.dto.AttachmentDto4Update
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.po.Attachment
import java.io.File
import java.io.OutputStream

/**
 * The [Attachment] Service Interface.
 *
 * This interface is design for all external modules to use.
 *
 * @author cjw
 * @author zh
 */
interface AttachmentService {
  /**
   *  Get an [Attachment] by its id.
   *
   *  @param[id] the id for matching.
   *  @return [Mono] emitting the [Attachment] with the given id or [Mono.empty] if none found.
   */
  fun get(id: String): Mono<Attachment>

  /**
   * Returns a [Page] of [Attachment]'s meeting the paging restriction provided in the [Pageable] object.
   *
   * @param[pageable] the pageable option
   * @return [Mono] emitting a page of attachments contains data or a empty page without data if none found
   */
  fun find(pageable: Pageable): Mono<Page<Attachment>>

  /**
   * Returns a [Flux] of [Attachment]'s by puid and upperId.
   *
   * @param[puid] the module identity
   * @param[upperId] the upperId from specify module
   * @return [Flux] emitting attachments or a empty flux without data if none found
   */
  fun find(puid: String, upperId: String?): Flux<Attachment>

  /**
   * Get ths full path of the specific attachment.
   *
   * If the attachment is not exists, return [Mono.error] with [NotFoundException].
   *
   * @param[id] the attachment's id
   * @return [Mono] the full path relative to `{file-root}` path
   */
  fun getFullPath(id: String): Mono<String>

  /**
   * Delete [Attachment] and physics file by its id.
   * If specify [Attachment] not exists then ignore and handle as success.
   *
   * @param[ids] the ids to delete
   * @return [Mono] signaling when operation has completed
   */
  fun delete(vararg ids: String): Mono<Void>

  /**
   * Update part of [AttachmentDto4Update] field in [Attachment] by id.
   * If the attachment is not exists, return [Mono.error] with [NotFoundException].
   *
   * @param[id] The id of the attachment to be updated
   * @return[Mono] signaling when operation has completed
   */
  fun update(id: String, dto: AttachmentDto4Update): Mono<Void>

  /**
   * Recursively find all the children of the specific upper's [id].
   *
   * @return the children that include its children recursively.
   *   If the upper has no children or the upper is not exists, return [Flux.empty]
   */
  fun findDescendents(id: String): Flux<AttachmentDtoWithChildren>

  /**
   * Create one or more [Attachment]
   *
   * @return[Flux] emitting id of the newly created attachments.
   *   If [Attachment] belong to different puid, return [Flux.error] with [ForbiddenException].
   *   If user don't have permission, return [Flux.error] with [PermissionDeniedException].
   */
  fun create(vararg attachments: Attachment): Flux<String>

  /**
   * Package attachments and their descendant attachments.
   *   if attachments don't have least-common-ancestors, the zip structure is
   *     |- {sub-folder.name}
   *       |- {sub-folder.name} or {sub-file.name}.{sub-file.type}
   *       |- ...
   *     |- {sub-folder.name} or {sub-file.name}.{sub-file.type}
   *     |- ...
   *   if attachments have least-common-ancestors and it is file, the zip structure is
   *     |- {sub-file.name}.{sub-file.type}
   *   if attachments have least-common-ancestors and it is folder, the zip structure is
   *     |- {least-common-ancestors.name}
   *       |- {sub-folder.name}
   *         |- {sub-folder.name} or {sub-file.name}.{sub-file.type}
   *         |- ...
   *       |- {sub-folder.name} or {sub-file.name}.{sub-file.type}
   *       |- ...
   * @param[outputStream] zip file data output position.
   * @param[ids] the attachments id.
   * @return[Mono] default name of the zip file.
   *   if attachments is not exists, return [Mono.empty]
   *   if attachments don't have least-common-ancestors, the name is "root.zip";
   *   if attachments have least-common-ancestors and it is file,
   *     the name is "{least-common-ancestors.name}.{least-common-ancestors.type}.zip";
   *   if attachments have least-common-ancestors and it is folder,
   *     the name is "{least-common-ancestors.name}.zip".
   */
  fun packageAttachments(outputStream: OutputStream, vararg ids: String): Mono<String>

  /**
   * create one file [attachment] and save physical file.
   *
   * If upperId is not exists, return [Mono.error] with [NotFoundException].
   * And if new file or it's upper folder is creation failed, return [Mono.error] with [IllegalAccessException].
   *
   * @param[attachment] the new attachment.
   * @param[writer] a function of writes the file data to [File] and return an [Mono.empty]
   *
   * @return[Mono] signaling when operation has completed.
   */
  fun uploadFile(attachment: Attachment, writer: (File) -> Mono<Void>): Mono<Void>

  /**
   * save physical file and update the [AttachmentDto].
   *
   * If [Attachment] is not exists, return [Mono.error] with [NotFoundException].
   * And if new file or it's upper folder is creation failed, return [Mono.error] with [IllegalAccessException].
   *
   * @param[dto] the [Attachment] will modify part of the value.
   * @param[fileData] the reupload file data.
   *
   * @return[Mono] signaling when operation has completed.
   */
  fun reuploadFile(dto: AttachmentDto, fileData: ByteArray): Mono<Void>
}