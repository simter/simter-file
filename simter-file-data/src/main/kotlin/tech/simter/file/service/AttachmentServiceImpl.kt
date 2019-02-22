package tech.simter.file.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.exception.UnauthenticatedException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDto
import tech.simter.file.dto.AttachmentDto4Update
import tech.simter.file.dto.AttachmentDto4Zip
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.po.Attachment
import tech.simter.file.service.AttachmentServiceImpl.OperationType.*
import tech.simter.kotlin.properties.AuthorizeModuleOperations
import tech.simter.kotlin.properties.AuthorizeRole
import tech.simter.reactive.context.SystemContext.User
import tech.simter.reactive.security.ReactiveSecurityService
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.OffsetDateTime
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The attachment service implementation.
 *
 * @author cjw
 * @author RJ
 * @author zh
 */
@Component
class AttachmentServiceImpl @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  val attachmentDao: AttachmentDao,
  authorizeModuleOperationProperties: AuthorizeModuleOperations,
  val securityService: ReactiveSecurityService
) : AttachmentService {
  private val authorizeModuleOperations = authorizeModuleOperationProperties.operations!!
    .map { module ->
      module.key to module.value.flatMap { authorizeOperation ->
        authorizeOperation.key.split(",").map { it to authorizeOperation.value }
      }.toMap()
    }.toMap()

  enum class OperationType {
    Read, Create, Update, Delete
  }

  private val operationDescribe =
    hashMapOf(Read to "read", Create to "create", Update to "update", Delete to "delete")

  private val hasAdmin = authorizeModuleOperations.containsKey("admin")

  /**
   * 1. If [module] is not "admin" and no admin configuration,
   *   verify module or default(no module configuration) [AuthorizeRole]
   * 2. If [module] is not "admin" and has admin configuration,
   *   verify module or default(no module configuration) [AuthorizeRole],
   *   verify admin [AuthorizeRole] if authentication failed.
   * 3. If [module] is "admin" and no admin configuration, authentication failed.
   * 4. If [module] is "admin" and has admin configuration, verify admin [AuthorizeRole].
   * 5. According to [strategy][AuthorizeRole.strategy],
   *   verify whether the system-context has any [roles][AuthorizeRole.roles] or has all roles [roles][AuthorizeRole.roles].
   *
   * @return[Mono.empty]
   *   return [Mono.error] with [UnauthenticatedException] if without a authenticated system-context.
   *   return a [Mono.error] with [PermissionDeniedException] if has a authenticated system-context but it has'ont specified roles.
   */
  fun verifyAuthorize(module: String?, operation: OperationType): Mono<Void> {
    val verifier: (Map<String, AuthorizeRole>?) -> Mono<Void> = {
      it?.get(operationDescribe[operation]!!)?.let {
        if (it.strategy == AuthorizeRole.Strategy.And)
          securityService.verifyHasAllRole(*it.roles.toTypedArray())
        else
          securityService.verifyHasAnyRole(*it.roles.toTypedArray())
      } ?: Mono.empty()
    }
    return when {
      module != "admin" && hasAdmin -> {
        verifier(authorizeModuleOperations[module] ?: authorizeModuleOperations["default"])
          .onErrorResume(PermissionDeniedException::class.java) { verifier(authorizeModuleOperations["admin"]) }
      }
      module != "admin" -> {
        verifier(authorizeModuleOperations[module] ?: authorizeModuleOperations["default"])
      }
      hasAdmin -> {
        verifier(authorizeModuleOperations["admin"])
      }
      else -> {
        Mono.error(PermissionDeniedException())
      }
    }
  }

  override fun packageAttachments(outputStream: OutputStream, vararg ids: String): Mono<String> {
    return attachmentDao.findDescendentsZipPath(*ids).collectList()
      .flatMap { reactivePackage(outputStream, it).then(Mono.just(it)) }
      .flatMap { dtos ->
        if (dtos.isNotEmpty()) {
          val att = dtos[0]
          val name = att.origin?.let {
            if (dtos.size == 1 && att.type != ":d") {
              "${att.zipPath!!.split("/")[0]}.${att.type}"
            } else {
              att.zipPath!!.split("/")[0]
            }
          } ?: "root"
          Mono.just("$name.zip")
        } else {
          Mono.empty()
        }
      }
  }

  /**
   * responsive package [dtos] to [outputStream], and return [Mono.empty]
   * @param[bufferLength] The length of each read
   */
  private fun reactivePackage(outputStream: OutputStream, dtos: List<AttachmentDto4Zip>, bufferLength: Int = 1024): Mono<Void> {
    // init zip file
    val zos = ZipOutputStream(outputStream)
    val byteBuffer = ByteBuffer.allocate(bufferLength)
    return dtos.toFlux().concatMap { dto ->
      // write a folder
      if (dto.type == ":d") {
        Mono.defer {
          zos.putNextEntry(ZipEntry("${dto.zipPath!!}/"))
          zos.closeEntry()
          Mono.just(Unit)
        }
      }
      // write a file
      else {
        // init reactiveReadAFileToZip a file and init zip entry
        Mono.defer {
          val channel = AsynchronousFileChannel.open(Paths.get("$fileRootDir/${dto.physicalPath!!}"))
          zos.putNextEntry(ZipEntry("${dto.zipPath!!}.${dto.type!!}"))
          Mono.just(channel)
        }
          // recursive reactiveReadAFileToZip the file many times
          .flatMap {
            reactiveReadFile(channel = it, byteBuffer = byteBuffer, index = 0,
              onRead = { byteBuffer, result ->
                zos.write(byteBuffer.array(), 0, result)
                byteBuffer.clear()
              },
              onComplete = {
                zos.closeEntry()
              })
          }
          // finish reactiveReadAFileToZip the file and finish zip entry
          .then(Mono.defer {
            zos.closeEntry()
            Mono.just(Unit)
          })
      }
    }
      // finish the zip file
      .then(Mono.defer {
        zos.flush()
        zos.close()
        Mono.empty<Void>()
      })
  }

  /**
   * responsive and recursive read a file
   * @param[channel] the file's [AsynchronousFileChannel]
   * @param[byteBuffer] used to buffer
   * @param[index] is file start position from the global
   * @param[onRead] each read data of the callback function
   *   format is (ByteBuffer, Int) -> Unit
   *   [ByteBuffer] is data buffer
   *   [Int] is this read length
   * @param[onComplete] read complete of the callback function
   * @param[onError] read error of the callback function
   *   format is (Throwable) -> Unit
   *   [Throwable] is the error
   * @return[Mono.empty] returns a complete signal from the global
   */
  private fun reactiveReadFile(channel: AsynchronousFileChannel,
                               byteBuffer: ByteBuffer,
                               index: Long,
                               onRead: (ByteBuffer, Int) -> Unit = { _, _ -> },
                               onComplete: () -> Unit = {},
                               onError: (Throwable) -> Unit = {})
    : Mono<Long> {
    return Mono.create<Long> {
      channel.read<Void>(byteBuffer, index, null, object : CompletionHandler<Int, Void> {
        override fun completed(result: Int, attachment: Void?) {
          if (result != -1) {
            onRead(byteBuffer, result)
            it.success(index + result)
          } else {
            onComplete()
            it.success()
          }
        }

        override fun failed(exc: Throwable, attachment: Void?) {
          onError(exc)
          it.error(exc)
        }
      })
    }.flatMap { reactiveReadFile(channel, byteBuffer, it, onRead, onComplete, onError) }
  }

  override fun create(vararg attachments: Attachment): Flux<String> {
    // 1. verify authorize
    return attachments.map { it.puid }.toSet().toMono().flatMap {
      if (it.size > 1)
        Mono.error(ForbiddenException("Created attachments has different puid"))
      else
        verifyAuthorize(it.firstOrNull(), Create)
    }
      // 2. set creator and modifier
      .then(Mono.defer { securityService.getAuthenticatedUser() })
      .map(Optional<User>::get).map(User::name)
      .map { userName -> attachments.map { it.copy(creator = userName, modifier = userName) }.toTypedArray() }
      // 3. save attachments and return ids
      .map { attachmentDao.save(*it) }
      .thenMany(attachments.map { it.id }.toFlux())
  }

  override fun findDescendents(id: String): Flux<AttachmentDtoWithChildren> {
    return attachmentDao.findPuids(id).next()
      .flatMap { verifyAuthorize(it.orElse(null), Read) }
      .thenMany(Flux.defer { attachmentDao.findDescendents(id) })
  }

  override fun update(id: String, dto: AttachmentDto4Update): Mono<Void> {
    // 1. verify authorize
    return attachmentDao.findPuids(id).next()
      .switchIfEmpty(Mono.error(NotFoundException("The attachment $id not exists")))
      .flatMap {
        val oldPuid = it.orElse(null)
        if (oldPuid != dto.puid)
          Mono.error(ForbiddenException("Can't Modify the puid"))
        else
          verifyAuthorize(oldPuid, Update)
      }
      // 2. set the modifyOn and modifier
      .then(Mono.defer { securityService.getAuthenticatedUser() })
      .map(Optional<User>::get).map(User::name)
      .map { userName -> dto.data.plus(mapOf("modifier" to userName, "modifyOn" to OffsetDateTime.now())) }
      // 3. update attachment and physical file
      .flatMap { data ->
        if (dto.path == null && dto.upperId == null) {
          attachmentDao.update(id, data)
        } else {
          // Changed the file path, need to get the full path before and after the change and move the it
          attachmentDao.getFullPath(id)
            .delayUntil { attachmentDao.update(id, data) }
            .zipWhen { attachmentDao.getFullPath(id) }
            .doOnNext {
              Files.move(Paths.get("$fileRootDir/${it.t1}"), Paths.get("$fileRootDir/${it.t2}"),
                StandardCopyOption.REPLACE_EXISTING)
            }
            .then()
        }
      }
  }

  override fun getFullPath(id: String): Mono<String> {
    return attachmentDao.getFullPath(id)
      .zipWith(attachmentDao.findPuids(id).next())
      .switchIfEmpty(Mono.error(NotFoundException("The attachment $id not exists")))
      .delayUntil { verifyAuthorize(it.t2.orElse(null), Read) }
      .map { it.t1 }
  }

  override fun get(id: String): Mono<Attachment> {
    return attachmentDao.get(id).delayUntil { verifyAuthorize(it.puid, Read) }
  }

  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    return attachmentDao.find(pageable)
  }

  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    return verifyAuthorize(puid, Read)
      .thenMany(Flux.defer { attachmentDao.find(puid, upperId) })
  }

  override fun delete(vararg ids: String): Mono<Void> {
    // 1. verify authorize
    return attachmentDao.findPuids(*ids).collectList()
      .flatMap {
        if (it.size > 1)
          Mono.error(ForbiddenException("Deleted attachments has different puid"))
        else
          verifyAuthorize(it.firstOrNull()?.orElse(null), Delete)
      }
      // 2. delete attachment
      .thenMany(Flux.defer { attachmentDao.delete(*ids) })
      // 3. delete physical file
      .doOnNext { File("$fileRootDir/$it").deleteRecursively() }
      .then()
  }

  override fun uploadFile(attachment: Attachment, writer: (File) -> Mono<Void>): Mono<Void> {
    val upperId = attachment.upperId
    // 1. verify authorize
    return verifyAuthorize(attachment.puid, Create)
      // 2. get upper full path
      .then(Mono.defer {
        upperId?.let {
          attachmentDao.getFullPath(upperId)
            .switchIfEmpty(Mono.error(NotFoundException("The attachment $upperId not exists")))
        } ?: Mono.just("")
      })
      // 3. save physical file
      .flatMap { upperFullPath ->
        val file = File("$fileRootDir/$upperFullPath/${attachment.path}")
        val fileDir = file.parentFile
        if (!fileDir.exists()) {
          if (!fileDir.mkdirs())  // create file directory if not exists
            throw IllegalAccessException("Failed to create parents dir: ${fileDir.absolutePath}")
        }
        if (!file.createNewFile()) throw IllegalAccessException("Failed to create file: ${file.absolutePath}")
        writer(file).then(Mono.defer {
          if (attachment.size != -1L) attachment.toMono()
          else attachment.copy(size = file.length()).toMono()
        })
      }
      // 4. set the creator and modifier
      .flatMap {
        securityService.getAuthenticatedUser().map(Optional<User>::get).map(User::name)
          .map { userName -> it.copy(creator = userName, modifier = userName) }
      }
      // 5. save attachment data
      .flatMap { attachmentDao.save(it) }
  }

  override fun reuploadFile(dto: AttachmentDto, fileData: ByteArray): Mono<Void> {
    val id = dto.id!!
    // 1. verify authorize
    return attachmentDao.findPuids(id).next()
      .switchIfEmpty(Mono.error(NotFoundException("The attachment $id not exists")))
      .flatMap { verifyAuthorize(it.orElse(null), Update) }
      // 2. get upper full path
      .then(Mono.defer { attachmentDao.getFullPath(id) })
      // 3. save physical file
      .doOnNext { fullPath ->
        val file = File("$fileRootDir/$fullPath")
        val fileDir = file.parentFile
        if (!fileDir.exists()) {
          if (!fileDir.mkdirs())  // create file directory if not exists
            throw IllegalAccessException("Failed to create parents dir: ${fileDir.absolutePath}")
        }
        if (!file.exists()) {
          if (!file.createNewFile())
            throw IllegalAccessException("Failed to create file: ${file.absolutePath}")
        }
        FileCopyUtils.copy(fileData, file)
      }
      // 4. set the modifyOn and modifier
      .then(Mono.defer { securityService.getAuthenticatedUser() })
      .map(Optional<User>::get).map(User::name)
      .map { userName -> dto.data.plus(mapOf("modifier" to userName, "modifyOn" to OffsetDateTime.now())) }
      // 5. save attachment data
      .flatMap { attachmentDao.update(id, it.filter { it.key != "id" }) }
  }
}