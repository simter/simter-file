package tech.simter.file.rest.webflux

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.router
import tech.simter.file.rest.webflux.handler.*

private const val MODULE = "tech.simter.file.rest.webflux"

/**
 * All configuration for this module.
 *
 * Register a `RouterFunction<ServerResponse>` with all routers for this module.
 * The default context-path of this router is '/'. And can be config by property `simter.rest.context-path.kv`.
 *
 * @author RJ
 */
@Configuration("$MODULE.ModuleConfiguration")
@ComponentScan(MODULE)
class ModuleConfiguration @Autowired constructor(
  @Value("\${module.version.simter-file:UNKNOWN}") private val version: String,
  @Value("\${module.rest-context-path.simter-file:/file}") private val contextPath: String,
  private val attachmentFormHandler: AttachmentFormHandler,
  private val attachmentViewHandler: AttachmentViewHandler,
  private val findModuleAttachmentsHandler: FindModuleAttachmentsHandler,
  private val uploadFileByFormHandler: UploadFileByFormHandler,
  private val uploadFileByStreamHandler: UploadFileByStreamHandler,
  private val downloadFileHandler: DownloadFileHandler,
  private val inlineFileHandler: InlineFileHandler,
  private val deleteFilesHandler: DeleteFilesHandler,
  private val reuploadFileByStreamHandler: ReuploadFileByStreamHandler,
  private val updateAttachmentHandler: UpdateAttachmentHandler,
  private val findAttechmentDescendentsHandler: FindAttechmentDescendentsHandler,
  private val createAttachmentsHandler: CreateAttachmentsHandler
) {
  private val logger = LoggerFactory.getLogger(ModuleConfiguration::class.java)

  init {
    logger.warn("module.version.simter-file='{}'", version)
    logger.warn("module.rest-context-path.simter-file='{}'", contextPath)
  }

  /** Register a `RouterFunction<ServerResponse>` with all routers for this module */
  @Bean("$MODULE.Routes")
  @ConditionalOnMissingBean(name = ["$MODULE.Routes"])
  fun fileRoutes() = router {
    contextPath.nest {
      // POST /
      UploadFileByFormHandler.REQUEST_PREDICATE.invoke(uploadFileByFormHandler::handle)
      // POST /?puid=:puid&upper=:upper&filename=:filename
      UploadFileByStreamHandler.REQUEST_PREDICATE.invoke(uploadFileByStreamHandler::handle)
      // PATCH /{id} Content-Type: application/octet-stream
      ReuploadFileByStreamHandler.REQUEST_PREDICATE.invoke(reuploadFileByStreamHandler::handle)
      // GET /attachment?page-no=:pageNo&page-size=:pageSize
      AttachmentViewHandler.REQUEST_PREDICATE.invoke(attachmentViewHandler::handle)
      // GET /attachment/{id}
      AttachmentFormHandler.REQUEST_PREDICATE.invoke(attachmentFormHandler::handle)
      // GET /attachment/{id}/descendent
      FindAttechmentDescendentsHandler.REQUEST_PREDICATE.invoke(findAttechmentDescendentsHandler::handle)
      // POST /attachment
      CreateAttachmentsHandler.REQUEST_PREDICATE.invoke(createAttachmentsHandler::handle)
      // PATCH /attachment/{id} Content-Type: application/json;charset=UTF-8
      UpdateAttachmentHandler.REQUEST_PREDICATE.invoke(updateAttachmentHandler::handle)
      // GET /parent/{puid}/{upperId}
      FindModuleAttachmentsHandler.REQUEST_PREDICATE.invoke(findModuleAttachmentsHandler::handle)
      // GET /{id}
      DownloadFileHandler.REQUEST_PREDICATE.invoke(downloadFileHandler::handle)
      // GET /inline/{id}
      InlineFileHandler.REQUEST_PREDICATE.invoke(inlineFileHandler::handle)
      // DELETE /{ids}
      DeleteFilesHandler.REQUEST_PREDICATE.invoke(deleteFilesHandler::handle)
      // GET /
      GET("/", { ok().contentType(TEXT_PLAIN).syncBody("simter-file-$version") })
      // OPTIONS /*
      OPTIONS("/**", { ServerResponse.noContent().build() })
    }
  }
}