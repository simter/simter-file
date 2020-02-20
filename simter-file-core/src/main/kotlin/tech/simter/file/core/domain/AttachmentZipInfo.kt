package tech.simter.file.core.domain

import javax.persistence.Id

interface AttachmentZipInfo {
  /**
   * the origin is nullable, so cannot set terminus and origin to joint the primary key
   * if origin is null, id = "null-$terminus"
   * if origin is not null, id = "\"$origin\"-\"$terminus\""
   */
  @get:Id
  val id: String
  val type: String
  /** descendant's id  */
  val terminus: String
  /** least-common-ancestor's id  */
  val origin: String?
  val zipPath: String
  val physicalPath: String
}