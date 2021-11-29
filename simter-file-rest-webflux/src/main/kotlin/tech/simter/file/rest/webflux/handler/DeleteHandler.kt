package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.FileService
import tech.simter.file.core.ModuleMatcher.Companion.autoModuleMatcher
import tech.simter.reactive.web.Utils.responseForbiddenStatus

/**
 * The [HandlerFunction] for delete file.
 *
 * Request url pattern `DELETE /file$id?module`.
 *
 * See [rest-api.md#delete-file](https://github.com/simter/simter-file/blob/master/docs/rest-api.md#3-delete-file)
 *
 * @author nb
 */
@Component
class DeleteHandler @Autowired constructor(
  private val fileService: FileService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    val mono = if (request.queryParam("module").isPresent) {
      fileService.delete(autoModuleMatcher(request.pathVariable("id")))
    } else {
      fileService.delete(*request.pathVariable("id").split(",").toTypedArray())
    }

    return mono.flatMap { ok().bodyValue(it) }
      // permission denied
      .onErrorResume(PermissionDeniedException::class.java, ::responseForbiddenStatus)
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = RequestPredicates.DELETE("/{id}")
  }
}