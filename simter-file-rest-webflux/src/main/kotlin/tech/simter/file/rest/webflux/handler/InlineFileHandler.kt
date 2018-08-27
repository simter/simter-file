package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tech.simter.file.service.AttachmentService

/**
 * The [HandlerFunction] for inline file.
 *
 * Request:
 *
 * ```
 * GET {context-path}/inline/{id}
 * ```
 *
 * Response: (if found)
 *
 * ```
 * 200 OK
 * Content-Type        : application/octet-stream
 * Content-Length      : {len}
 * Content-Disposition : inline; filename="{file-name}.{ext}"
 *
 * :FILE-DATA
 * ```
 *
 * Response: (if not found)
 *
 * ```
 * 404 Not Found
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Watch-Attachment-Inline)
 *
 * @author JW
 */
@Component
class InlineFileHandler @Autowired constructor(
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
        ServerResponse.ok()
          .contentLength(it.size)
          .header("Content-Disposition", "inline; filename=\"${String(it.fileName.toByteArray(), Charsets.ISO_8859_1)}\"")
          .body(BodyInserters.fromResource(FileSystemResource("$fileRootDir/${it.path}")))
      })
      // not found
      .switchIfEmpty(ServerResponse.notFound().build())
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/inline/{id}")
  }
}