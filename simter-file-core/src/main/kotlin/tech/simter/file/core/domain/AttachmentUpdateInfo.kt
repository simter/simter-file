package tech.simter.file.core.domain

import tech.simter.kotlin.beans.DynamicBean

/**
 * The attachment update info.
 *
 * @author zh
 * @author RJ
 * */
interface AttachmentUpdateInfo : DynamicBean {
  val name: String?
  val type: String?
  val path: String?
  val size: Long?
  val puid: String?
  val upperId: String?

  /** 原值列表 */
  val originalValue: Map<String, Any?>?
}