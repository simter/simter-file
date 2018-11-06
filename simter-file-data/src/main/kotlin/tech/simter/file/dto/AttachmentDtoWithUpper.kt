package tech.simter.file.dto

import tech.simter.file.po.Attachment
import javax.persistence.Entity

/**
 * The dto of the [Attachment] basic information plus upper information
 *
 * @author zh
 * */
@Entity
class AttachmentDtoWithUpper : AttachmentDto() {
  var upperId: String? by holder
}