package tech.simter.file.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.dto.AttachmentDto4Update
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.po.Attachment

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
   * Create or update one or more [Attachment].
   *
   * @param[attachments] the attachments to save or update
   * @return [Mono] signaling when operation has completed
   */
  fun save(vararg attachments: Attachment): Mono<Void>

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
   * If the specified path already exists, return [Mono.error] with [PermissionDeniedException].
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
   *  If the specified path already exists, return [Flux.error] with [PermissionDeniedException].
   */
  fun create(vararg attachments: Attachment): Flux<String>
}