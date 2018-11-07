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
 * The [HandlerFunction] for download file.
 *
 * Request:
 *
 * ```
 * GET {context-path}/{id}
 * ```
 *
 * Response: (if found)
 *
 * ```
 * 200 OK
 * Content-Type        : application/octet-stream
 * Content-Length      : {len}
 * Content-Disposition : attachment; filename="{file-name}.{type}"
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
 * [More](https://github.com/simter/simter-file/wiki/Download-One-File)
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
    val id = request.pathVariable("id")
    return attachmentService.get(id)
      .zipWith(Mono.defer { attachmentService.getFullPath(id) })
      // the flatMap run on other thread by the scheduler
      .publishOn(Schedulers.elastic())
      // found
      .flatMap({
        // return response
        ok().contentType(APPLICATION_OCTET_STREAM)
          .contentLength(it.t1.size)
          .header("Content-Disposition",
            "attachment; filename=\"${String("${it.t1.name}.${it.t1.type}".toByteArray(), ISO_8859_1)}\"")
          .body(BodyInserters.fromResource(FileSystemResource("$fileRootDir/${it.t2}")))
      })
      // not found
      .switchIfEmpty(notFound().build())
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/{id}")
  }
}