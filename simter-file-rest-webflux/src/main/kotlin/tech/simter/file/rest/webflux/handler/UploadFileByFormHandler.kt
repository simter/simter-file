package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.core.domain.Attachment
import java.net.URI
import java.util.*

/**
 * The [HandlerFunction] for upload file by from.
 *
 * Request: (form submit with <input type="file">)
 *
 * ```
 * POST {context-path}/
 * Content-Type        : multipart/form-data; boundary=----{boundary}
 * Content-Length      : {len}
 *
 * ------{boundary}
 * Content-Disposition: form-data; name="{input-name}"; filename="{file-name}.{type}"
 * Content-Type: {media-type}
 *
 * {file-data}
 * ------{boundary}
 * Content-Disposition: form-data; name="puid"
 *
 * {puid}
 * ------{boundary}
 * Content-Disposition: form-data; name="upperId"
 *
 * {upperId}
 * ------{boundary}--
 * ```
 *
 * Response: (if created)
 *
 * ```
 * 204 No Content
 * Location : {context-path}/{id}
 * ```
 *
 * Response: (if permission denied)
 *
 * ```
 * 403 Forbidden
 * ```
 *
 * Response: (if not found upper)
 *
 * ```
 * 404 Not Found
 *
 * Upper not exists
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Upload-One-File-By-Form)
 *
 * @author JF
 * @author RJ
 * @author zh
 */
@Component
class UploadFileByFormHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request
      .body(BodyExtractors.toMultipartData())
      // 2. save file to disk
      .flatMap { mvm ->
        val it = mvm.toSingleValueMap()
        // get the FilePart by the Map
        val fileData = it["fileData"] as FilePart
        // convert to Attachment instance
        val attachment = createAttachment(
          id = newId(),
          fileSize = fileData.headers().contentLength,
          fileName = fileData.filename(),
          puid = (it["puid"] as FormFieldPart?)?.value(),
          upperId = (it["upperId"] as FormFieldPart?)?.value()
        )
        attachmentService.uploadFile(attachment) { file ->
          fileData.transferTo(file)
        }.thenReturn(attachment.id)
      }
      // 3. return response
      .flatMap { status(CREATED).location(URI.create("/$it")).build() }
      .onErrorResume(NotFoundException::class.java) {
        if (it.message.isNullOrEmpty()) status(NOT_FOUND).build()
        else status(NOT_FOUND).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
  }

  /** Generate a new [Attachment] id */
  fun newId() = UUID.randomUUID().toString()

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = POST("/").and(contentType(MULTIPART_FORM_DATA))
  }
}