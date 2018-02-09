package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import tech.simter.file.service.AttachmentService

/**
 * The handler for file view.
 *
 * @author JF
 */
@Component
class FileViewHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService
      // find Page<Attachment> by queryParam page-no, page-size
      .find(PageRequest.of(
        request.queryParam("page-no").get().toInt(),
        request.queryParam("page-size").get().toInt()
      ))
      .map({
        // build response body
        val fileViewData = HashMap<String, Any>()
        fileViewData["count"] = it.totalElements
        fileViewData["pageNo"] = it.pageable.pageNumber
        fileViewData["pageSize"] = it.pageable.pageSize
        fileViewData["rows"] = it.content
        fileViewData
      })
      .flatMap({
        // return response
        ServerResponse.ok()
          .contentType(MediaType.APPLICATION_JSON_UTF8)
          .syncBody(it)
      })
  }

  /** Default router */
  fun router(): RouterFunction<ServerResponse> {
    return route(GET("/attachment"), this)
  }
}