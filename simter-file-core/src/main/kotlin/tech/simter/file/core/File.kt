package tech.simter.file.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.reactivestreams.Publisher
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.codec.multipart.FilePart
import tech.simter.kotlin.serialization.serializer.javatime.iso.IsoOffsetDateTimeSerializer
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.*

/**
 * The information to describe the file.
 *
 * @author RJ
 */
interface FileDescriber {
  val module: String
  val name: String
  val type: String
  val size: Long

  val fileName: String
    get() = "$name.$type"

  data class Impl(
    override val module: String,
    override val name: String,
    override val type: String,
    override val size: Long
  ) : FileDescriber
}

/** The file pack information */
interface FilePack : FileDescriber {
  val path: String
  val createOn: OffsetDateTime
  val modifyOn: OffsetDateTime

  @Serializable
  @SerialName("FilePack")
  data class Impl(
    override val module: String,
    override val name: String,
    override val type: String,
    override val size: Long,
    override val path: String,
    @Serializable(with = IsoOffsetDateTimeSerializer::class)
    override val createOn: OffsetDateTime,
    @Serializable(with = IsoOffsetDateTimeSerializer::class)
    override val modifyOn: OffsetDateTime
  ) : FilePack
}

/** The file store information */
interface FileStore : FileDescriber, FilePack {
  val id: String
  val creator: String
  val modifier: String

  @Serializable
  @SerialName("FileStore")
  data class Impl(
    override val id: String,
    override val module: String,
    override val name: String,
    override val type: String,
    override val size: Long,
    override val path: String,
    override val creator: String,
    @Serializable(with = IsoOffsetDateTimeSerializer::class)
    override val createOn: OffsetDateTime,
    override val modifier: String,
    @Serializable(with = IsoOffsetDateTimeSerializer::class)
    override val modifyOn: OffsetDateTime
  ) : FileStore
}

/** The file upload source for load the real file data */
sealed class FileUploadSource(open val value: Any) {
  data class FromFilePart(override val value: FilePart) : FileUploadSource(value)
  data class FromResource(override val value: Resource) : FileUploadSource(value)
  data class FromDataBufferPublisher(override val value: Publisher<DataBuffer>) : FileUploadSource(value)
}

/** The file download information */
interface FileDownload {
  val describer: FileDescriber
  val source: Source

  /** Source to load the real file data */
  sealed class Source(open val value: Any) {
    data class FromPath(override val value: Path) : Source(value)
    data class FromDataBufferPublisher(override val value: Publisher<DataBuffer>) : Source(value)
  }

  companion object {
    fun from(fileStore: FileStore, basePath: Path): FileDownload {
      return Impl(
        describer = fileStore,
        source = Source.FromPath(basePath.resolve(fileStore.path))
      )
    }
  }

  data class Impl(
    override val describer: FileDescriber,
    override val source: Source
  ) : FileDownload
}

/** The file update info describe */
interface FileUpdateDescriber {
  val module: Optional<String>
  val name: Optional<String>
  val type: Optional<String>
  val size: OptionalLong

  data class Impl(
    override val module: Optional<String> = Optional.empty(),
    override val name: Optional<String> = Optional.empty(),
    override val type: Optional<String> = Optional.empty(),
    override val size: OptionalLong = OptionalLong.empty()
  ) : FileUpdateDescriber
}

/** The file update info */
interface FileUpdate: FileUpdateDescriber {
  val path: Optional<String>
  val modifier: Optional<String>
  val modifyOn: Optional<OffsetDateTime>

  data class Impl(
    override val module: Optional<String> = Optional.empty(),
    override val name: Optional<String> = Optional.empty(),
    override val type: Optional<String> = Optional.empty(),
    override val size: OptionalLong = OptionalLong.empty(),
    override val path: Optional<String> = Optional.empty(),
    override val modifier: Optional<String> = Optional.empty(),
    override val modifyOn: Optional<OffsetDateTime> = Optional.empty()
  ) : FileUpdate
}