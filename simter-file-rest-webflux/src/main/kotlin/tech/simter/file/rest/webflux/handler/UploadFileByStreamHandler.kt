package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.file.po.Attachment
import tech.simter.file.service.AttachmentService
import tech.simter.reactive.web.Utils.TEXT_PLAIN_UTF8
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap

/**
 * The [HandlerFunction] for upload file by stream.
 *
 * Request: (form submit with <input type="file">)
 *
 * ```
 * POST {context-path}/?puid=:puid&upper=:upper&filename=:filename
 * Content-Type        : application/octet-stream
 * Content-Length      : {len}
 *
 * {file-data}
 * ```
 *
 * Response:
 *
 * ```
 * 201 Created
 * {id}
 * ```
 *
 * If not Found the upper
 * ```
 * 404 Not Found
 *
 * Upper not exists
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Upload-One-File-By-Stream)
 *
 * @author JW
 * @author zh
 */
@Component
class UploadFileByStreamHandler @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request
      .bodyToMono(ByteArray::class.java)
      // 1. extract data in request body
      .map {
        // build Map by data in list
        val formDataMap = HashMap<String, Any>()
        formDataMap["id"] = request.queryParam("id").orElse(newId())
        formDataMap["fileName"] = request.queryParam("filename")
          .orElseThrow { NullPointerException("Parameter \"filename\" mustn't be null!") }
        formDataMap["puid"] = request.queryParam("puid").orElse("")
        formDataMap["upperId"] = request.queryParam("upper").orElse("EMPTY")
        formDataMap["fileData"] = it
        formDataMap
      }
      // 2. save file to disk
      .flatMap {
        // get the FilePart by the Map
        val fileData = it["fileData"] as ByteArray
        // convert to Attachment instance
        val attachment = toAttachment(it["id"] as String, fileData.size.toLong(),
          it["fileName"] as String, it["puid"] as String, it["upperId"] as String)
        // get path
        attachmentService.getFullPath(it["upperId"] as String)
          .flatMap { upperFullPath ->
            val file = File("$fileRootDir/$upperFullPath/${attachment.path}")
            val fileDir = file.parentFile
            if (!fileDir.exists()) {
              if (!fileDir.mkdirs())  // create file directory if not exists
                throw IllegalAccessException("Failed to create parents dir: ${fileDir.absolutePath}")
            }
            if (!file.createNewFile()) throw IllegalAccessException("Failed to create file: ${file.absolutePath}")
            // save to disk
            Mono.just(FileCopyUtils.copy(fileData, file))
              .then(Mono.just(if (attachment.size != -1L) attachment else attachment.copy(size = file.length())))
          }
      }
      // 3. save attachment
      .flatMap { attachmentService.save(it).thenReturn(it) }
      // 4. return response
      .flatMap { status(CREATED).syncBody(it.id) }
      .onErrorResume(NotFoundException::class.java) {
        status(NOT_FOUND).contentType(TEXT_PLAIN_UTF8).syncBody(it.message ?: "")
      }
  }

  private fun toAttachment(id: String, fileSize: Long, fileName: String, puid: String, upperId: String): Attachment {
    val now = OffsetDateTime.now()
    val lastDotIndex = fileName.lastIndexOf(".")
    val type = fileName.substring(lastDotIndex + 1)
    val path = if (upperId == "EMPTY") {
      "${now.format(DateTimeFormatter.ofPattern("yyyyMM"))}/${now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.$type"
    } else {
      "${now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.$type"
    }
    return Attachment(id = id, path = path, name = fileName.substring(0, lastDotIndex), type = type, size = fileSize,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", puid = puid, upperId = upperId)
  }

  /** Generate a new [Attachment] id */
  fun newId() = UUID.randomUUID().toString()

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = POST("/").and(contentType(APPLICATION_OCTET_STREAM))
  }
}