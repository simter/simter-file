package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.MediaType.MULTIPART_MIXED
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.FileService
import tech.simter.file.core.FileUpdateDescriber
import tech.simter.file.core.FileUploadSource
import tech.simter.reactive.web.Utils.responseForbiddenStatus
import tech.simter.reactive.web.Utils.responseGoneStatus
import java.util.*

/**
 * The [HandlerFunction] for update file.
 *
 * Request url pattern `PATCH /file/id?module=x&name=x&type=x&input-name=x`.
 *
 * See [rest-api.md#update-file](https://github.com/simter/simter-file/blob/master/docs/rest-api.md#2-update-file)
 *
 * @author nb
 */
@Component
class UpdateHandler @Autowired constructor(
  private val fileService: FileService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    val id = request.pathVariable("id")
    val module = request.queryParam("module")
    val name = request.queryParam("name")
    val type = request.queryParam("type")
    val fileUpdateDescriber = FileUpdateDescriber.Impl(module = module, name = name, type = type)

    // verify header
    val headers = request.headers()
    val contentType = headers.contentType()
    val updateResult = if (!contentType.isPresent) {
      fileService.update(id, fileUpdateDescriber, Optional.empty()).then(noContent().build())
    } else if (contentType.get().isCompatibleWith(MULTIPART_FORM_DATA)
      || contentType.get().isCompatibleWith(MULTIPART_MIXED)) {
      // traditional upload update by form submit
      request.bodyToFlux(Part::class.java).filter { it is FilePart }.collectList()
        .flatMap { files ->
          when {
            files.isEmpty() -> badRequest().bodyValue("missing file part in request body")
            files.size > 1 -> badRequest().bodyValue("update multiple files not supported yet")
            else -> {
              val file = files.first() as FilePart
              fileService.update(id, fileUpdateDescriber, source = Optional.of(FileUploadSource.FromFilePart(file)))
                .then(noContent().build())
            }
          }
        }
    } else {
      // upload update file by directly transmit file binary data through request body
      // contentType value as the real file media type
      val len = headers.contentLength()
      if (!len.isPresent) badRequest().bodyValue("missing header 'Content-Length'")
      else {
        fileService.update(id, fileUpdateDescriber,
          Optional.of(FileUploadSource.FromDataBufferPublisher(request.body(BodyExtractors.toDataBuffers())))
        ).then(noContent().build())
      }
    }

    return updateResult
      // resource not exists
      .onErrorResume(NotFoundException::class.java, ::responseGoneStatus)
      // permission denied
      .onErrorResume(PermissionDeniedException::class.java, ::responseForbiddenStatus)
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = RequestPredicates.PATCH("/{id}")
  }
}