package tech.simter.file.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDto4Update
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.po.Attachment
import javax.persistence.PersistenceException

/**
 * The attachment service implementation.
 *
 * @author cjw
 * @author RJ
 * @author zh
 */
@Component
class AttachmentServiceImpl @Autowired constructor(val attachmentDao: AttachmentDao) : AttachmentService {
  override fun create(vararg attachments: Attachment): Flux<String> {
    return attachmentDao.save(*attachments).thenMany(attachments.map { it.id }.toFlux())
      .onErrorResume(PersistenceException::class.java) {
        Flux.error(PermissionDeniedException("The specified path already exists"))
      }
  }

  override fun findDescendents(id: String): Flux<AttachmentDtoWithChildren> {
    return attachmentDao.findDescendents(id)
  }

  override fun update(id: String, dto: AttachmentDto4Update): Mono<Void> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getFullPath(id: String): Mono<String> {
    return attachmentDao.getFullPath(id)
      .switchIfEmpty(Mono.error(NotFoundException("The attachment $id not exists")))
  }

  override fun get(id: String): Mono<Attachment> {
    return attachmentDao.get(id)
  }

  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    return attachmentDao.find(pageable)
  }

  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    return attachmentDao.find(puid, upperId)
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    return attachmentDao.save(*attachments)
  }

  override fun delete(vararg ids: String): Mono<Void> {
    return attachmentDao.delete(*ids)
  }

}