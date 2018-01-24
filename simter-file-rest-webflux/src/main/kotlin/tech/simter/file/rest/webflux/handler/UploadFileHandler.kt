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
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * The handler for upload file.
 *
 * @author JF
 */
@Component
class UploadFileHandler @Autowired constructor(
  @Value("\${app.file.root}") private val fileRootDir: String,
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest?): Mono<ServerResponse> {
    val requestBody = request?.bodyToFlux(Part::class.java) ?: throw IllegalArgumentException("the request body is null")
    val contentLength = request.headers().contentLength().asLong
    val id = UUID.randomUUID().toString()
    attachmentService.create(requestBody.map { part -> saveFileToLocal(part, contentLength, id) }.blockFirst()!!)
    return ServerResponse.noContent().location(URI.create(id)).build()
  }

  private fun saveFileToLocal(part: Part, contentLength: Long, id: String): Mono<Attachment> {
    val fileData = part as FilePart
    val fileNameWithExt = fileData.filename()
    val lastIndexOfPeriod = fileNameWithExt.lastIndexOf(".")
    val fileNameWithoutExt = fileNameWithExt.substring(0, lastIndexOfPeriod)
    val fileExt = fileNameWithExt.substring(lastIndexOfPeriod + 1, fileNameWithExt.length)
    val fileDir = "$fileRootDir/${YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"))}"
    val fileName = "${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.$fileExt"
    val filePath = File(fileDir, fileName)

    if (!File(fileDir).exists()) File(fileDir).mkdirs()
    if (filePath.createNewFile()) {
      fileData.transferTo(filePath)
    }
    return Mono.just(
      Attachment(id, fileName, fileNameWithoutExt, fileExt, contentLength, OffsetDateTime.now(), "Simter")
    )
  }

  /** Default router */
  fun router(): RouterFunction<ServerResponse> {
    return route(POST("/").and(contentType(MediaType.MULTIPART_FORM_DATA)), this)
  }
}