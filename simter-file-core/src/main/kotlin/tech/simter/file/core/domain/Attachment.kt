package tech.simter.file.core.domain

import java.time.OffsetDateTime

/**
 * The physical file information.
 *
 * @author RJ
 * @author JF
 */
interface Attachment :
  AttachmentIdentityProperties,
  AttachmentRequiredProperties,
  AttachmentLinkProperties,
  AttachmentCreationProperties,
  AttachmentModificationProperties,
  AttachmentRedundancyProperties {
  override val fileName: String
    get() = "$name${if (type == ":d") "" else ".$type"}"
}

interface AttachmentIdentityProperties {
  val id: String
}

interface AttachmentRequiredProperties {
  /** File name without extension */
  val name: String
  /**
   * If it is a file, the type is file extension without dot symbol.
   * and if it is a folder, the type is ":d".
   */
  val type: String
  /** The byte unit file size */
  val size: Long
  /** The relative path that store the actual physical file */
  val path: String
}

interface AttachmentLinkProperties : AttachmentModuleProperties, AttachmentUpperProperties
interface AttachmentModuleProperties {
  /** The unique id of the belong business module */
  val puid: String?
}

interface AttachmentUpperProperties {
  /** The upperId of the parent module */
  val upperId: String?
}

interface AttachmentCreationProperties {
  /** The created datetime */
  val createOn: OffsetDateTime
  /** The creator */
  val creator: String
}

interface AttachmentModificationProperties {
  /** The last modify datetime */
  val modifyOn: OffsetDateTime
  /** The last modifier */
  val modifier: String
}

interface AttachmentRedundancyProperties {
  /**
   * The file name with extension.
   *
   * If it's a directory, return "".
   */
  val fileName: String
}