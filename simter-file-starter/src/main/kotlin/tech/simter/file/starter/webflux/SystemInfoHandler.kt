package tech.simter.file.starter.webflux

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * The handler for get system information.
 *
 * @author RJ
 */
@Component
class SystemInfoHandler @Autowired constructor(
  gitProperties: GitProperties?,
  buildProperties: BuildProperties?
) : HandlerFunction<ServerResponse> {

  private var systemInfo = linkedMapOf(
    "gitBranch" to gitProperties?.branch,
    "gitCommitTime" to if (gitProperties != null) OffsetDateTime.ofInstant(gitProperties.commitTime, ZoneId.systemDefault()).toString() else null,
    "gitCommitId" to gitProperties?.commitId,
    "gitShortCommitId" to gitProperties?.shortCommitId,
    "gitDirty" to if (gitProperties != null) java.lang.Boolean(gitProperties.get("dirty")) else null,

    "projectName" to buildProperties?.name,
    "projectDescription" to buildProperties?.get("description"),
    "projectGroupId" to buildProperties?.group,
    "projectGroupId" to buildProperties?.group,
    "projectArtifactId" to buildProperties?.artifact,
    "projectVersion" to buildProperties?.version,
    "projectSimterVersion" to buildProperties?.get("simter.version"),
    "projectJavaTarget" to buildProperties?.get("java.target"),

    "projectBuildTime" to if (buildProperties != null) OffsetDateTime.ofInstant(buildProperties.time, ZoneId.systemDefault()).toString() else null,
    "projectStartTime" to OffsetDateTime.now().toString()
  )

  override fun handle(request: ServerRequest?): Mono<ServerResponse> {
    return ok().contentType(APPLICATION_JSON_UTF8).syncBody(systemInfo)
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/system-info")
  }
}