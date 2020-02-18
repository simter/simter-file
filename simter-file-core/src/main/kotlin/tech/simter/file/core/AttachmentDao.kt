package tech.simter.file.core

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.file.core.domain.Attachment
import tech.simter.file.core.domain.AttachmentDto4Zip
import tech.simter.file.core.domain.AttachmentDtoWithChildren
import java.util.*

/**
 * The [Attachment] Dao Interface.
 *
 * @author cjw
 * @author RJ
 * @author zh
 */
interface AttachmentDao {
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
   * Delete [Attachment] by id.
   * If the attachment to be deleted is a folder type, recursively delete its descendants
   *
   * @param[ids] the ids to delete
   * @return [Flux] The full path of the exists attachments to be deleted.
   *   If all specify attachments not exists, return [Flux.empty]
   */
  fun delete(vararg ids: String): Flux<String>

  /**
   * Get ths full path of the specific attachment.
   *
   * @param[id] the attachment's id
   * @return [Mono] the full path relative to `{file-root}` path or [Mono.empty] if none found.
   */
  fun getFullPath(id: String): Mono<String>

  /**
   * Recursively find all the children of the specific upper's [id].
   *
   * @return the children that include its children recursively.
   *   If the upper has no children or the upper is not exists, return [Flux.empty]
   */
  fun findDescendants(id: String): Flux<AttachmentDtoWithChildren>

  /**
   *  Update part of the fields in the attachment
   *
   * @param[id] the attachment's id
   * @param[data] The fields that will be modified
   * @return [Mono] signaling when operation has completed
   *   If the attachment is not exists, return [Mono.error] with [NotFoundException].
   */
  fun update(id: String, data: Map<String, Any?>): Mono<Void>

  /**
   * Find [AttachmentDto4Zip] from the descendants of attachments and attachments.
   *   If not found, return [Flux.empty].
   *   [AttachmentDto4Zip.zipPath] is logical path relatively to the recent common ancestor of file attachments
   *     and it not include the file suffix .
   *   [AttachmentDto4Zip.physicalPath] is the physical path of the file.
   * @param[ids] the attachments id.
   * @return order by [AttachmentDto4Zip.zipPath] asc
   */
  fun findDescendantsZipPath(vararg ids: String): Flux<AttachmentDto4Zip>

  /**
   * Find and returns [Attachment]s distinct puid collection.
   * If not found, return [Flux.empty]
   *
   * @param[ids] the attachments id.
   */
  fun findPuids(vararg ids: String): Flux<Optional<String>>
}