package tech.simter.file.impl

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.file.BASE_DATA_DIR
import tech.simter.file.core.*
import tech.simter.kotlin.data.Page
import tech.simter.reactive.security.ReactiveSecurityService
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.FileChannel
import java.nio.file.Files
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
    offset: Optional<Long>
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
          throw IllegalArgumentException("specific upload file size not match the real file size " + targetFile.toFile().length() + "|" + describer.size)
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
    // unique markup
    val ts = Optional.of(OffsetDateTime.now())
    val uuid = Optional.of(UUID.randomUUID())

    return fileDao.get(id)
      .switchIfEmpty(Mono.error(NotFoundException("no file to update was found!")))
      .flatMap { fileStore ->
        var size = fileStore.size
        val fileDescriber = FileDescriber.Impl(
          module = describer.module.orElse(fileStore.module),
          name = describer.name.orElse(fileStore.name),
          type = describer.type.orElse(fileStore.type),
          size = size
        )
        val filePath = filePathGenerator.resolve(
          describer = fileDescriber,
          ts = if (source.isPresent) ts else Optional.of(fileStore.createOn),
          uuid = uuid
        )
        val targetFile = basePath.resolve(filePath)
        val parentDir = targetFile.toFile().parentFile
        if (!parentDir.exists()) {
          parentDir.mkdirs()
          logger.info("create directory '{}'", parentDir)
        }

        val mono = if (source.isPresent) {
          val fileSource = source.get()
          when (fileSource) {
            is FileUploadSource.FromFilePart -> fileSource.value.transferTo(targetFile)
            is FileUploadSource.FromResource -> {
              // zero-copy for disk file
              FileChannel.open(targetFile, CREATE_NEW, WRITE)
                .transferFrom(fileSource.value.readableChannel(), 0, fileSource.value.contentLength())
              Mono.empty()
            }
            is FileUploadSource.FromDataBufferPublisher -> {
              val channel = AsynchronousFileChannel.open(targetFile, CREATE_NEW, WRITE)
              DataBufferUtils.write(fileSource.value, channel)
                .map {
                  DataBufferUtils.release(it)
                  it
                }
                .doOnTerminate { channel.close() }
                .then()
            }
          }.doOnSuccess {
            logger.info("transfer file data to target file '{}'", targetFile)
            size = targetFile.toFile().length()

            val oldFile = Paths.get(baseDir, fileStore.path).toFile()
            if (oldFile.delete()) logger.info("delete old file '{}'", oldFile.absolutePath)
          }
        } else {
          Files.move(Paths.get(baseDir, fileStore.path), targetFile)
          Mono.empty<Void>()
        }

        mono.then(Mono.defer { getCurrentUser() }).flatMap { modifier ->
          fileDao.update(id, FileUpdateDescriber.Impl(
            module = describer.module,
            name = describer.name,
            type = describer.type,
            size = OptionalLong.of(size),
            path = Optional.of(filePath.toString()),
            modifier = Optional.of(modifier),
            modifyOn = ts
          ))
        }.then()
      }
  }

  override fun download(id: String): Mono<FileDownload> {
    return fileDao.get(id)
      .map { FileDownload.from(it, basePath) }
  }

  override fun delete(vararg ids: String): Mono<Int> {
    return fileDao.findById(*ids).collectList().flatMap { list ->
      fileDao.delete(*ids).flatMap {
        for (fileStore in list) {
          val file = Paths.get(baseDir, fileStore.path).toFile()
          if (file.delete()) logger.info("delete file '{}'", file.absolutePath)
        }

        Mono.just(it)
      }
    }
  }

  override fun delete(moduleMatcher: ModuleMatcher): Mono<Int> {
    return fileDao.findList(moduleMatcher = moduleMatcher).collectList().flatMap { list ->
      fileDao.delete(moduleMatcher).flatMap {
        for (fileStore in list) {
          val file = Paths.get(baseDir, fileStore.path).toFile()
          if (file.delete()) logger.info("delete file '{}'", file.absolutePath)
        }

        Mono.just(it)
      }
    }
  }
}