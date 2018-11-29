package tech.simter.file.rest.webflux.handler

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import tech.simter.file.rest.webflux.handler.PackageFileHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.util.*

/**
 * Test [PackageFileHandler].
 *
 * @author zh
 */
@SpringJUnitConfig(PackageFileHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
internal class PackageFileHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  handler: PackageFileHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @Test
  fun setZipName() {
    val id = UUID.randomUUID().toString()
    val defaultName = "test.zip"
    val specifiedName = "specified"
    `when`(service.packageAttachments(any(), eq(id))).thenReturn(defaultName.toMono())

    client.get().uri("/zip/$id?name=$specifiedName")
      .exchange().expectStatus().isOk
      .expectHeader().valueEquals("Content-Type", APPLICATION_OCTET_STREAM_VALUE)
      .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"$specifiedName.zip\"")
      .expectBody().returnResult()
  }

  @Test
  fun notSetZipName() {
    val id = UUID.randomUUID().toString()
    val defaultName = "test.zip"
    `when`(service.packageAttachments(any(), eq(id))).thenReturn(defaultName.toMono())

    client.get().uri("/zip/$id")
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
    client.get().uri("/zip/$id?name=test")
      .exchange().expectStatus().isNotFound

    // verify
    verify(service).packageAttachments(any(), eq(id))
  }
}