package tech.simter.file.dto

import tech.simter.file.common.DynamicBean
import tech.simter.file.po.Attachment

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