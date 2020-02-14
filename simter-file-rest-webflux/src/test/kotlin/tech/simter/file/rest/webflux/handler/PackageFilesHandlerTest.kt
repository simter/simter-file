package tech.simter.file.rest.webflux.handler

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.rest.webflux.handler.PackageFilesHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.core.AttachmentService
import java.util.*

/**
 * Test [PackageFilesHandler].
 *
 * @author zh
 */
@SpringJUnitConfig(PackageFilesHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
@TestPropertySource(properties = ["simter.file.root=target"])
internal class PackageFilesHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  @Value("\${simter.file.root}") private val fileRootDir: String,
  handler: PackageFilesHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @Test
  fun setZipName() {
    val ids = List(3) { UUID.randomUUID().toString() }
    val defaultName = "test.zip"
    val specifiedName = "specified"
    `when`(service.packageAttachments(any(), *(ids.map { eq(it) }).toTypedArray()))
      .thenReturn(defaultName.toMono())

    client.post().uri("/zip?name=$specifiedName")
      .contentType(APPLICATION_FORM_URLENCODED).syncBody(ids.joinToString("&") { "id=$it" })
      .exchange().expectStatus().isOk
      .expectHeader().valueEquals("Content-Type", APPLICATION_OCTET_STREAM_VALUE)
      .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"$specifiedName.zip\"")
      .expectBody().returnResult()
  }

  @Test
  fun notSetZipName() {
    val ids = List(3) { UUID.randomUUID().toString() }
    val defaultName = "test.zip"
    `when`(service.packageAttachments(any(), *(ids.map { eq(it) }).toTypedArray()))
      .thenReturn(defaultName.toMono())

    client.post().uri("/zip")
      .contentType(APPLICATION_FORM_URLENCODED).syncBody(ids.joinToString("&") { "id=$it" })
      .exchange().expectStatus().isOk
      .expectHeader().valueEquals("Content-Type", APPLICATION_OCTET_STREAM_VALUE)
      .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"$defaultName\"")
      .expectBody().returnResult()
  }

  @Test
  fun notFound() {
    // mock
    val id = UUID.randomUUID().toString()
    `when`(service.packageAttachments(any(), eq(id))).thenReturn(Mono.empty())

    // invoke
    client.post().uri("/zip?name=test")
      .contentType(APPLICATION_FORM_URLENCODED).syncBody("id=$id")
      .exchange().expectStatus().isNotFound

    // verify
    verify(service).packageAttachments(any(), eq(id))
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val id = UUID.randomUUID().toString()
    `when`(service.packageAttachments(any(), eq(id))).thenReturn(Mono.error(PermissionDeniedException()))

    // invoke
    client.post().uri("/zip?name=test")
      .contentType(APPLICATION_FORM_URLENCODED).syncBody("id=$id")
      .exchange().expectStatus().isForbidden

    // verify
    verify(service).packageAttachments(any(), eq(id))
  }

  @Test
  fun failedByAcrossModule() {
    // mock
    val id = UUID.randomUUID().toString()
    `when`(service.packageAttachments(any(), eq(id))).thenReturn(Mono.error(ForbiddenException()))

    // invoke
    client.post().uri("/zip?name=test")
      .contentType(APPLICATION_FORM_URLENCODED).syncBody("id=$id")
      .exchange().expectStatus().isForbidden

    // verify
    verify(service).packageAttachments(any(), eq(id))
  }
}