package tech.simter.file.dao.jpa

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.Repository
import tech.simter.file.po.Attachment
import java.util.*

/**
 * The block JPA-DAO Repository. See [CrudRepository], [PagingAndSortingRepository] and [SimpleJpaRepository].
 *
 * @author RJ
 */
//@RepositoryDefinition(domainClass = Attachment::class, idClass = String::class)
interface AttachmentJpaDao : Repository<Attachment, String> {
  /**
   * Create a new Attachment.
   *
   * See [CrudRepository.save].
   *
   * @param attachment the attachment
   * @return the saved attachment
   */
  fun save(attachment: Attachment): Attachment

  /**
   * Find an Attachment by its id.
   *
   * See [CrudRepository.findById].
   *
   * @param id the identity
   * @return the attachment with the given id or [Optional.empty] if none found
   */
  fun findById(id: String): Optional<Attachment>

  /**
   * Returns a [Page] of attachments meeting the paging restriction provided in the `Pageable` object.
   *
   * @param pageable page options
   * @return a page of attachments
   */
  fun findAll(pageable: Pageable): Page<Attachment>
}