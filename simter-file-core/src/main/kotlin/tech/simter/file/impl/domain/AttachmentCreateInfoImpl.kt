package tech.simter.file.impl.domain

import tech.simter.file.core.domain.AttachmentCreateInfo

data class AttachmentCreateInfoImpl(
  override val id: String,
  override val name: String,
  override val type: String,
  override val size: Long,
  override val puid: String?,
  override val upperId: String?,
  override val path: String
) : AttachmentCreateInfo {
  companion object {
    fun from(info: AttachmentCreateInfo): AttachmentCreateInfoImpl {
      return if (info is AttachmentCreateInfoImpl) return info
      else AttachmentCreateInfoImpl(
        id = info.id,
        name = info.name,
        type = info.type,
        size = info.size,
        puid = info.puid,
        path = info.path,
        upperId = info.upperId
      )
    }
  }
}