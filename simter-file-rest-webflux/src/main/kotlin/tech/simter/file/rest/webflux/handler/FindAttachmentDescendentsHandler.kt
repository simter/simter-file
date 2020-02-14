package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.reactive.web.Utils.TEXT_PLAIN_UTF8

/**
 * The [HandlerFunction] for find attachment'descendants of the forest structure.
 *
 * Request:
 *
 * ```
 * GET {context-path}/{id}/descendent
 * ```
 *
 * Response: (if found)
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
 * Response: (if permission denied)
 *
 * ```
 * 403 Forbidden
 * ```
 *
 * @author zh
 */
@Component
class FindAttachmentDescendentsHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService.findDescendents(request.pathVariable("id")).collectList()
      .flatMap { ok().contentType(APPLICATION_JSON_UTF8).syncBody(it) }
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN_UTF8).syncBody(it.message!!)
      }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/attachment/{id}/descendent")
  }
}