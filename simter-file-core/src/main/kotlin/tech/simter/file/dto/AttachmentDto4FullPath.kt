package tech.simter.file.dto

import tech.simter.file.common.DynamicBean
import tech.simter.file.po.Attachment
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