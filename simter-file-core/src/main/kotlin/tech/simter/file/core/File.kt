package tech.simter.file.core

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
}

/** The file store information */
interface FileStore : FileDescriber, FilePack {
  val id: String
  val creator: String
  val modifier: String

  @Serializable
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

/** The file update info */
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

/** The page data holder */
interface Page<T> {
  /** The query start point */
  val offset: Int

  /** The max rows count for each page query */
  val limit: Int

  /** The total rows count match the query */
  val total: Long

  /** The current page data */
  val rows: List<T>

  @Serializable
  private data class Impl<T>(
    override val offset: Int,
    override val limit: Int,
    override val total: Long,
    override val rows: List<T>
  ) : Page<T>

  companion object {
    private object EmptyPage : Page<Nothing> {
      override val offset: Int = 0
      override val limit: Int = 0
      override val total: Long = 0
      override val rows: List<Nothing> = emptyList()
    }

    /**
     * Build a page instance.
     *
     * The returned page is kotlin [Serializable].
     */
    fun <T> of(limit: Int, offset: Int = 0, total: Long = 0, rows: List<T> = emptyList()): Page<T> {
      return Impl(
        offset = offset,
        limit = limit,
        total = total,
        rows = rows
      )
    }

    /**
     * Returns an empty page.
     *
     * The returned page is kotlin [Serializable].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> emptyPage(): Page<T> = EmptyPage as Page<T>
  }
}