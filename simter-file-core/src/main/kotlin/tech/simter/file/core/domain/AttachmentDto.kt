package tech.simter.file.core.domain

import tech.simter.kotlin.beans.AbstractDynamicBean
import java.time.OffsetDateTime

/**
 * The dto of the [Attachment] basic information
 *
 * @author zh
 * */
open class AttachmentDto : AbstractDynamicBean() {
  var id: String? by holder
  var name: String? by holder
  var type: String? by holder
  var size: Long? by holder
  var modifyOn: OffsetDateTime? by holder
  var modifier: String? by holder
}