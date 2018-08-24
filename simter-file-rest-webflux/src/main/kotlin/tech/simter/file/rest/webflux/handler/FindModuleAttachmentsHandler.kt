package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.po.Attachment
import tech.simter.file.service.AttachmentService

/**
 * The [HandlerFunction] for find multiple [Attachment] by module info.
 *
 * Request:
 *
 * ```
 * GET {context-path}/parent/:puid/:subgroup
 * ```
 *
 * Response:
 *
 * ```
 * 200 OK
 * Content-Type: application/json;charset=UTF-8
 *
 * [{id, path, name, ext, size, uploadOn, uploader, fileName, puid, subgroup}, ...]
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Find-Module-Attachments)
 *
 * @author JW
 */
@Component
class FindModuleAttachmentsHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService.find(
      request.pathVariable("puid"),
      request.pathVariable("subgroup").toShort()
    ).collectList()// response
      .flatMap { ServerResponse.ok().contentType(MediaType.APPLICATION_JSON_UTF8).syncBody(it) }
      // error mapping
      .onErrorResume(PermissionDeniedException::class.java, {
        ServerResponse.status(HttpStatus.FORBIDDEN).contentType(MediaType.TEXT_PLAIN).syncBody(it.message ?: "")
      })
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/parent/{puid}/{subgroup}")
  }
}