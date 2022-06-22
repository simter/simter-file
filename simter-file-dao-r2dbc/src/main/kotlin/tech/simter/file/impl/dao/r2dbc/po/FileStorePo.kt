package tech.simter.file.impl.dao.r2dbc.po

import kotlinx.serialization.Serializable
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import tech.simter.file.TABLE_FILE
import tech.simter.file.core.FileStore
import tech.simter.kotlin.serialization.serializer.javatime.iso.IsoOffsetDateTimeSerializer
import java.time.OffsetDateTime

@Table(TABLE_FILE)
@Serializable
data class FileStorePo(
  @Id
  private val id: String,
  override val module: String,
  override val name: String,
  override val type: String,
  override val size: Long,
  override val path: String,
  @Serializable(with = IsoOffsetDateTimeSerializer::class)
  override val createOn: OffsetDateTime,
  @Serializable(with = IsoOffsetDateTimeSerializer::class)
  override val modifyOn: OffsetDateTime,
  override val creator: String,
  override val modifier: String
) : FileStore {
  override fun getId() = id

  override fun isNew() = true

  companion object {
    fun from(fileStore: FileStore): FileStorePo {
      return if (fileStore is FileStorePo) fileStore
      else FileStorePo(
        id = fileStore.id,
        module = fileStore.module,
        name = fileStore.name,
        type = fileStore.type,
        size = fileStore.size,
        path = fileStore.path,
        creator = fileStore.creator,
        createOn = fileStore.createOn,
        modifier = fileStore.modifier,
        modifyOn = fileStore.modifyOn
      )
    }
  }
}