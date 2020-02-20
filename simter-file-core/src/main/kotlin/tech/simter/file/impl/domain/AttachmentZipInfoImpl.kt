package tech.simter.file.impl.domain

import tech.simter.file.core.domain.AttachmentZipInfo

data class AttachmentZipInfoImpl(
  override val id: String,
  override val type: String,
  override val terminus: String,
  override val origin: String?,
  override val zipPath: String,
  override val physicalPath: String
) : AttachmentZipInfo