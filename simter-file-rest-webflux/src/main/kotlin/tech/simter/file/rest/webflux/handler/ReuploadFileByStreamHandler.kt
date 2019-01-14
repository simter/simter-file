package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
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
import tech.simter.file.po.Attachment
import tech.simter.file.service.AttachmentService
import tech.simter.reactive.web.Utils.TEXT_PLAIN_UTF8
import java.io.File
import java.net.URLDecoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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
 * Response:
 *
 * ```
 * 204 No Content
 * ```
 *
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
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request
      .bodyToMono(ByteArray::class.java)
      .flatMap { fileData ->
        // 1. extract data in request body
        val fileName = getFileName(request.headers().header("Content-Disposition"))
        val id = request.pathVariable("id")
        // 2. save file to disk
        attachmentService.get(id)
          .zipWith(Mono.defer { attachmentService.getFullPath(id) })
          .flatMap {
            var attachment = it.t1
            val oldFullPath = it.t2
            val parentFullPath = oldFullPath.substring(0, oldFullPath.lastIndexOf(attachment.path))
            // delete old file
            val oldFile = File("$fileRootDir/$oldFullPath")
            if (oldFile.exists() && oldFile.isFile) oldFile.delete()
            // modify attachment
            attachment = modifyAttachment(attachment, fileData.size.toLong(), fileName)
            // create new file
            val newFile = File("$fileRootDir/$parentFullPath/${attachment.path}")
            val fileDir = newFile.parentFile
            if (!fileDir.exists()) {
              if (!fileDir.mkdirs())  // create file directory if not exists
                throw IllegalAccessException("Failed to create parents dir: ${fileDir.absolutePath}")
            }
            if (!newFile.createNewFile()) throw IllegalAccessException("Failed to create file: ${newFile.absolutePath}")
            // save to disk
            Mono.just(FileCopyUtils.copy(fileData, newFile))
              .then(Mono.just(if (attachment.size != -1L) attachment else attachment.copy(size = newFile.length())))
          }
      }
      // 3. save attachment
      .flatMap { attachmentService.save(it).thenReturn(it) }
      // 4. return response
      .flatMap { noContent().build() }
      .onErrorResume(NotFoundException::class.java) {
        if (it.message.isNullOrEmpty()) status(NOT_FOUND).build()
        else status(NOT_FOUND).contentType(TEXT_PLAIN_UTF8).syncBody(it.message!!)
      }
  }

  private fun modifyAttachment(attachment: Attachment, fileSize: Long, filename: String?): Attachment {
    val now = OffsetDateTime.now()
    return if (filename != null && attachment.fileName != filename) {
      val lastDotIndex = filename.lastIndexOf(".")
      val type = filename.substring(lastDotIndex + 1)
      val path = if (attachment.upperId == "EMPTY") {
        "${now.format(DateTimeFormatter.ofPattern("yyyyMM"))}/${now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-${attachment.id}.$type"
      } else {
        "${now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-${attachment.id}.$type"
      }
      attachment.copy(modifyOn = now, modifier = "Simter", size = fileSize,
        name = filename.substring(0, lastDotIndex), type = type, path = path)
    } else {
      attachment.copy(modifyOn = now, modifier = "Simter", size = fileSize)
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