package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.core.domain.Attachment

/**
 * The [HandlerFunction] for find multiple [Attachment] info.
 *
 * Request:
 *
 * ```
 * GET {context-path}/attachment?page-no=:pageNo&page-size=:pageSize
 * ```
 *
 * Response: (if found)
 *
 * ```
 * 200 OK
 * Content-Type: application/json;charset=UTF-8
 *
 * {
 *   count, pageNo, pageSize,
 *   rows: [{id, path, name, type, size, createOn, creator, fileName, puid, upperId}, ...]
 * }
 * ```
 *
 * Response: (if permission denied)
 *
 * ```
 * 403 Forbidden
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Attachment-View)
 *
 * @author JF
 * @author zh
 * @author RJ
 */
@Component
class AttachmentViewHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService
      // find Page<Attachment> by queryParam page-no, page-size
      .find(PageRequest.of(
        request.queryParam("page-no").get().toInt(),
        request.queryParam("page-size").get().toInt()
      ))
      .map {
        // build response body
        val attachmentViewData = HashMap<String, Any>()
        attachmentViewData["count"] = it.totalElements
        attachmentViewData["pageNo"] = it.pageable.pageNumber
        attachmentViewData["pageSize"] = it.pageable.pageSize
        attachmentViewData["rows"] = it.content
        attachmentViewData
      }
      .flatMap {
        // return response
        ServerResponse.ok()
          .contentType(APPLICATION_JSON)
          .bodyValue(it)
      }
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/attachment")
  }
}