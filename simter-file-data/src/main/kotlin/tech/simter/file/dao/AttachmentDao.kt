package tech.simter.file.dao

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment

/**
 * The [Attachment] Dao Interface.
 *
 * @author cjw
 * @author RJ
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
   * Returns a [Flux] of [Attachment]'s by puid and subgroup.
   *
   * @param[puid] the module identity
   * @param[subgroup] the subgroup from specify module
   * @return [Flux] emitting attachments or a empty flux without data if none found
   */
  fun find(puid: String, subgroup: Short?): Flux<Attachment>

  /**
   * Create or update one or more [Attachment].
   *
   * @param[attachments] the attachments to save or update
   * @return [Mono] signaling when operation has completed
   */
  fun save(vararg attachments: Attachment): Mono<Void>

  /**
   * Delete [Attachment] by its id.
   *
   * @param[ids] the ids to delete
   * @return [Mono] signaling when operation has completed
   */
  fun delete(vararg ids: String): Mono<Void>
}