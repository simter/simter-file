package tech.simter.file.core.domain

import tech.simter.file.core.domain.DynamicBean
import tech.simter.file.core.domain.Attachment
import javax.persistence.Entity
import javax.persistence.Id

/**
 * The dto of the [Attachment]'s full path and designated nearest ancestor
 *
 * @author zh
 * */
@Entity
class AttachmentDto4FullPath : DynamicBean() {
  @get:Id
  var id: String? by holder
  var fullPath: String? by holder
}