package tech.simter.file.dto

import tech.simter.file.po.Attachment

/**
 * The dto of the [Attachment] basic information plus descendents information
 *
 * @author zh
 * */
class AttachmentDtoWithChildren : AttachmentDto() {
  var children: List<AttachmentDtoWithChildren>? by holder
}