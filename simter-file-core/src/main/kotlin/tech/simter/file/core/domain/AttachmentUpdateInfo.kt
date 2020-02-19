package tech.simter.file.core.domain

import tech.simter.kotlin.beans.DynamicBean

/**
 * The dto of the [Attachment] update fields.
 *
 * @author zh
 * @author RJ
 * */
interface AttachmentUpdateInfo : DynamicBean {
  val name: String?
  val path: String?
  val puid: String?
  val upperId: String?

  /** 原值列表 */
  val originalValue: Map<String, Any?>?
}