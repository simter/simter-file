package tech.simter.file.dto

import tech.simter.file.po.Attachment

/**
 * The dto of the [Attachment] create field
 *
 * @author zh
 * */
class AttachmentDto4Create : AttachmentDto4Update() {
  var id: String? by holder
}