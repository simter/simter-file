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
import tech.simter.file.po.Attachment
import tech.simter.file.service.AttachmentService
import tech.simter.reactive.web.Utils.TEXT_PLAIN_UTF8

/**
 * The [HandlerFunction] for find multiple [Attachment] by module info.
 *
 * Request:
 *
 * ```
 * GET {context-path}/parent/:puid/:upperId
 * ```
 *
 * Response: (if found)
 *
 * ```
 * 200 OK
 * Content-Type: application/json;charset=UTF-8
 *
 * [{id, path, name, type, size, createOn, creator, fileName, puid, upperId}, ...]
 * ```
 *
 * Response: (if permission denied)
 *
 * ```
 * 403 Forbidden
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Find-Module-Attachments)
 *
 * @author JW
 * @author zh
 */
@Component
class FindModuleAttachmentsHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService.find(
      request.pathVariable("puid"),
      request.pathVariable("upperId")
    ).collectList()// response
      .flatMap { ok().contentType(APPLICATION_JSON_UTF8).syncBody(it) }
      // error mapping
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN_UTF8).syncBody(it.message!!)
      }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/parent/{puid}/{upperId}")
  }
}