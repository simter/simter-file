package tech.simter.file.po

import jdk.nashorn.internal.ir.annotations.Ignore
import org.springframework.data.mongodb.core.mapping.Document
import java.time.OffsetDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

/**
 * The meta information of the upload file.
 * @author RJ
 */
@Entity
@Document
data class Attachment(
  /** UUID */
  @Column(nullable = false, length = 36) @Id @org.springframework.data.annotation.Id val id: String,
  /** The relative path that store the actual physical file */
  @Column(nullable = false) val path: String,
  /** File name without extension */
  @Column(nullable = false) val name: String,
  /** File extension without dot symbol */
  @Column(nullable = false, length = 10) val ext: String,
  /** The byte unit file length */
  @Column(nullable = false) val size: Long,
  /** Upload time */
  @Column(nullable = false) val uploadOn: OffsetDateTime,
  /** The account do the upload */
  @Column(nullable = false) val uploader: String) {

  /** File name with extension */
  @Ignore
  @org.springframework.data.annotation.Transient
  val fileName = name + "." + ext
}