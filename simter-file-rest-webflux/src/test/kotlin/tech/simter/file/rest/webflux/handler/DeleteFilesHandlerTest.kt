package tech.simter.file.rest.webflux.handler

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
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
    `when`(service.delete(id)).thenReturn(Mono.empty())

    // invoke
    client.delete().uri("/$id").exchange().expectStatus().isNoContent

    // verify
    Mockito.verify(service).delete(id)
  }

  @Test
  fun deleteBatch() {
    // mock
    val ids = arrayOf("a0001", "a0002", "a0003", "a0004", "a0005")
    `when`(service.delete(*ids)).thenReturn(Mono.empty())

    // invoke
    client.delete().uri("/${ids.joinToString(",")}").exchange().expectStatus().isNoContent

    // verify
    Mockito.verify(service).delete(*ids)
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val ids = arrayOf("a0001", "a0002", "a0003", "a0004", "a0005")
    `when`(service.delete(*ids)).thenReturn(Mono.error(PermissionDeniedException()))

    // invoke
    client.delete().uri("/${ids.joinToString(",")}").exchange().expectStatus().isForbidden

    // verify
    Mockito.verify(service).delete(*ids)
  }

  @Test
  fun failedByAcrossModule() {
    // mock
    val ids = arrayOf("a0001", "a0002", "a0003", "a0004", "a0005")
    `when`(service.delete(*ids)).thenReturn(Mono.error(ForbiddenException()))

    // invoke
    client.delete().uri("/${ids.joinToString(",")}").exchange().expectStatus().isForbidden

    // verify
    Mockito.verify(service).delete(*ids)
  }
}