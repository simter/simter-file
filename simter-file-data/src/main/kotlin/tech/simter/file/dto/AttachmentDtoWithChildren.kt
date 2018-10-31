package tech.simter.file.dto

class AttachmentDtoWithChildren : AttachmentDto() {
  var children: List<AttachmentDtoWithChildren>? by holder
}