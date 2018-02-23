package tech.simter.file.rest.webflux.router

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import reactor.core.publisher.Mono
import tech.simter.file.rest.webflux.handler.*


/**
 * All routers.configuration
 *
 * @author RJ
 */
@Component
class RouterConfiguration @Autowired constructor(
  private val systemInfoHandler: SystemInfoHandler,
  private val uploadFileHandler: UploadFileHandler,
  private val downloadFileHandler: DownloadFileHandler,
  private val attachmentViewHandler: AttachmentViewHandler,
  private val attachmentFormHandler: AttachmentFormHandler
) : RouterFunction<ServerResponse> {
  override fun route(request: ServerRequest): Mono<HandlerFunction<ServerResponse>> {
    return RouterFunctions.route(GET("/"), systemInfoHandler) // /root
      .andRoute(GET("/system-info"), systemInfoHandler)       // /system-info
      .andRoute(POST("/").and(contentType(MediaType.MULTIPART_FORM_DATA)), uploadFileHandler) // /root
      .andRoute(GET("/attachment"), attachmentViewHandler)      // /attachment?page-no=:pageNo&page-size=:pageSize
      .andRoute(GET("/attachment/{id}"), attachmentFormHandler) // /attachment/{id}
      .andRoute(GET("/{id}"), downloadFileHandler)   // /{id}
      .route(request)
  }
}