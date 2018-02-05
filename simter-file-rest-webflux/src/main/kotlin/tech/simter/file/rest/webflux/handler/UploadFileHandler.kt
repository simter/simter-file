package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBufferUtils
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
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
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
        // convert
        val id = newId()
        val now = OffsetDateTime.now()
        val filename = it.filename()
        val lastDotIndex = filename.lastIndexOf(".")
        val ext = filename.substring(lastDotIndex + 1)
        val relativePath = "${now.format(DateTimeFormatter.ofPattern("yyyyMM"))}/${now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.$ext"

        val file = File("$fileRootDir/$relativePath")
        val fileDir = file.parentFile
        if (!fileDir.exists()) {
          if (!fileDir.mkdirs())  // create file directory if not exists
            throw IllegalAccessException("Failed to create parents dir: ${fileDir.absolutePath}")
        }
        //if (!file.createNewFile()) throw IllegalAccessException("Failed to create file: ${file.absolutePath}")
        val toPath = Files.createFile(file.toPath())
        val channel = AsynchronousFileChannel.open(toPath, StandardOpenOption.WRITE)

        DataBufferUtils
          // save to file
          .write(it.content(), channel, 0)
          // release data buffer
          .map(DataBufferUtils::release)
          // return Attachment instance
          .then(Mono.just(Attachment(
            id,                                     // id
            relativePath,                           // relative path
            filename.substring(0, lastDotIndex),    // name
            ext,                                    // ext
            file.length(),                          // file size
            now,                                    // upload time
            "Simter"                                // uploader
          )))
      })
      // 2. save attachment
      .flatMap({ attachmentService.create(it) })
      // 3. return response
      .flatMap({ ServerResponse.noContent().location(URI.create("/${it.id}")).build() })
  }

  /** Generate a new [Attachment] id */
  fun newId() = UUID.randomUUID().toString()

  /** Default router */
  fun router(): RouterFunction<ServerResponse> {
    return route(POST("/").and(contentType(MediaType.MULTIPART_FORM_DATA)), this)
  }
}