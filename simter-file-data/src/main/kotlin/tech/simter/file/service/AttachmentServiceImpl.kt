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
 */
@Component
class AttachmentServiceImpl : AttachmentService {
  @Autowired lateinit var attachmentDao: AttachmentDao

  override fun create(attachment: Mono<Attachment>): Mono<Attachment> {
    if (attachment == null) throw IllegalArgumentException("Can't saving the null attachment!")
    return attachmentDao.save(attachment.block())
  }
}