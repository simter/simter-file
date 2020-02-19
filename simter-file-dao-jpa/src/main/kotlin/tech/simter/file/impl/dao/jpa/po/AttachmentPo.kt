package tech.simter.file.impl.dao.jpa.po

import com.fasterxml.jackson.annotation.JsonIgnore
import tech.simter.file.core.domain.Attachment
import java.time.OffsetDateTime
import javax.persistence.*

/**
 * The physical file information.
 *
 * @author RJ
 */
@Entity
@Table(
  name = "st_attachment",
  uniqueConstraints = [UniqueConstraint(columnNames = ["path", "upperId"])]
)
data class AttachmentPo(
  @Id @Column(nullable = false, length = 36)
  override val id: String,
  @Column(nullable = false)
  override val path: String,
  @Column(nullable = false)
  override val name: String,
  @Column(nullable = false, length = 10)
  override val type: String,
  @Column(nullable = false)
  override val size: Long,
  @Column(nullable = false)
  override val createOn: OffsetDateTime,
  @Column(nullable = false)
  override val creator: String,
  @Column(nullable = false)
  override val modifyOn: OffsetDateTime,
  @Column(nullable = false)
  override val modifier: String,
  @Column(nullable = true, length = 36)
  override val puid: String? = null,
  @Column(nullable = true, length = 36)
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