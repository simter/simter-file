package tech.simter.file.impl

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.file.*
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
  private val fileAuthorizer: FileAuthorizer,
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
  private val logger = LoggerFactory.getLogger(FileServiceImpl::class.java)
  private val basePath: Path = Paths.get(baseDir)

  override fun findPage(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Optional<Int>,
    offset: Optional<Long>
  ): Mono<Page<FileStore>> {
    return fileAuthorizer.verifyHasPermission(moduleMatcher.module, OPERATION_READ)
      .then(Mono.defer {
        fileDao.findPage(
          moduleMatcher = moduleMatcher,
          search = search,
          limit = limit.orElse(defaultFindPageLimit),
          offset = offset.orElse(0)
        )
      })
  }

  override fun findList(
    moduleMatcher: ModuleMatcher,
    search: Optional<String>,
    limit: Optional<Int>
  ): Flux<FileStore> {
    return fileAuthorizer.verifyHasPermission(moduleMatcher.module, OPERATION_READ)
      .thenMany(Flux.defer {
        fileDao.findList(
          moduleMatcher = moduleMatcher,
          search = search,
          limit = if (limit.isPresent) limit else Optional.of(defaultFindListLimit)
        )
      })
  }

  /** Get the context username */
  internal fun getCurrentUser(): Mono<String> {
    return securityService.getAuthenticatedUser()
      .map { if (it.isPresent) it.get().name else "System" }
  }

  /** Get the current timestamp */
  internal fun getCurrentTimestamp(): OffsetDateTime {
    return OffsetDateTime.now()
  }

  /** create a random uuid */
  internal fun generateUUID(): UUID {
    return UUID.randomUUID()
  }

  override fun upload(describer: FileDescriber, source: FileUploadSource): Mono<String> {
    // generate unique markup
    val ts = Optional.of(getCurrentTimestamp())
    val uuid = Optional.of(generateUUID())

    return fileAuthorizer
      // 1. verify permission
      .verifyHasPermission(describer.module, OPERATION_CREATE)
      .then(Mono.defer {
        // 2. save physical file
        // 2.1. generate file path
        val filePath = filePathGenerator.resolve(describer = describer, ts = ts, uuid = uuid)
        val targetFile = basePath.resolve(filePath)
        val parentDir = targetFile.toFile().parentFile
        if (!parentDir.exists()) {
          parentDir.mkdirs()
          logger.info("create directory '{}'", parentDir)
        }

        // 2.2. store file data to physical disk
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
        }

        // return the save-to-path
        transferTo.doOnSuccess { logger.info("transfer file data to target file '{}'", targetFile) }
          .thenReturn(filePath)
      })
      .flatMap { filePath ->
        // get current user
        getCurrentUser().flatMap { creator ->
          // 3. generate file id
          fileIdGenerator.nextId(ts = ts, uuid = uuid)
            .flatMap { id ->
              // 4. save to database and return the id
              fileDao.create(FileStore.Impl(
                id = id,
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
      }
  }

  override fun update(
    id: String,
    describer: FileUpdateDescriber,
    source: Optional<FileUploadSource>
  ): Mono<Void> {
    // unique markup
    val ts = Optional.of(getCurrentTimestamp())
    val uuid = Optional.of(generateUUID())

    return fileDao.get(id)
      .switchIfEmpty(Mono.error(NotFoundException("no file to update was found!")))
      .delayUntil { fileAuthorizer.verifyHasPermission(it.module, OPERATION_UPDATE) } // verify permission
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
    return fileDao
      // get FileStore
      .get(id)
      // verify permission
      .delayUntil { fileAuthorizer.verifyHasPermission(it.module, OPERATION_READ) }
      // convert to FileDownload
      .map { FileDownload.from(it, basePath) }
  }

  override fun delete(vararg ids: String): Mono<Int> {
    return fileDao.findById(*ids)
      .collectList()
      // convert empty list to empty mono
      .flatMap { if (it.isEmpty()) Mono.empty<List<FileStore>>() else Mono.just(it) }
      // verify permission
      .delayUntil { list ->
        if (list.isEmpty()) Mono.empty<List<FileStore>>()
        else Mono.`when`(list.map { fileAuthorizer.verifyHasPermission(it.module, OPERATION_DELETE) })
      }
      // delete db records
      .flatMap { list -> fileDao.delete(*ids).map { Pair(list, it) } }
      // delete physical files
      .doOnNext {
        for (fileStore in it.first) {
          val file = Paths.get(baseDir, fileStore.path).toFile()
          if (file.delete()) logger.info("delete file '{}'", file.absolutePath)
        }
      }
      .map { it.second }
      .switchIfEmpty(Mono.just(0))
  }

  override fun delete(moduleMatcher: ModuleMatcher): Mono<Int> {
    return fileAuthorizer
      // verify permission
      .verifyHasPermission(moduleMatcher.module, OPERATION_DELETE)
      .then(Mono.defer {
        // find them
        fileDao.findList(moduleMatcher = moduleMatcher)
          .collectList()
          // convert empty list to empty mono
          .flatMap { if (it.isEmpty()) Mono.empty<List<FileStore>>() else Mono.just(it) }
          .flatMap { list ->
            // delete db records
            fileDao.delete(moduleMatcher)
              .delayUntil {
                // delete physical files
                for (fileStore in list) {
                  val file = Paths.get(baseDir, fileStore.path).toFile()
                  if (file.delete()) logger.info("delete file '{}'", file.absolutePath)
                }
                Mono.empty<Int>()
              }
          }
          .switchIfEmpty(Mono.just(0))
      })
  }
}