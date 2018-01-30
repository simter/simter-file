package tech.simter.file.dao.jpa

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.Repository
import tech.simter.file.po.Attachment
import java.util.*

/**
 * The block JPA-DAO Repository. See [CrudRepository].
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
}