package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.DELETE
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import tech.simter.file.service.AttachmentService
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.web.reactive.function.server.ServerResponse.status
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.reactive.web.Utils.TEXT_PLAIN_UTF8

/**
 * The [HandlerFunction] for delete files.
 *
 * Request:
 *
 * ```
 * Delete {context-path}/{ids}
 * ```
 *
 * Response: (if deleted)
 *
 * ```
 * 204 No Content
 * ```
 *
 * Response: (if permission denied or across module)
 *
 * ```
 * 403 Forbidden
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Delete-Files)
 *
 * @author JW
 * @author zh
 */
@Component
class DeleteFilesHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService.delete(*request.pathVariable("ids").split(",").toTypedArray())
      .then(ServerResponse.noContent().build())
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN_UTF8).syncBody(it.message!!)
      }
      .onErrorResume(ForbiddenException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN_UTF8).syncBody(it.message!!)
      }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = DELETE("/{ids}")
  }
}