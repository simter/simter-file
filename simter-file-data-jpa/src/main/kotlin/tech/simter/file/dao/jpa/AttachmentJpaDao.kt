package tech.simter.file.dao.jpa

import org.springframework.data.repository.Repository
import tech.simter.file.po.Attachment

/**
 * The block JPA-DAO Repository.
 *
 * @author RJ
 */
//@RepositoryDefinition(domainClass = Attachment::class, idClass = String::class)
interface AttachmentJpaDao : Repository<Attachment, String> {
  /** Create a new Attachment */
  fun <S : Attachment?> save(entity: S): S
}