package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.PATCH
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.dto.AttachmentDto4Update
import tech.simter.file.service.AttachmentService
import tech.simter.reactive.web.Utils.TEXT_PLAIN_UTF8

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
 * Response:
 *
 * ```
 * 204 No Content
 * ```
 *
 * If not Found the attachment
 * 
 * ```
 * 404 Not Found
 * Attachment  not exists
 * ```
 *
 * If the specified path already exists
 *
 * ```
 * 403 Forbidden
 *
 * The specified path already exists
 * ```
 *
 * @author zh
 */
@Component
class UpdateAttachmentHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request.bodyToMono<AttachmentDto4Update>()
      .flatMap { attachmentService.update(request.pathVariable("id"), it) }
      .then(noContent().build())
      .onErrorResume(NotFoundException::class.java) {
        status(NOT_FOUND).contentType(TEXT_PLAIN_UTF8).syncBody(it.message ?: "")
      }
      .onErrorResume(PermissionDeniedException::class.java) {
        status(FORBIDDEN).contentType(TEXT_PLAIN_UTF8).syncBody(it.message ?: "")
      }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = PATCH("/attachment/{id}").and(contentType(APPLICATION_JSON_UTF8))
  }
}