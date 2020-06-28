package tech.simter.file.impl

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.file.BASE_DATA_DIR
import tech.simter.file.core.*
import tech.simter.kotlin.data.Page
import tech.simter.reactive.security.ReactiveSecurityService
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE
import java.time.OffsetDateTime
import java.util.*

/**
 * The Service implementation of [FileService].
 *
 * @author RJ
 */
@Service
class FileServiceImpl @Autowired constructor(
  @Value("\${$BASE_DATA_DIR}")
  private val baseDir: String,
  @Value("\${simter-file.verify-real-file-size: true}")
  private val verifyRealFileSize: Boolean,
  @Value("\${simter-file.default-find-page-limit: 25}")
  private val defaultFindPageLimit: Int,
  @Value("\${simter-file.default-find-list-limit: 50}")
  private val defaultFindListLimit: Int,
  val fileDao: FileDao,
  val fileIdGenerator: FileIdGenerator,
  val filePathGenerator: FilePathGenerator,
  val securityService: ReactiveSecurityService
) : FileService {
  private final val logger = LoggerFactory.getLogger(FileServiceImpl::class.java)
  private val basePath: Path = Paths.get(baseDir)

  override fun findPage(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Optional<Int>,
    offset: Optional<Int>
  ): Mono<Page<FileStore>> {
    return fileDao.findPage(
      moduleMatcher = moduleMatcher,
      search = search,
      limit = limit.orElse(defaultFindPageLimit),
      offset = offset.orElse(0)
    )
  }

  override fun findList(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Optional<Int>
  ): Flux<FileStore> {
    return fileDao.findList(
      moduleMatcher = moduleMatcher,
      search = search,
      limit = if (limit.isPresent) limit else Optional.of(defaultFindListLimit)
    )
  }

  /** Get the context user name */
  private fun getCurrentUser(): Mono<String> {
    return securityService.getAuthenticatedUser()
      .map { if (it.isPresent) it.get().name else "System" }
  }

  override fun upload(describer: FileDescriber, source: FileUploadSource): Mono<String> {
    // unique markup
    val ts = Optional.of(OffsetDateTime.now())
    val uuid = Optional.of(UUID.randomUUID())

    // 1. generate file path
    val filePath = filePathGenerator.resolve(describer = describer, ts = ts, uuid = uuid)
    val targetFile = basePath.resolve(filePath)
    val parentDir = targetFile.toFile().parentFile
    if (!parentDir.exists()) {
      parentDir.mkdirs()
      logger.info("create directory '{}'", parentDir)
    }

    // 2. store file data to physical disk
    val transferTo: Mono<Void> = when (source) {
      is FileUploadSource.FromFilePart -> source.value.transferTo(targetFile).doOnSuccess {
        // verify real file size
        if (verifyRealFileSize && targetFile.toFile().length() != describer.size)
          throw IllegalArgumentException("specific upload file size not match the real file size")
      }
      is FileUploadSource.FromResource -> {
        // zero-copy for disk file
        FileChannel.open(targetFile, CREATE_NEW, WRITE)
          .transferFrom(source.value.readableChannel(), 0, source.value.contentLength())
        Mono.empty()
      }
      is FileUploadSource.FromDataBufferPublisher -> {
        val channel = AsynchronousFileChannel.open(targetFile, CREATE_NEW, WRITE)
        DataBufferUtils.write(source.value, channel)
          .map {
            DataBufferUtils.release(it)
            it
          }
          .doOnTerminate { channel.close() }
          .then()
      }
    }.doOnSuccess { logger.info("transfer file data to target file '{}'", targetFile) }

    return transferTo.then(Mono.defer {
      getCurrentUser().flatMap { creator ->
        // 3. generate file id
        fileIdGenerator.nextId(ts = ts, uuid = uuid)
          .flatMap {
            // 4. store file information to database and return the id
            fileDao.create(FileStore.Impl(
              id = it,
              module = describer.module,
              name = describer.name,
              type = describer.type,
              size = describer.size,
              path = filePath.toString(),
              creator = creator,
              createOn = ts.get(),
              modifier = creator,
              modifyOn = ts.get()
            ))
          }
      }
    })
  }

  override fun update(
    id: String,
    describer: FileUpdateDescriber,
    source: Optional<FileUploadSource>
  ): Mono<Void> {
    TODO("not implemented")
  }

  override fun download(id: String): Mono<FileDownload> {
    return fileDao.get(id)
      .map { FileDownload.from(it, basePath) }
  }

  override fun delete(vararg ids: String): Mono<Int> {
    TODO("Not yet implemented")
  }

  override fun delete(moduleMatcher: ModuleMatcher): Mono<Int> {
    TODO("Not yet implemented")
  }
}