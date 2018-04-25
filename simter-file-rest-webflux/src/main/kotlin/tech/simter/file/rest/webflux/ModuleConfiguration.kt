package tech.simter.file.rest.webflux

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.router
import tech.simter.file.rest.webflux.handler.AttachmentFormHandler
import tech.simter.file.rest.webflux.handler.AttachmentViewHandler
import tech.simter.file.rest.webflux.handler.DownloadFileHandler
import tech.simter.file.rest.webflux.handler.UploadFileHandler

private const val MODULE_PACKAGE = "tech.simter.file.rest.webflux"

/**
 * All configuration for this module.
 *
 * Register a `RouterFunction<ServerResponse>` with all routers for this module.
 * The default context-path of this router is '/'. And can be config by property `simter.rest.context-path.kv`.
 *
 * @author RJ
 */
@Configuration("$MODULE_PACKAGE.ModuleConfiguration")
@ComponentScan(MODULE_PACKAGE)
@EnableWebFlux
class ModuleConfiguration @Autowired constructor(
  @Value("\${simter.rest.context-path.file:/}") private val contextPath: String,
  private val attachmentFormHandler: AttachmentFormHandler,
  private val attachmentViewHandler: AttachmentViewHandler,
  private val uploadFileHandler: UploadFileHandler,
  private val downloadFileHandler: DownloadFileHandler
) {
  private val logger = LoggerFactory.getLogger(ModuleConfiguration::class.java)

  init {
    logger.warn("simter.rest.context-path.file='{}'", contextPath)
  }

  /** Register a `RouterFunction<ServerResponse>` with all routers for this module */
  @Bean("tech.simter.file.rest.webflux.Routes")
  @ConditionalOnMissingBean(name = ["tech.simter.file.rest.webflux.Routes"])
  fun fileRoutes() = router {
    contextPath.nest {
      // POST /
      UploadFileHandler.REQUEST_PREDICATE.invoke(uploadFileHandler::handle)
      // GET /attachment?page-no=:pageNo&page-size=:pageSize
      AttachmentViewHandler.REQUEST_PREDICATE.invoke(attachmentViewHandler::handle)
      // GET /attachment/{id}
      AttachmentFormHandler.REQUEST_PREDICATE.invoke(attachmentFormHandler::handle)
      // GET /{id}
      DownloadFileHandler.REQUEST_PREDICATE.invoke(downloadFileHandler::handle)
    }
  }

//  fun route(request: ServerRequest): Mono<HandlerFunction<ServerResponse>> {
//    return RouterFunctions.route(GET("/"), systemInfoHandler) // /root
//      .andRoute(GET("/system-info"), systemInfoHandler)       // /system-info
//      .andRoute(POST("/").and(contentType(MediaType.MULTIPART_FORM_DATA)), uploadFileHandler) // /root
//      .andRoute(GET("/attachment"), attachmentViewHandler)      // /attachment?page-no=:pageNo&page-size=:pageSize
//      .andRoute(GET("/attachment/{id}"), attachmentFormHandler) // /attachment/{id}
//      .andRoute(GET("/{id}"), downloadFileHandler)   // /{id}
//      .route(request)
//  }
}