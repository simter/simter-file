package tech.simter.file.impl.domain

import tech.simter.file.core.domain.Attachment
import java.time.OffsetDateTime

data class AttachmentImpl(
  override val id: String,
  override val path: String,
  override val name: String,
  override val type: String,
  override val size: Long,
  override val createOn: OffsetDateTime,
  override val creator: String,
  override val modifyOn: OffsetDateTime,
  override val modifier: String,
  override val puid: String? = null,
  override val upperId: String? = null
) : Attachment {
  companion object {
    fun from(attachment: Attachment): AttachmentImpl {
      return if (attachment is AttachmentImpl) return attachment
      else AttachmentImpl(
        id = attachment.id,
        path = attachment.path,
        name = attachment.name,
        type = attachment.type,
        size = attachment.size,
        createOn = attachment.createOn,
        creator = attachment.creator,
        modifyOn = attachment.modifyOn,
        modifier = attachment.modifier,
        puid = attachment.puid,
        upperId = attachment.upperId
      )
    }
  }
}