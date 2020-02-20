package tech.simter.file.core.domain

import javax.persistence.Entity
import javax.persistence.Id

/**
 * The dto of the [Attachment]'s full path and designated nearest ancestor
 *
 * @author zh
 * */
@Entity
data class AttachmentDto4FullPath(
  @Id
  val id: String,
  val fullPath: String
)