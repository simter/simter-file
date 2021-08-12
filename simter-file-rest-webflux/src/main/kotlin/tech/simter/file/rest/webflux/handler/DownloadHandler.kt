package tech.simter.file.rest.webflux.handler

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
import org.springframework.http.MediaType.TEXT_PLAIN
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters.fromResource
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.BASE_DATA_DIR
import tech.simter.file.buildContentDisposition
import tech.simter.file.core.FileDownload.Source.FromDataBufferPublisher
import tech.simter.file.core.FileDownload.Source.FromPath
import tech.simter.file.core.FileService
import tech.simter.file.core.ModuleMatcher
import tech.simter.file.packFilesTo
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * The [HandlerFunction] for download file.
 *
 * Request url pattern `GET /file/$id?type=x&filename=x&inline&pack`.
 *
 * See [rest-api.md#download-file](https://github.com/simter/simter-file/blob/master/docs/rest-api.md#4-download-file)
 *
 * @author RJ
 */
@Component
class DownloadHandler @Autowired constructor(
  private val json: Json,
  @Value("\${$BASE_DATA_DIR}") private val baseDir: String,
  @Value("\${simter-file.pack-limits: 25}") private val packLimits: Int,
  private val fileService: FileService
) : HandlerFunction<ServerResponse> {
  private val basePath = Paths.get(baseDir)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    val response = when (request.queryParam("type").orElse("")) {
      // download by path - fast
      "path" -> downloadByPath(request)
      // download by module
      "module" -> downloadByModule(request)
      // default download by id
      else -> downloadById(request)
    }
    return response
      // not found
      .switchIfEmpty(notFound().build())
      .onErrorResume(PermissionDeniedException::class.java) {
        if (it.message.isNullOrEmpty()) status(FORBIDDEN).build()
        else status(FORBIDDEN).contentType(TEXT_PLAIN).bodyValue(it.message!!)
      }
  }

  private fun downloadById(request: ServerRequest): Mono<ServerResponse> {
    val id = request.pathVariable("id")
    val packParam = request.queryParam("pack")
    val filenameParam = request.queryParam("filename")
    return if (packParam.isPresent) TODO("Not yet implemented for compress to zip file download")
    else {
      fileService.download(id)
        .flatMap {
          // calculate file name
          val filename = (filenameParam.orElseGet { "${it.describer.name}.${it.describer.type}" }) +
            (if (packParam.isPresent) ".zip" else "")

          // return response
          ok().contentLength(it.describer.size)
            .header(
              "Content-Disposition",
              buildContentDisposition(
                type = if (request.queryParam("inline").isPresent) "inline" else "attachment",
                filename = filename
              )
            )
            .body(
              when (it.source) {
                is FromPath -> fromResource(FileSystemResource(it.source.value as Path)) // zero-copy by webflux
                is FromDataBufferPublisher -> TODO("Not yet implemented")
              }
            )
        }
    }
  }

  private fun downloadByPath(request: ServerRequest): Mono<ServerResponse> {
    return downloadByPath(
      path = request.pathVariable("id"),
      pack = request.queryParam("pack").isPresent,
      inline = request.queryParam("inline").isPresent,
      customFilename = request.queryParam("filename")
    )
  }

  private fun downloadByPath(
    path: String,
    pack: Boolean,
    inline: Boolean,
    customFilename: Optional<String>
  ): Mono<ServerResponse> {
    // check whether file is on disk
    val filePath = Paths.get(baseDir, path)
    val file = filePath.toFile()
    if (!file.exists()) return Mono.empty() // not found

    // response
    return if (pack) TODO("Not yet implemented for compress to zip file download")
    else {
      ok()
        .header(
          "Content-Disposition",
          buildContentDisposition(
            type = (if (inline) "inline" else "attachment"),
            filename = (customFilename.orElseGet { file.name }) + (if (pack) ".zip" else "")
          )
        )
        // zero-copy by webflux
        .body(fromResource(FileSystemResource(filePath)))
    }
  }

  private fun downloadByModule(request: ServerRequest): Mono<ServerResponse> {
    return fileService.findList(ModuleMatcher.autoModuleMatcher(request.pathVariable("id")))
      .collectList()
      .flatMap { fileViews ->
        when {
          fileViews.isEmpty() -> Mono.empty()
          fileViews.size == 1 -> { // only one file match
            val file = fileViews.first()
            downloadByPath(
              path = file.path,
              pack = request.queryParam("pack").isPresent,
              inline = request.queryParam("inline").isPresent,
              customFilename = Optional.of(file.fileName)
            )
          }
          else -> { // more than one file need to pack
            // check pack limits
            if (fileViews.size > packLimits) {
              status(FORBIDDEN).contentType(TEXT_PLAIN)
                .bodyValue("Forbidden to pack more than $packLimits files")
            } else {
              val filename = (request.queryParam("filename").orElseGet { "unknown.zip" })
              ok().contentType(APPLICATION_OCTET_STREAM)
                .header(
                  "Content-Disposition",
                  buildContentDisposition(
                    type = (if (request.queryParam("inline").isPresent) "inline" else "attachment"),
                    filename = if (filename.endsWith(".zip")) filename else "$filename.zip"
                  )
                )
                .body { outputMessage, _ ->
                  // get custom mapper
                  val mapperString = request.queryParam("mapper").orElse("")
                  val mapper: Map<String, String> = when {
                    mapperString.isEmpty() -> emptyMap()
                    mapperString.startsWith("{") -> json.decodeFromString(mapperString)
                    else -> mapOf("_" to mapperString)
                  }
                  // pack to zip format
                  val buffer = outputMessage.bufferFactory().allocateBuffer()
                  packFilesTo(
                    outputStream = buffer.asOutputStream(),
                    files = fileViews,
                    basePath = basePath,
                    moduleMapper = mapper,
                    autoClose = true
                  )
                  outputMessage.writeWith(Mono.just(buffer))
                }
            }
          }
        }
      }
  }

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = GET("/{id}")
  }
}