package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import tech.simter.file.service.AttachmentService

/**
 * The handler for attachment form.
 *
 * @author JF
 */
@Component
class AttachmentFormHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return attachmentService
      // get Attachment by path variable id
      .get(request.pathVariable("id"))
      .flatMap({
        // return response
        ServerResponse.ok()
          .contentType(MediaType.APPLICATION_JSON_UTF8)
          .syncBody(it)
      })
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/attachment/{id}")
  }
}