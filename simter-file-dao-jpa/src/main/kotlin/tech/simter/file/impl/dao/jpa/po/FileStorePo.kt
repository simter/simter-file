package tech.simter.file.impl.dao.jpa.po

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.serialization.Serializable
import tech.simter.file.core.FileStore
import tech.simter.kotlin.serialization.serializer.javatime.iso.IsoOffsetDateTimeSerializer
import java.time.OffsetDateTime
import javax.persistence.*

/**
 * The physical file information.
 *
 * @author RJ
 */
@Entity
@Table(name = "st_file")
@Serializable
data class FileStorePo(
  @Id @Column(nullable = false, length = 36)
  private val id: String,
  @Column(nullable = false)
  override val module: String,
  @Column(nullable = false, length = 100)
  override val name: String,
  @Column(nullable = false, length = 10)
  override val type: String,
  @Column(nullable = false)
  override val size: Long,
  @Column(nullable = false)
  override val path: String,
  @Column(nullable = false, length = 50)
  override val creator: String,
  @Column(nullable = false)
  @Serializable(with = IsoOffsetDateTimeSerializer::class)
  override val createOn: OffsetDateTime,
  @Column(nullable = false, length = 50)
  override val modifier: String,
  @Column(nullable = false)
  @Serializable(with = IsoOffsetDateTimeSerializer::class)
  override val modifyOn: OffsetDateTime
) : FileStore {
  override fun getId() = id
  override fun isNew() = true

  @get:JsonIgnore
  @get:Transient
  override val fileName: String
    get() = super.fileName

  companion object {
    fun from(file: FileStore): FileStorePo {
      return if (file is FileStorePo) return file
      else FileStorePo(
        id = file.id,
        module = file.module,
        name = file.name,
        type = file.type,
        size = file.size,
        path = file.path,
        creator = file.creator,
        createOn = file.createOn,
        modifier = file.modifier,
        modifyOn = file.modifyOn
      )
    }
  }
}