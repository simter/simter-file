package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import tech.simter.file.service.AttachmentService

/**
 * The [HandlerFunction] for find attachment'descendants of the forest structure.
 *
 * Request:
 *
 * ```
 * GET {context-path}/{id}/descendent
 * ```
 *
 * Response:
 *
 * ```
 * 200 OK
 * Content-Type : application/json;charset=UTF-8
 *
 * [{CHILD_DATA}, ...]
 * ```
 *
 * {CHILD_DATA}={id, name, type, size, modifyOn, modifier, children: [{CHILD_DATA}, ...]}
 *
 * @author zh
 */
@Component
class FindAttechmentDescendentsHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService.findDescendents(request.pathVariable("id")).collectList()
      .flatMap { ok().contentType(APPLICATION_JSON_UTF8).syncBody(it) }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/attachment/{id}/descendent")
  }
}