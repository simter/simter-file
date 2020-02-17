package tech.simter.file.core.domain

import tech.simter.kotlin.beans.AbstractDynamicBean
import java.time.OffsetDateTime
import javax.persistence.MappedSuperclass

/**
 * The dto of the [Attachment] basic information
 *
 * @author zh
 */
@MappedSuperclass
open class AttachmentDto : AbstractDynamicBean() {
  @get:javax.persistence.Id
  var id: String? by holder
  var name: String? by holder
  var type: String? by holder
  var size: Long? by holder
  var modifyOn: OffsetDateTime? by holder
  var modifier: String? by holder
}