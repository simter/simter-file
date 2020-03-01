package tech.simter.file.core.domain

interface AttachmentZipInfo {
  /**
   * the origin is nullable, so cannot set terminus and origin to joint the primary key
   * if origin is null, id = "null-$terminus"
   * if origin is not null, id = "\"$origin\"-\"$terminus\""
   */
  val id: String
  val type: String
  /** descendant's id  */
  val terminus: String
  /** least-common-ancestor's id  */
  val origin: String?
  val zipPath: String
  val physicalPath: String
}