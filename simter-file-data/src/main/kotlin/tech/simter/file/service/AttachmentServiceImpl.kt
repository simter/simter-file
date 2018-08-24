package tech.simter.file.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.po.Attachment

/**
 * The attachment service implementation.
 *
 * @author cjw
 * @author RJ
 */
@Component
class AttachmentServiceImpl @Autowired constructor(val attachmentDao: AttachmentDao) : AttachmentService {
  override fun get(id: String): Mono<Attachment> {
    return attachmentDao.get(id)
  }

  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    return attachmentDao.find(pageable)
  }

  override fun find(puid: String, subgroup: Short?): Flux<Attachment> {
    return attachmentDao.find(puid, subgroup)
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    return attachmentDao.save(*attachments)
  }

  override fun delete(vararg ids: String): Mono<Void> {
    return attachmentDao.delete(*ids)
  }
}