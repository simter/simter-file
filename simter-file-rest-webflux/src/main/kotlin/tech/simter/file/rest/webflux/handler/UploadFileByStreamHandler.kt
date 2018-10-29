package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment
import tech.simter.file.service.AttachmentService
import java.io.File
import java.net.URI
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
 * POST {context-path}?puid=xxx&upperId=xxx
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
 * Location : {context-path}/{id}
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Upload-One-File-By-Stream)
 *
 * @author JW
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
      .map({
        // build Map by data in list
        val formDataMap = HashMap<String, Any>()
        formDataMap["filename"] = getFileName(request.headers().header("Content-Disposition"))
        formDataMap["puid"] = request.queryParam("puid")
          .orElseThrow { NullPointerException("Parameter \"puid\" mustn't be null!") }
        formDataMap["upperId"] = request.queryParam("upperId")
          .orElseThrow { NullPointerException("Parameter \"upperId\" mustn't be null!") }
        formDataMap["fileData"] = it
        formDataMap
      })
      // 2. save file to disk
      .flatMap({
        // get the FilePart by the Map
        val fileData = it["fileData"] as ByteArray
        // convert to Attachment instance
        val attachment = toAttachment(fileData.size.toLong(), it["filename"] as String, it["puid"] as String, it["upperId"] as String)

        val file = File("$fileRootDir/${attachment.path}")
        val fileDir = file.parentFile
        if (!fileDir.exists()) {
          if (!fileDir.mkdirs())  // create file directory if not exists
            throw IllegalAccessException("Failed to create parents dir: ${fileDir.absolutePath}")
        }
        if (!file.createNewFile()) throw IllegalAccessException("Failed to create file: ${file.absolutePath}")

        // save to disk
        Mono.just(FileCopyUtils.copy(fileData, file))
          .then(Mono.just(if (attachment.size != -1L) attachment else attachment.copy(size = file.length())))
      })
      // 3. save attachment
      .flatMap({ attachmentService.save(it).thenReturn(it) })
      // 4. return response
      .flatMap({ ServerResponse.noContent().location(URI.create("/${it.id}")).build() })
  }

  private fun toAttachment(fileSize: Long, filename: String, puid: String, upperId: String): Attachment {
    val id = newId()
    val now = OffsetDateTime.now()
    val lastDotIndex = filename.lastIndexOf(".")
    val type = filename.substring(lastDotIndex + 1)
    val path = "${now.format(DateTimeFormatter.ofPattern("yyyyMM"))}/${now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.$type"
    return Attachment(id = id, path = path, name = filename.substring(0, lastDotIndex), type = type, size = fileSize,
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", puid = puid, upperId = upperId)
  }

  /** Generate a new [Attachment] id */
  fun newId() = UUID.randomUUID().toString()

  /** Get file name from content disposition */
  private fun getFileName(contentDisposition: List<String>): String {
    val disposition = contentDisposition[0]
    val filenameIndex = disposition.indexOf("filename=\"").plus("filename=\"".length)
    return disposition.substring(filenameIndex, disposition.indexOf("\"", filenameIndex))
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = POST("/").and(contentType(MediaType.APPLICATION_OCTET_STREAM))
  }
}