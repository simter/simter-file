package tech.simter.file.impl.domain

import tech.simter.file.core.domain.Attachment
import tech.simter.file.core.domain.AttachmentUpdateInfo
import tech.simter.kotlin.beans.AbstractDynamicBean

class AttachmentUpdateInfoImpl : AbstractDynamicBean(), AttachmentUpdateInfo {
  override var name: String? by holder
  override var type: String? by holder
  override var path: String? by holder
  override var size: Long? by holder
  override var puid: String? by holder
  override var upperId: String? by holder

  override var originalValue: Map<String, Any?>? = null

  companion object {
    fun from(attachment: Attachment): AttachmentUpdateInfo {
      return AttachmentUpdateInfoImpl().apply {
        name = attachment.name
        type = attachment.type
        path = attachment.path
        size = attachment.size
        puid = attachment.puid
        upperId = attachment.upperId
      }
    }
  }
}