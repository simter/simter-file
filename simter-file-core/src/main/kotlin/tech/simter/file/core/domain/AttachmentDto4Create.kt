package tech.simter.file.core.domain

/**
 * The dto of the [Attachment] create field
 *
 * @author zh
 * */
class AttachmentDto4Create : AttachmentDto4Update() {
  var id: String? by holder
  var type: String? by holder
}