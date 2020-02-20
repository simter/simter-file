package tech.simter.file.impl.dao.r2dbc.po

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import tech.simter.file.TABLE_ATTACHMENT
import tech.simter.file.core.domain.Attachment
import java.time.OffsetDateTime

@Table(TABLE_ATTACHMENT)
data class AttachmentPo(
  @Id @JvmField
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
) : Attachment, Persistable<String> {
  @get:Transient
  override val fileName = super.fileName

  override fun getId() = this.id
  override fun isNew() = true

  companion object {
    fun from(attachment: Attachment): AttachmentPo {
      return if (attachment is AttachmentPo) return attachment
      else AttachmentPo(
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