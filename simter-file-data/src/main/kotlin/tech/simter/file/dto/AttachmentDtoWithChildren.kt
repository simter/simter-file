package tech.simter.file.dto

import tech.simter.file.po.Attachment

/**
 * The dto of the [Attachment] basic information plus descendents information
 *
 * @author zh
 * */
class AttachmentDtoWithChildren : AttachmentDto() {
  var children: List<AttachmentDtoWithChildren>? by holder

  fun copy(attachment: Attachment): AttachmentDtoWithChildren {
    this.id = attachment.id
    this.name = attachment.name
    this.type = attachment.type
    this.size = attachment.size
    this.modifyOn = attachment.modifyOn
    this.modifier = attachment.modifier
    return this
  }

  fun copy(node: AttachmentDtoWithUpper): AttachmentDtoWithChildren {
    this.id = node.id
    this.name = node.name
    this.type = node.type
    this.size = node.size
    this.modifyOn = node.modifyOn
    this.modifier = node.modifier
    return this
  }
}