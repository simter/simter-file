package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.MediaType.MULTIPART_MIXED
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.Part
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.badRequest
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.FileDescriber
import tech.simter.file.core.FileService
import tech.simter.file.core.FileUploadSource
import tech.simter.reactive.web.Utils

/**
 * The [HandlerFunction] for upload file.
 *
 * Request url pattern `POST /file?module=x&name=x&type=x&input-name=x`.
 *
 * See [rest-api.md#upload-file](https://github.com/simter/simter-file/blob/master/docs/rest-api.md#1-upload-file)
 *
 * @author RJ
 */
@Component
class UploadHandler @Autowired constructor(
  private val fileService: FileService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    // verify header
    val headers = request.headers()
    val contentType = headers.contentType()
    if (!contentType.isPresent) return badRequest().bodyValue("missing header 'Content-Type'")

    val module = request.queryParam("module").orElse("/default/")
    val name = request.queryParam("name").orElse("unknown")
    val type = request.queryParam("type").orElse("xyz")
    val size = request.queryParam("size").orElse("0").toLong()

    // upload file
    val uploadResult = if (contentType.get().isCompatibleWith(MULTIPART_FORM_DATA)
      || contentType.get().isCompatibleWith(MULTIPART_MIXED)) {
      // traditional upload by form submit
      request.bodyToFlux(Part::class.java)
        .filter { it is FilePart }
        .collectList()
        .flatMap { files ->
          when {
            files.isEmpty() -> badRequest().bodyValue("missing file part in request body")
            files.size > 1 -> badRequest().bodyValue("upload multiple files not supported yet")
            else -> {
              val file = files.first() as FilePart
              fileService.upload(
                describer = FileDescriber.Impl(
                  module = module,
                  name = name,
                  type = type,
                  size = if (size != 0L) size else file.headers().contentLength
                ),
                source = FileUploadSource.FromFilePart(file)
              )
                .flatMap { status(CREATED).bodyValue(it) }
            }
          }
        }
    } else {
      // upload file by directly transmit file binary data through request body
      // contentType value as the real file media type
      val headerSize = headers.contentLength()
      if (!headerSize.isPresent) badRequest().bodyValue("missing header 'Content-Length'")
      else {
        fileService.upload(
          describer = FileDescriber.Impl(
            module = module,
            name = name,
            type = type,
            size = headerSize.asLong
          ),
          source = FileUploadSource.FromDataBufferPublisher(request.body(BodyExtractors.toDataBuffers()))
        )
          .flatMap { status(CREATED).bodyValue(it) }
      }
    }
    return uploadResult
      // permission denied
      .onErrorResume(PermissionDeniedException::class.java, Utils::responseForbiddenStatus)
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = POST("").or(POST("/"))
  }
}