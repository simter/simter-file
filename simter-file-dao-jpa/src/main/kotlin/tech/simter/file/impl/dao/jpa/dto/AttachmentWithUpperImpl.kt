package tech.simter.file.impl.dao.jpa.dto

import tech.simter.file.core.domain.AttachmentWithUpper
import java.time.OffsetDateTime
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class AttachmentWithUpperImpl(
  @Id
  override val id: String,
  override val path: String,
  override val name: String,
  override val type: String,
  override val size: Long,
  override val modifyOn: OffsetDateTime,
  override val modifier: String,
  override val upperId: String?
) : AttachmentWithUpper