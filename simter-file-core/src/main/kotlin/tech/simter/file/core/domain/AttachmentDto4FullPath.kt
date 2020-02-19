package tech.simter.file.core.domain

import tech.simter.kotlin.beans.AbstractDynamicBean
import javax.persistence.Entity
import javax.persistence.Id

/**
 * The dto of the [Attachment]'s full path and designated nearest ancestor
 *
 * @author zh
 * */
@Entity
class AttachmentDto4FullPath : AbstractDynamicBean() {
  @get:Id
  var id: String? by holder
  var fullPath: String? by holder
}