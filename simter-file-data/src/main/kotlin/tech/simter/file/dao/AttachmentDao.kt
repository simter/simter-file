package tech.simter.file.dao

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import tech.simter.file.po.Attachment

/**
 * Interface for generic CRUD operations on the attachment.
 *
 * @author cjw
 */
interface AttachmentDao : ReactiveCrudRepository<Attachment, String>