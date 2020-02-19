package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.util.*

/**
 * Test [DeleteFilesHandler].
 *
 * @author JW
 * @author zh
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
@TestPropertySource(properties = ["simter.file.root=target/files"])
class DeleteFilesHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService
) {
  @Test
  fun deleteOne() {
    // mock
    val id = UUID.randomUUID().toString()
    every { service.delete(id) } returns Mono.empty()

    // invoke
    client.delete().uri("/$id").exchange().expectStatus().isNoContent

    // verify
    verify { service.delete(id) }
  }

  @Test
  fun deleteBatch() {
    // mock
    val ids = arrayOf("a0001", "a0002", "a0003", "a0004", "a0005")
    every { service.delete(*ids) } returns Mono.empty()

    // invoke
    client.delete().uri("/${ids.joinToString(",")}").exchange().expectStatus().isNoContent

    // verify
    verify { service.delete(*ids) }
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val ids = arrayOf("a0001", "a0002", "a0003", "a0004", "a0005")
    every { service.delete(*ids) } returns Mono.error(PermissionDeniedException())

    // invoke
    client.delete().uri("/${ids.joinToString(",")}").exchange().expectStatus().isForbidden

    // verify
    verify { service.delete(*ids) }
  }

  @Test
  fun failedByAcrossModule() {
    // mock
    val ids = arrayOf("a0001", "a0002", "a0003", "a0004", "a0005")
    every { service.delete(*ids) } returns Mono.error(ForbiddenException())

    // invoke
    client.delete().uri("/${ids.joinToString(",")}").exchange().expectStatus().isForbidden

    // verify
    verify { service.delete(*ids) }
  }
}