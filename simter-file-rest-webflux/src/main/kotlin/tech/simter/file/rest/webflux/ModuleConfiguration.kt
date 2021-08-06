package tech.simter.file.rest.webflux

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router
import tech.simter.file.PACKAGE
import tech.simter.file.rest.webflux.handler.DownloadHandler as DownloadFileHandler
import tech.simter.file.rest.webflux.handler.FindHandler as FindFileViewDataHandler
import tech.simter.file.rest.webflux.handler.UploadHandler as UploadFileHandler
import tech.simter.file.rest.webflux.handler.DeleteHandler as DeleteFileHandler
import tech.simter.file.rest.webflux.handler.UpdateHandler as UpdateFileHandler

/**
 * All configuration for this module.
 *
 * Register a `RouterFunction<ServerResponse>` with all routers for this module.
 * The default context-path of this router is '/file'.
 * And can be config by property `simter-file.rest-context-path`.
 *
 * @author RJ
 */
@Configuration("$PACKAGE.rest.webflux.ModuleConfiguration")
@ComponentScan
class ModuleConfiguration @Autowired constructor(
  @Value("\${simter-file.rest-context-path:/file}")
  private val contextPath: String,
  private val downloadFileHandler: DownloadFileHandler,
  private val findFileViewDataHandler: FindFileViewDataHandler,
  private val uploadFileHandler: UploadFileHandler,
  private val deleteFileHandler: DeleteFileHandler,
  private val updateFileHandler: UpdateFileHandler
) {
  /** Register a `RouterFunction<ServerResponse>` with all routers for this module */
  @Bean("$PACKAGE.rest.webflux.Routes")
  @ConditionalOnMissingBean(name = ["$PACKAGE.rest.webflux.Routes"])
  fun fileRoutes() = router {
    contextPath.nest {
      // download file
      DownloadFileHandler.REQUEST_PREDICATE.invoke(downloadFileHandler::handle)
      // find file-view data
      FindFileViewDataHandler.REQUEST_PREDICATE.invoke(findFileViewDataHandler::handle)
      // upload file
      UploadFileHandler.REQUEST_PREDICATE.invoke(uploadFileHandler::handle)
      // delete file
      DeleteFileHandler.REQUEST_PREDICATE.invoke(deleteFileHandler::handle)
      // update file
      UpdateFileHandler.REQUEST_PREDICATE.invoke(updateFileHandler::handle)
    }
  }
}