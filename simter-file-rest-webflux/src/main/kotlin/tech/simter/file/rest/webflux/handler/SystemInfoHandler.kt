package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
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
  gitProperties: GitProperties,
  buildProperties: BuildProperties
) : HandlerFunction<ServerResponse> {

  private var systemInfo = linkedMapOf(
    "gitBranch" to gitProperties.branch,
    "gitCommitTime" to OffsetDateTime.ofInstant(gitProperties.commitTime.toInstant(), ZoneId.systemDefault()).toString(),
    "gitCommitId" to gitProperties.commitId,
    "gitShortCommitId" to gitProperties.shortCommitId,
    "gitDirty" to java.lang.Boolean(gitProperties.get("dirty")),

    "projectName" to buildProperties.name,
    "projectDescription" to buildProperties.get("description"),
    "projectGroupId" to buildProperties.group,
    "projectGroupId" to buildProperties.group,
    "projectArtifactId" to buildProperties.artifact,
    "projectVersion" to buildProperties.version,
    "projectSimterVersion" to buildProperties.get("simter.version"),
    "projectJavaTarget" to buildProperties.get("java.target"),

    "projectBuildTime" to OffsetDateTime.ofInstant(buildProperties.time.toInstant(), ZoneId.systemDefault()).toString(),
    "projectStartTime" to OffsetDateTime.now().toString()
  )

  override fun handle(request: ServerRequest?): Mono<ServerResponse> {
    return ServerResponse.ok().contentType(APPLICATION_JSON_UTF8).syncBody(systemInfo)
  }

  /** Default router */
  fun router(): RouterFunction<ServerResponse> {
    return route(GET("/"), this)
  }
}