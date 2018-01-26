package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tech.simter.file.service.AttachmentService
import java.io.File
import kotlin.text.Charsets.ISO_8859_1


/**
 * The handler for download file.
 *
 * @author JF
 */
@Component
class DownloadFileHandler @Autowired constructor(
  @Value("\${app.file.root}") private val fileRootDir: String,
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService
      // get Attachment by path variable id
      .get(request.pathVariable("id"))
      // the flatMap run on other thread by the scheduler
      .publishOn(Schedulers.elastic())
      .flatMap({
        // return response
        ServerResponse.ok()
          .contentType(APPLICATION_OCTET_STREAM)
          .contentLength(it.size)
          .header("Content-Disposition", "attachment; filename=\"${String(it.fileName.toByteArray(), ISO_8859_1)}\"")
          .body(fileToByteArray("$fileRootDir/${it.path}"), ByteArray::class.java)
      })
  }

  fun fileToByteArray(filePath: String): Mono<ByteArray> {
    return Mono.just(FileCopyUtils.copyToByteArray(File(filePath)))
  }

  /** Default router */
  fun router(): RouterFunction<ServerResponse> {
    return route(GET("/{id}"), this)
  }
}