package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
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
import reactor.core.publisher.toMono
import tech.simter.exception.NotFoundException
import tech.simter.file.po.Attachment
import tech.simter.file.service.AttachmentService
import tech.simter.reactive.web.Utils.TEXT_PLAIN_UTF8
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request.bodyToMono(ByteArray::class.java)
      .flatMap {
        val id = request.queryParam("id").orElse(newId())
        val puid = request.queryParam("puid").orElse(null)
        val upperId = request.queryParam("upper").orElse(null)
        val fileName = request.queryParam("filename")
          .orElseThrow { NullPointerException("Parameter \"filename\" mustn't be null!") }
        attachmentService.uploadFile(
          createAttachment(id = id, fileSize = it.size.toLong(), fileName = fileName, puid = puid, upperId = upperId)
        ) { file ->
          FileCopyUtils.copy(it, file).toMono().then()
        }.thenReturn(id)
      }
      .flatMap { status(CREATED).syncBody(it) }
      .onErrorResume(NotFoundException::class.java) {
        if (it.message.isNullOrEmpty()) status(NOT_FOUND).build()
        else status(NOT_FOUND).contentType(TEXT_PLAIN_UTF8).syncBody(it.message!!)
      }
  }

  /** Generate a new [Attachment] id */
  fun newId() = UUID.randomUUID().toString()

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = POST("/").and(contentType(APPLICATION_OCTET_STREAM))
  }
}

fun createAttachment(id: String, fileSize: Long, fileName: String, puid: String?, upperId: String?): Attachment {
  val now = OffsetDateTime.now()
  val lastDotIndex = fileName.lastIndexOf(".")
  val type = fileName.substring(lastDotIndex + 1)
  val path = if (upperId == null) {
    "${now.format(DateTimeFormatter.ofPattern("yyyyMM"))}/${now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.$type"
  } else {
    "${now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.$type"
  }
  return Attachment(id = id, path = path, name = fileName.substring(0, lastDotIndex), type = type, size = fileSize,
    createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", puid = puid, upperId = upperId)
}