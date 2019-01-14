package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment
import tech.simter.file.service.AttachmentService

/**
 * The [HandlerFunction] for find single [Attachment] info.
 *
 * Request:
 *
 * ```
 * GET {context-path}/attachment/{id}
 * ```
 *
 * Response: (if found)
 *
 * ```
 * 200 OK
 * Content-Type: application/json;charset=UTF-8
 *
 * {id, path, name, type, size, createOn, creator, fileName, puid, upperId}
 * ```
 *
 * Response: (if not found)
 *
 * ```
 * 404 Not Found
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Attachment-Form)
 *
 * @author JF
 * @author RJ
 */
@Component
class AttachmentFormHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService.get(request.pathVariable("id"))
      .flatMap { ok().contentType(MediaType.APPLICATION_JSON_UTF8).syncBody(it) } // found
      .switchIfEmpty(notFound().build())                                          // not found
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/attachment/{id}")
  }
}