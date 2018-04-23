package tech.simter.file.po

import jdk.nashorn.internal.ir.annotations.Ignore
import org.springframework.data.mongodb.core.mapping.Document
import java.time.OffsetDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The meta information of the upload file.
 * @author RJ
 * @author JF
 */
@Entity
@Table(name = "st_attachment")
@Document(collection = "st_attachment")
data class Attachment(
  /** UUID */
  @javax.persistence.Id
  @org.springframework.data.annotation.Id
  @Column(nullable = false, length = 36)
  val id: String,
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
  @Column(nullable = false) val uploader: String,
  /** The unique id of the parent module */
  @Column(nullable = false, length = 36) val puid: String = "0",
  /** The subgroup of the parent module */
  @Column(nullable = false) val subgroup: Short = 0) {

  /** File name with extension */
  @Ignore
  @javax.persistence.Transient
  @org.springframework.data.annotation.Transient
  val fileName = "$name.$ext"
}