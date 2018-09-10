package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.DELETE
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import tech.simter.file.service.AttachmentService

/**
 * The [HandlerFunction] for delete files.
 *
 * Request:
 *
 * ```
 * Delete {context-path}/{ids}
 * ```
 *
 * Response: (if found)
 *
 * ```
 * 204 No Content
 * ```
 *
 * Response: (if not found)
 *
 * ```
 * 404 Not Found
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Delete-Files)
 *
 * @author JW
 */
@Component
class DeleteFilesHandler @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return Mono.empty()
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = DELETE("/{ids}")
  }
}