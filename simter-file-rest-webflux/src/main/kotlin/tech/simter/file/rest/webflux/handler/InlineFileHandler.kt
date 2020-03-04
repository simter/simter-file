package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.FILE_ROOT_DIR_KEY
import tech.simter.file.core.AttachmentService

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
 * Content-Disposition : inline; filename="{file-name}.{type}"
 *
 * :FILE-DATA
 * ```
 *
 * Response: (if permission denied)
 *
 * ```
 * 403 Forbidden
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
 * @author zh
 * @author RJ
 */
@Component
class InlineFileHandler @Autowired constructor(
  @Value("\${$FILE_ROOT_DIR_KEY}") private val fileRootDir: String,
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    val id = request.pathVariable("id")
    return attachmentService.get(id)
      .zipWith(Mono.defer { attachmentService.getFullPath(id) })
      // the flatMap run on other thread by the scheduler
      .publishOn(Schedulers.elastic())
      // found
      .flatMap {
        // return response
        ok()
          .contentLength(it.t1.size)
          .header("Content-Disposition",
            "inline; filename=\"${String("${it.t1.name}.${it.t1.type}".toByteArray(), Charsets.ISO_8859_1)}\"")
          .body(BodyInserters.fromResource(FileSystemResource("$fileRootDir/${it.t2}")))
      }
      // not found
      .switchIfEmpty(notFound().build())
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/inline/{id}")
  }
}