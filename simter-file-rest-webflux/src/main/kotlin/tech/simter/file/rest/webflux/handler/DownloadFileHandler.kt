package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tech.simter.file.service.AttachmentService
import kotlin.text.Charsets.ISO_8859_1


/**
 * The handler for download file.
 *
 * @author JF
 * @author RJ
 */
@Component
class DownloadFileHandler @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService
      // get Attachment by path variable id
      .get(request.pathVariable("id"))
      // the flatMap run on other thread by the scheduler
      .publishOn(Schedulers.elastic())
      // found
      .flatMap({
        // return response
        ok().contentType(APPLICATION_OCTET_STREAM)
          .contentLength(it.size)
          .header("Content-Disposition", "attachment; filename=\"${String(it.fileName.toByteArray(), ISO_8859_1)}\"")
          .body(BodyInserters.fromResource(FileSystemResource("$fileRootDir/${it.path}")))
      })
      // not found
      .switchIfEmpty(notFound().build())
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/{id}")
  }
}