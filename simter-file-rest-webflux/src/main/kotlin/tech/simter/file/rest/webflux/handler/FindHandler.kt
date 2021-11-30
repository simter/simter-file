package tech.simter.file.rest.webflux.handler

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.DEFAULT_MODULE_VALUE
import tech.simter.file.core.FileService
import tech.simter.file.core.ModuleMatcher.Companion.autoModuleMatcher
import tech.simter.kotlin.data.Page
import tech.simter.kotlin.data.Page.Companion.MappedType.OffsetLimit
import tech.simter.reactive.web.Utils.responseForbiddenStatus

/**
 * The [HandlerFunction] for find file-view data.
 *
 * Request url pattern `GET /file?pageable&offset=&limit=&module=$search=`.
 *
 * See [rest-api.md#upload-file](https://github.com/simter/simter-file/blob/master/docs/rest-api.md#5-find-file-view-data)
 *
 * @author RJ
 */
@Component
class FindHandler @Autowired constructor(
  private val json: Json,
  private val fileService: FileService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    val pageable = request.queryParam("pageable")
    val module = request.queryParam("module").orElse(DEFAULT_MODULE_VALUE)
    val search = request.queryParam("search")
    val limit = request.queryParam("limit").map { it.toInt() }
    val offset = request.queryParam("offset").map { it.toLong() }

    val queryResult = if (pageable.isPresent) { // pageable query
      fileService.findPage(
        moduleMatcher = autoModuleMatcher(module),
        search = search,
        offset = offset,
        limit = limit
      )
        // TODO delete json.encodeToString when spring-boot support auto config
        .flatMap {
          ok().contentType(APPLICATION_JSON).bodyValue(json.encodeToString(Page.toMap(it, json, OffsetLimit)))
        }
    } else {                                    // none-pageable query
      fileService.findList(
        moduleMatcher = autoModuleMatcher(module),
        search = search,
        limit = limit
      )
        .collectList()
        .flatMap { ok().contentType(APPLICATION_JSON).bodyValue(json.encodeToString(it)) }
    }

    return queryResult
      // permission denied
      .onErrorResume(PermissionDeniedException::class.java, ::responseForbiddenStatus)
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("").or(GET("/"))
  }
}