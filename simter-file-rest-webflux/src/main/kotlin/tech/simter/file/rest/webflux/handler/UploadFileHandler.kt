package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
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


/**
 * The handler for upload file.
 *
 * @author JF
 * @author RJ
 */
@Component
class UploadFileHandler @Autowired constructor(
  @Value("\${app.file.root}") private val fileRootDir: String,
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request
      .bodyToFlux(Part::class.java)
      .filter({ it is FilePart })
      .map({ it as FilePart })
      .next() // only support upload one file by this time
      // 1. save file to disk
      .map({ it ->
        // convert to Attachment instance
        val attachment = toAttachment(it.headers().contentLength, it.filename())

        // save file to disk
        val file = File("$fileRootDir/${attachment.path}")
        val fileDir = file.parentFile
        if (!fileDir.exists()) {
          if (!fileDir.mkdirs())  // create file directory if not exists
            throw IllegalAccessException("Failed to create parents dir: ${fileDir.absolutePath}")
        }
        if (!file.createNewFile()) throw IllegalAccessException("Failed to create file: ${file.absolutePath}")

        // save to disk
        it.transferTo(file).then(Mono.just(attachment))
      })
      // 2. save attachment
      .flatMap({ attachmentService.create(it) })
      // 3. return response
      .flatMap({ ServerResponse.noContent().location(URI.create("/${it.id}")).build() })
  }

  private fun toAttachment(fileSize: Long, filename: String): Attachment {
    val id = newId()
    val now = OffsetDateTime.now()
    val lastDotIndex = filename.lastIndexOf(".")
    val ext = filename.substring(lastDotIndex + 1)
    val path = "${now.format(DateTimeFormatter.ofPattern("yyyyMM"))}/${now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.$ext"
    return Attachment(
      id,                                     // id
      path,                                   // relative path
      filename.substring(0, lastDotIndex),    // name
      ext,                                    // ext
      fileSize,                               // file size
      now,                                    // upload time
      "Simter"                                // uploader
    )
  }

  /** Generate a new [Attachment] id */
  fun newId() = UUID.randomUUID().toString()

  /** Default router */
  fun router(): RouterFunction<ServerResponse> {
    return route(POST("/").and(contentType(MediaType.MULTIPART_FORM_DATA)), this)
  }
}