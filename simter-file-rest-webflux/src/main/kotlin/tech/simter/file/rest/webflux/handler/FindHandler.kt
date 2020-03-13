package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_PLAIN
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
import tech.simter.file.DEFAULT_MODULE_VALUE
import tech.simter.file.core.FileService
import tech.simter.file.core.ModuleMatcher.Companion.autoModuleMatcher

/**
 * The [HandlerFunction] for find file-view data.
 *
 * Request url pattern `GET /?pageable&offset=&limit=&module=$search=`.
 *
 * See [rest-api.md#upload-file](https://github.com/simter/simter-file/blob/master/docs/rest-api.md#5-find-file-view-data)
 *
 * @author RJ
 */
@Component
class FindHandler @Autowired constructor(
  private val fileService: FileService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    val pageable = request.queryParam("pageable")
    val module = request.queryParam("module").orElse(DEFAULT_MODULE_VALUE)
    val search = request.queryParam("search")
    val limit = request.queryParam("limit").map { it.toInt() }
    val offset = request.queryParam("offset").map { it.toInt() }

    val queryResult = if (pageable.isPresent) { // pageable query
      fileService.findPage(
          moduleMatcher = autoModuleMatcher(module),
          search = search,
          offset = offset,
          limit = limit
        )
        .flatMap { ok().contentType(APPLICATION_JSON).bodyValue(it) }
    } else {                                    // none-pageable query
      fileService.findList(
          moduleMatcher = autoModuleMatcher(module),
          search = search,
          limit = limit
        )
        .collectList()
        .flatMap { ok().contentType(APPLICATION_JSON).bodyValue(it) }
    }

    return queryResult.onErrorResume(PermissionDeniedException::class.java) {
      if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
      else status(FORBIDDEN).contentType(TEXT_PLAIN).bodyValue(it.message!!)
    }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/")
  }
}