package tech.simter.file.core.domain

import tech.simter.file.core.domain.DynamicBean
import tech.simter.file.core.domain.Attachment

/**
 * The dto of the [Attachment] update field
 *
 * @author zh
 * */
open class AttachmentDto4Update : DynamicBean() {
  var name: String? by holder
  var upperId: String? by holder
  var path: String? by holder
  var puid: String? by holder
}