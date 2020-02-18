package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.PATCH
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.core.domain.AttachmentDto
import java.net.URLDecoder

/**
 * The [HandlerFunction] for reupload file by stream.
 *
 * Request: (form submit with <input type="file">)
 *
 * ```
 * PATCH {context-path}/{id}
 * Content-Type        : application/octet-stream
 * Content-Length      : {len}
 * Content-Disposition : {fileName}
 *
 * {file-data}
 * ```
 *
 * Response: (if found)
 *
 * ```
 * 204 No Content
 * ```
 *
 * Response: (if permission denied)
 *
 * ```
 * 403 Forbidden
 * ```
 *
 * Response: (if not found)
 * ```
 * 404 Not Found
 *
 * Attachment not exists
 * ```
 *
 * @author zh
 */
@Component
class ReuploadFileByStreamHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request
      .bodyToMono(ByteArray::class.java)
      // 1. extract data in request body
      .flatMap { fileData ->
        attachmentService.reuploadFile(AttachmentDto().apply {
          id = request.pathVariable("id")
          size = fileData.size.toLong()
          getFileName(request.headers().header("Content-Disposition"))?.let {
            val lastDotIndex = it.lastIndexOf(".")
            type = it.substring(lastDotIndex + 1)
            name = it.substring(0, lastDotIndex)
          }
        }, fileData)
      }
      // 2. return response
      .then(noContent().build())
      .onErrorResume(NotFoundException::class.java) {
        if (it.message.isNullOrEmpty()) status(NOT_FOUND).build()
        else status(NOT_FOUND).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
  }

  /** Get file name from content disposition */
  private fun getFileName(contentDisposition: List<String>): String? {
    if (contentDisposition.isEmpty()) return null
    val disposition = contentDisposition[0]
    val filenameIndex = disposition.indexOf("filename=\"").plus("filename=\"".length)
    val fileName = disposition.substring(filenameIndex, disposition.indexOf("\"", filenameIndex))
    return URLDecoder.decode(fileName, "utf-8")
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = PATCH("/{id}").and(contentType(APPLICATION_OCTET_STREAM))
  }
}