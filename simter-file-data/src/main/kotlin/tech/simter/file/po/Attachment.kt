package tech.simter.file.po

import java.time.OffsetDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Transient

/**
 * The meta information of the upload file.
 * @author RJ
 */
@Entity
data class Attachment(
  /** UUID */
  @Column(length = 36) @Id val id: String? = null,
  /** The relative path that store the actual physical file */
  val path: String,
  /** File name without extension */
  val name: String,
  /** File extension without dot symbol */
  @Column(length = 10) val ext: String,
  /** The byte unit file length */
  val size: Long,
  /** Upload time */
  val uploadOn: OffsetDateTime,
  /** The account do the upload */
  val uploader: String) {

  /** File name with extension */
  @Transient val fileName = name + "." + ext
}