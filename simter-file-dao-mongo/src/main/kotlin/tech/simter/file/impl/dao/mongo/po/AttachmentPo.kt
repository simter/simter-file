package tech.simter.file.impl.dao.mongo.po

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.mapping.Document
import tech.simter.file.core.domain.Attachment
import java.time.OffsetDateTime

/**
 * The physical file information.
 *
 * @author RJ
 */
@Document(collection = "st_attachment")
data class AttachmentPo(
  @Id
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
  @get:JsonIgnore
  @get:Transient
  override val fileName: String
    get() = super.fileName

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