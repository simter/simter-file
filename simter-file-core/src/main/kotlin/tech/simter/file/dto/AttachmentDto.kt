package tech.simter.file.dto

import tech.simter.file.common.DynamicBean
import tech.simter.file.po.Attachment
import java.time.OffsetDateTime
import javax.persistence.Id
import javax.persistence.MappedSuperclass

/**
 * The dto of the [Attachment] basic information
 *
 * @author zh
 * */
@MappedSuperclass
open class AttachmentDto : DynamicBean() {
  @get:Id
  var id: String? by holder
  var name: String? by holder
  var type: String? by holder
  var size: Long? by holder
  var modifyOn: OffsetDateTime? by holder
  var modifier: String? by holder
}