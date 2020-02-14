package tech.simter.file.core.domain

import org.springframework.data.annotation.AccessType
import tech.simter.file.core.domain.Attachment
import tech.simter.file.core.domain.AttachmentDto
import javax.persistence.Entity

/**
 * The dto of the [Attachment] basic information plus upper information
 *
 * @author zh
 * */
@Entity
@AccessType(AccessType.Type.PROPERTY)
class AttachmentDtoWithUpper : AttachmentDto() {
  var upperId: String? by holder
}