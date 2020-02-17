package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.PATCH
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.impl.domain.AttachmentUpdateInfoImpl

/**
 * The [HandlerFunction] for update attachment.
 *
 * Request:
 *
 * ```
 * PATCH {context-path}/attachment/{id}
 * Content-Type : application/json;charset=UTF-8
 *
 * {DATA}
 * ```
 *
 * Response: (if updated)
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
 * Response: (if not found)
 *
 * ```
 * 404 Not Found
 *
 * Attachment  not exists
 * ```
 *
 * @author zh
 * @author RJ
 */
@Component
class UpdateAttachmentHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request.bodyToMono<AttachmentUpdateInfoImpl>()
      .flatMap { attachmentService.update(request.pathVariable("id"), it) }
      .then(noContent().build())
      .onErrorResume(NotFoundException::class.java) {
        if (it.message.isNullOrEmpty()) status(NOT_FOUND).build()
        else status(NOT_FOUND).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
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
    val REQUEST_PREDICATE: RequestPredicate = PATCH("/attachment/{id}").and(contentType(APPLICATION_JSON))
  }
}