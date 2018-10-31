package tech.simter.file.dto

import tech.simter.file.common.DynamicBean
import java.time.OffsetDateTime

open class AttachmentDto : DynamicBean() {
  var id: String? by holder
  var name: String? by holder
  var type: String? by holder
  var size: Long? by holder
  var modifyOn: OffsetDateTime? by holder
  var modifier: String? by holder
}