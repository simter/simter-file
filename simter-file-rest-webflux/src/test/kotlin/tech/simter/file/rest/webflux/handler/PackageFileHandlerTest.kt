package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.util.*

/**
 * Test [PackageFileHandler].
 *
 * @author zh
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class PackageFileHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService
) {
  @Test
  fun setZipName() {
    val id = UUID.randomUUID().toString()
    val defaultName = "test.zip"
    val specifiedName = "specified"
    every { service.packageAttachments(any(), eq(id)) } returns defaultName.toMono()

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
    every { service.packageAttachments(any(), eq(id)) } returns defaultName.toMono()

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
    every { service.packageAttachments(any(), eq(id)) } returns Mono.empty()

    // invoke
    client.get().uri("/zip/$id?name=test")
      .exchange().expectStatus().isNotFound

    // verify
    verify { service.packageAttachments(any(), eq(id)) }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val id = UUID.randomUUID().toString()
    every { service.packageAttachments(any(), eq(id)) } returns Mono.error(PermissionDeniedException())

    // invoke
    client.get().uri("/zip/$id?name=test")
      .exchange().expectStatus().isForbidden

    // verify
    verify { service.packageAttachments(any(), eq(id)) }
  }

  @Test
  fun failedByAcrossModule() {
    // mock
    val id = UUID.randomUUID().toString()
    every { service.packageAttachments(any(), eq(id)) } returns Mono.error(ForbiddenException())

    // invoke
    client.get().uri("/zip/$id?name=test")
      .exchange().expectStatus().isForbidden

    // verify
    verify { service.packageAttachments(any(), eq(id)) }
  }
}