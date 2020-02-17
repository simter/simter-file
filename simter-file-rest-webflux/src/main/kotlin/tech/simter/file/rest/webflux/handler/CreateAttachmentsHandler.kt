package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.core.domain.Attachment
import tech.simter.file.core.domain.AttachmentCreateInfo
import tech.simter.file.impl.domain.AttachmentCreateInfoImpl
import tech.simter.file.impl.domain.AttachmentImpl
import java.time.OffsetDateTime

/**
 * The [HandlerFunction] for Create Attachments .
 *
 * Request:
 *
 * ```
 * POST {context-path}/attachment
 * Content-Type : application/json;charset=UTF-8
 *
 * [{DATA}, ...]
 * ```
 *
 * {DATA}={id, name, upperId, path, type, puid}
 * >
 *  If no path is specified, use ID instead.
 *  The user can specify the id but is responsible for ensuring the global uniqueness of the id.
 *  If type is ":d", the attachment is a folder attachment.
 *  If type is none or blank, the attachment type is not specified.
 * >
 *
 * Response: (if permission denied or across module)
 *
 * ```
 * 403 Forbidden
 * ```
 *
 * Response: (if created)
 *
 * ```
 * 201 Created
 *
 *  [id1, ..., idN] # The order is the same as [{DATA}, ...]
 * ```
 *
 * @author zh
 * @author RJ
 */

@Component
class CreateAttachmentsHandler @Autowired constructor(
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request.bodyToFlux<AttachmentCreateInfoImpl>().map { toAttachment(it) }
      .collectList().map { it.toTypedArray() }
      .flatMap { attachmentService.create(*it).collectList() }
      .flatMap { status(CREATED).contentType(APPLICATION_JSON).bodyValue(it) }
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
      .onErrorResume(ForbiddenException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
  }

  fun toAttachment(dto: AttachmentCreateInfo): Attachment {
    val id = dto.id
    val now = OffsetDateTime.now()
    return AttachmentImpl(id = id, path = dto.path, name = dto.name, type = dto.type,
      size = 0, createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter",
      puid = dto.puid ?: "", upperId = dto.upperId ?: "EMPTY")
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = POST("/attachment").and(contentType(APPLICATION_JSON))
  }
}