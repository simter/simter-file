package tech.simter.file.impl.dao.jpa.dto

import tech.simter.file.core.domain.AttachmentZipInfo
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class AttachmentZipInfoImpl(
  @Id
  override val id: String,
  override val type: String,
  override val terminus: String,
  override val origin: String?,
  override val zipPath: String,
  override val physicalPath: String
) : AttachmentZipInfo