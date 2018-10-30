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
  /** If it is a file, the type is file extension without dot symbol.
   *  and if it is a folder, the type is ":d".
   */
  @Column(nullable = false, length = 10) val type: String,
  /** The byte unit file length */
  @Column(nullable = false) val size: Long,
  /** Created time */
  @Column(nullable = false) val createOn: OffsetDateTime,
  /** The account do the created */
  @Column(nullable = false) val creator: String,
  /** Last modify time */
  @Column(nullable = false) val modifyOn: OffsetDateTime,
  /** The account do the last modify */
  @Column(nullable = false) val modifier: String,
  /** The unique id of the parent module */
  @Column(nullable = true, length = 36) val puid: String = "",
  /** The upperId of the parent module */
  @Column(nullable = true, length = 36) val upperId: String = "EMPTY") {

  /** File name with extension */
  @Ignore
  @javax.persistence.Transient
  @org.springframework.data.annotation.Transient
  val fileName = "$name${if (type == ":d") "" else ".$type"}"
}