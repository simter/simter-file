package tech.simter.file.impl.domain

import tech.simter.file.core.domain.AttachmentUpdateInfo
import tech.simter.kotlin.beans.AbstractDynamicBean

class AttachmentUpdateInfoImpl : AbstractDynamicBean(), AttachmentUpdateInfo {
  override var name: String? by holder
  override var path: String? by holder
  override var puid: String? by holder
  override var upperId: String? by holder
  override var originalValue: Map<String, Any?>? = null
}