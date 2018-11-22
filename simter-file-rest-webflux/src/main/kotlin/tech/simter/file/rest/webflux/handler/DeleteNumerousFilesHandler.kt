package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.DELETE
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import reactor.core.publisher.Mono
import tech.simter.file.service.AttachmentService

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
 * Response:
 *
 * ```
 * 204 No Content
 * ```
 *
 * @author zh
 */
@Component
class DeleteNumerousFilesHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request.bodyToMono<Array<String>>()
      .flatMap { attachmentService.delete(*it) }
      .then(noContent().build())
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = DELETE("/").and(contentType(APPLICATION_JSON_UTF8))
  }
}