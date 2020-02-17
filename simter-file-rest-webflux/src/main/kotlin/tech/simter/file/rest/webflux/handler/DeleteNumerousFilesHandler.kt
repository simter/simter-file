package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.DELETE
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService

/**
 * The [HandlerFunction] for delete files.
 *
 * Request:
 *
 * ```
 * Delete {context-path}/
 * Content-Type : application/json;charset=UTF-8
 *
 * {ids}  [id1, id2, id3, ...]
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
 * @author zh
 * @author RJ
 */
@Component
class DeleteNumerousFilesHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request.bodyToMono<Array<String>>()
      .flatMap { attachmentService.delete(*it) }
      .then(noContent().build())
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
      .onErrorResume(ForbiddenException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = DELETE("/").and(contentType(APPLICATION_JSON))
  }
}