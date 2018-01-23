package tech.simter.file.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
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
  override fun create(attachment: Mono<Attachment>): Mono<Attachment> {
    return attachmentDao.save(attachment.block() as Attachment)
  }
}