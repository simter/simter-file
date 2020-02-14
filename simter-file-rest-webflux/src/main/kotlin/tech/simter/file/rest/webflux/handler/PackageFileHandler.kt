package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.reactive.web.Utils.TEXT_PLAIN_UTF8
import java.io.ByteArrayOutputStream

/**
 * The [HandlerFunction] for package files.
 * Request:
 * ```
 * GET {context-path}/zip/{id}?name=:name
 * ```
 * > name is optional
 * > {id} can be {id1,id2,...}, but the user need to make sure that {id} is not truncated
 *
 * Response: (if found)
 *
 * ```
 * 200 OK
 * Content-Type        : application/octet-stream
 * Content-Length      : {len}
 * Content-Disposition : attachment; filename="{name}.zip"
 *
 * {FILE-DATA}
 * ```
 *
 * Response: (if permission denied or across module)
 * 
 * ```
 * 403 Forbidden
 * ```
 *
 * Response: (if not found)
 *
 * ```
 * 404 Not Found
 * ```
 *
 * @author zh
 */
@Component
class PackageFileHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    val ids = request.pathVariable("id").split(",")
    val byteOutputStream = ByteArrayOutputStream()
    return attachmentService.packageAttachments(byteOutputStream, *ids.toTypedArray())
      .map { defaultName ->
        Pair(byteOutputStream.toByteArray(), request.queryParam("name").map { "$it.zip" }.orElse(defaultName))
      }
      .flatMap {
        val data = it.first
        ok().contentType(APPLICATION_OCTET_STREAM)
          .contentLength(data.size.toLong())
          .header("Content-Disposition", "attachment; filename=\"${it.second}\"")
          .body(BodyInserters.fromResource(ByteArrayResource(data)))
      }
      .switchIfEmpty(notFound().build())
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN_UTF8).syncBody(it.message!!)
      }
      .onErrorResume(ForbiddenException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN_UTF8).syncBody(it.message!!)
      }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/zip/{id}")
  }
}